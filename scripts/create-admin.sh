#!/usr/bin/env bash
# ==============================================================================
# DevTrack Administrator Provisioning Tool
# ==============================================================================
# Securely provisions or promotes an administrator in Keycloak without
# committing credentials to version control.
#
# Usage:
#   ./scripts/create-admin.sh [-u <username>] [-e <email>] [-p <password>]
#
# If flags are omitted, the script prompts interactively.
# ==============================================================================

set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

KEYCLOAK_CONTAINER="${KEYCLOAK_CONTAINER:-devtrack-keycloak}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="devtrack"

KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin_secret_change_me_in_prod}"

ADMIN_USER=""
ADMIN_EMAIL=""
ADMIN_PASSWORD=""
FIRST_NAME="DevTrack"
LAST_NAME="Administrator"

# Parse optional command-line flags
while [[ $# -gt 0 ]]; do
  case "$1" in
    -u|--username)
      ADMIN_USER="$2"
      shift 2
      ;;
    -e|--email)
      ADMIN_EMAIL="$2"
      shift 2
      ;;
    -p|--password)
      ADMIN_PASSWORD="$2"
      shift 2
      ;;
    -f|--first-name)
      FIRST_NAME="$2"
      shift 2
      ;;
    -l|--last-name)
      LAST_NAME="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [-u username] [-e email] [-p password] [-f firstName] [-l lastName]"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}      DevTrack Administrator Provisioning Tool        ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Interactive prompts if arguments are not provided
if [[ -z "$ADMIN_USER" ]]; then
  read -r -p "Enter administrator username to create or promote: " ADMIN_USER
fi

if [[ -z "$ADMIN_USER" ]]; then
  echo -e "${RED}Error: Username cannot be empty.${NC}"
  exit 1
fi

# Check if Keycloak container is running or if we can contact Keycloak
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${KEYCLOAK_CONTAINER}$"; then
  USE_DOCKER=true
  echo -e "${CYAN}✓ Detected running container '${KEYCLOAK_CONTAINER}'. Executing via Keycloak kcadm CLI...${NC}"
else
  USE_DOCKER=false
  echo -e "${YELLOW}Note: Docker container '${KEYCLOAK_CONTAINER}' not detected locally.${NC}"
  echo -e "${YELLOW}Attempting direct REST API authentication to ${KEYCLOAK_URL}...${NC}"
fi

if [[ "$USE_DOCKER" == "true" ]]; then
  # 1. Authenticate with kcadm inside container
  docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh config credentials \
    --server http://localhost:8080 \
    --realm master \
    --user "$KEYCLOAK_ADMIN" \
    --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null 2>&1 || {
      echo -e "${RED}Failed to authenticate master admin. Check KEYCLOAK_ADMIN and KEYCLOAK_ADMIN_PASSWORD.${NC}"
      exit 1
    }

  # 2. Check if user already exists
  EXISTING_USER_ID=$(docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get users -r "$REALM" -q username="$ADMIN_USER" --fields id --format csv --noquotes 2>/dev/null || true)

  if [[ -n "$EXISTING_USER_ID" ]]; then
    echo -e "${CYAN}User '${ADMIN_USER}' already exists in realm '${REALM}' (ID: ${EXISTING_USER_ID}).${NC}"
    echo -e "${CYAN}Promoting user to ADMIN role...${NC}"
  else
    if [[ -z "$ADMIN_EMAIL" ]]; then
      read -r -p "Enter email for ${ADMIN_USER} [${ADMIN_USER}@devtrack.local]: " ADMIN_EMAIL
      ADMIN_EMAIL="${ADMIN_EMAIL:-${ADMIN_USER}@devtrack.local}"
    fi

    if [[ -z "$ADMIN_PASSWORD" ]]; then
      read -r -s -p "Enter secure password for ${ADMIN_USER}: " ADMIN_PASSWORD
      echo ""
      if [[ -z "$ADMIN_PASSWORD" ]]; then
        echo -e "${RED}Error: Password cannot be empty.${NC}"
        exit 1
      fi
    fi

    echo -e "${CYAN}Creating user '${ADMIN_USER}' (${ADMIN_EMAIL})...${NC}"
    docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh create users -r "$REALM" \
      -s username="$ADMIN_USER" \
      -s email="$ADMIN_EMAIL" \
      -s firstName="$FIRST_NAME" \
      -s lastName="$LAST_NAME" \
      -s enabled=true \
      -s emailVerified=true

    echo -e "${CYAN}Setting user credentials...${NC}"
    docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh set-password -r "$REALM" \
      --username "$ADMIN_USER" \
      --new-password "$ADMIN_PASSWORD"
  fi

  # 3. Assign ADMIN and USER roles
  echo -e "${CYAN}Assigning ADMIN role...${NC}"
  docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh add-roles -r "$REALM" \
    --uusername "$ADMIN_USER" \
    --rolename ADMIN

  docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh add-roles -r "$REALM" \
    --uusername "$ADMIN_USER" \
    --rolename USER || true

  echo -e "\n${GREEN}======================================================${NC}"
  echo -e "${GREEN}✓ Successfully provisioned administrator '${ADMIN_USER}'!${NC}"
  echo -e "${GREEN}  Realm:      ${REALM}${NC}"
  echo -e "${GREEN}  Roles:      ADMIN, USER${NC}"
  echo -e "${GREEN}  Login URL:  ${KEYCLOAK_URL}/realms/${REALM}/account${NC}"
  echo -e "${GREEN}======================================================${NC}"

else
  # REST API fallback when kcadm is not available directly via docker exec
  echo -e "${CYAN}Obtaining admin access token from Keycloak REST API...${NC}"
  TOKEN_RES=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=${KEYCLOAK_ADMIN}" \
    -d "password=${KEYCLOAK_ADMIN_PASSWORD}")

  ACCESS_TOKEN=$(echo "$TOKEN_RES" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

  if [[ -z "$ACCESS_TOKEN" ]]; then
    echo -e "${RED}Failed to obtain admin token from ${KEYCLOAK_URL}. Ensure Keycloak is running.${NC}"
    exit 1
  fi

  # Find user
  USER_QUERY=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${ADMIN_USER}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  USER_ID=$(echo "$USER_QUERY" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -n 1)

  if [[ -z "$USER_ID" ]]; then
    if [[ -z "$ADMIN_EMAIL" ]]; then
      read -r -p "Enter email for ${ADMIN_USER} [${ADMIN_USER}@devtrack.local]: " ADMIN_EMAIL
      ADMIN_EMAIL="${ADMIN_EMAIL:-${ADMIN_USER}@devtrack.local}"
    fi
    if [[ -z "$ADMIN_PASSWORD" ]]; then
      read -r -s -p "Enter secure password for ${ADMIN_USER}: " ADMIN_PASSWORD
      echo ""
    fi

    # Create user
    echo -e "${CYAN}Creating user '${ADMIN_USER}' via Admin REST API...${NC}"
    CREATE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{
        \"username\": \"${ADMIN_USER}\",
        \"email\": \"${ADMIN_EMAIL}\",
        \"firstName\": \"${FIRST_NAME}\",
        \"lastName\": \"${LAST_NAME}\",
        \"enabled\": true,
        \"emailVerified\": true,
        \"credentials\": [{
          \"type\": \"password\",
          \"value\": \"${ADMIN_PASSWORD}\",
          \"temporary\": false
        }]
      }")

    if [[ "$CREATE_STATUS" != "201" ]]; then
      echo -e "${RED}Failed to create user (HTTP ${CREATE_STATUS}).${NC}"
      exit 1
    fi

    USER_QUERY=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${ADMIN_USER}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}")
    USER_ID=$(echo "$USER_QUERY" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -n 1)
  fi

  # Fetch ADMIN role id
  ROLES_RES=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/ADMIN" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  ROLE_ID=$(echo "$ROLES_RES" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

  # Map ADMIN role
  MAPPING_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_ID}/role-mappings/realm" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "[{\"id\":\"${ROLE_ID}\",\"name\":\"ADMIN\"}]")

  echo -e "\n${GREEN}======================================================${NC}"
  echo -e "${GREEN}✓ Successfully provisioned administrator '${ADMIN_USER}'!${NC}"
  echo -e "${GREEN}  Realm:      ${REALM}${NC}"
  echo -e "${GREEN}  Roles:      ADMIN${NC}"
  echo -e "${GREEN}======================================================${NC}"
fi
