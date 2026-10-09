#!/usr/bin/env bash
# ==============================================================================
# DevTrack End-to-End API Smoke Test
# Verifies Keycloak Token Issuance, Protected Endpoints, Role Enforcement,
# and Strict Multi-Tenant Ownership Boundaries.
# ==============================================================================

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8081}"
REALM="devtrack"
CLIENT_ID="devtrack-frontend"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}  DevTrack Production Integration & Security Smoke Test ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Check dependencies
command -v curl >/dev/null 2>&1 || { echo -e "${RED}curl is required${NC}"; exit 1; }
command -v jq >/dev/null 2>&1 || { echo -e "${YELLOW}Warning: jq is not installed. Output won't be formatted.${NC}"; }

# 1. Health checks
echo -e "\n${YELLOW}[Step 1/6] Verifying Backend Health...${NC}"
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/actuator/health" || true)
if [ "$HEALTH_STATUS" != "200" ]; then
    echo -e "${RED}FAILED: Backend health check returned HTTP ${HEALTH_STATUS}. Ensure services are up.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Backend is UP (HTTP 200)${NC}"

# 2. Authenticate as dev_user
echo -e "\n${YELLOW}[Step 2/6] Authenticating as dev_user via Keycloak OIDC...${NC}"
USER_TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=${CLIENT_ID}" \
    -d "username=dev_user" \
    -d "password=password123")

USER_TOKEN=$(echo "$USER_TOKEN_RESPONSE" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

if [ -z "$USER_TOKEN" ]; then
    echo -e "${RED}FAILED: Could not obtain token for dev_user.${NC}"
    echo "Response: $USER_TOKEN_RESPONSE"
    exit 1
fi
echo -e "${GREEN}✓ Successfully obtained JWT access token for dev_user${NC}"

# 3. Create a task as dev_user
echo -e "\n${YELLOW}[Step 3/6] Creating Task via Protected API with Bearer token...${NC}"
CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BACKEND_URL}/api/tasks" \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
        "title": "Smoke Test Task - Verify Auth",
        "description": "Automated verification of OIDC token authorization",
        "priority": "HIGH",
        "status": "OPEN"
    }')

HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n 1)
BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" != "201" ]; then
    echo -e "${RED}FAILED: Expected HTTP 201, got HTTP ${HTTP_CODE}${NC}"
    echo "Body: $BODY"
    exit 1
fi
TASK_ID=$(echo "$BODY" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
echo -e "${GREEN}✓ Task created successfully with ID: ${TASK_ID} (HTTP 201)${NC}"

# 4. Verify Non-Admin Forbidden on Admin Endpoint
echo -e "\n${YELLOW}[Step 4/6] Verifying Role-Based Access Control: dev_user forbidden from /api/admin/metrics...${NC}"
ADMIN_FORBIDDEN_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BACKEND_URL}/api/admin/metrics" \
    -H "Authorization: Bearer ${USER_TOKEN}")

if [ "$ADMIN_FORBIDDEN_CODE" != "403" ]; then
    echo -e "${RED}FAILED: Expected HTTP 403 Forbidden for non-admin user, got ${ADMIN_FORBIDDEN_CODE}${NC}"
    exit 1
fi
echo -e "${GREEN}✓ RBAC Verified: dev_user correctly denied access to admin metrics (HTTP 403)${NC}"

# 5. Authenticate as dev_admin
echo -e "\n${YELLOW}[Step 5/6] Authenticating as dev_admin...${NC}"
ADMIN_TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=${CLIENT_ID}" \
    -d "username=dev_admin" \
    -d "password=password123")

ADMIN_TOKEN=$(echo "$ADMIN_TOKEN_RESPONSE" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}FAILED: Could not obtain token for dev_admin.${NC}"
    exit 1
fi

METRICS_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BACKEND_URL}/api/admin/metrics" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

if [ "$METRICS_CODE" != "200" ]; then
    echo -e "${RED}FAILED: dev_admin expected HTTP 200 on /api/admin/metrics, got ${METRICS_CODE}${NC}"
    exit 1
fi
echo -e "${GREEN}✓ dev_admin successfully accessed system telemetry (HTTP 200)${NC}"

# 6. Verify Ownership Privacy Boundary: dev_admin cannot view dev_user's private task
echo -e "\n${YELLOW}[Step 6/6] Verifying Privacy Boundary: Admin cannot inspect user private task...${NC}"
ADMIN_PRIVATE_TASK_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BACKEND_URL}/api/tasks/${TASK_ID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

if [ "$ADMIN_PRIVATE_TASK_CODE" != "403" ]; then
    echo -e "${RED}FAILED: Privacy boundary violated! Expected 403 Forbidden for cross-user task access, got ${ADMIN_PRIVATE_TASK_CODE}${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Task ownership boundary verified: dev_admin denied access to dev_user's private task (HTTP 403)${NC}"

echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}  ALL SMOKE TESTS PASSED! DEVTRACK IS FULLY FUNCTIONAL ${NC}"
echo -e "${GREEN}======================================================${NC}"
