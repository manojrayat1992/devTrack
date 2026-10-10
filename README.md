# DevTrack — Production Developer Task Management Platform

[![CI Pipeline](https://github.com/devtrack/devtrack/actions/workflows/ci.yml/badge.svg)](https://github.com/devtrack/devtrack/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Keycloak](https://img.shields.io/badge/Keycloak-24.0.5-red.svg)](https://www.keycloak.org/)
[![React 18](https://img.shields.io/badge/React-18.3-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg)](https://www.typescriptlang.org/)

**DevTrack** is an enterprise-grade developer task management platform designed to reflect real-world production engineering practices. It features clean architecture, strict tenant isolation, finite-state-machine status transitions, OAuth 2.0 / OIDC authentication with Keycloak (PKCE S256 flow), self-registration, role-based access control (RBAC), and container orchestration.

---

## Architecture Overview

```
                          ┌─────────────────────────────────────┐
                          │         Browser Client (SPA)        │
                          │      React 18 + TypeScript + Vite   │
                          └──────┬───────────────────────▲──────┘
                                 │                       │
                 1. OIDC PKCE    │                       │  4. Protected REST Calls
                 Auth Flow       │                       │     (Bearer JWT)
                                 ▼                       │
     ┌───────────────────────────────┐     ┌─────────────┴─────────────────┐
     │           Keycloak            │     │      DevTrack Backend         │
     │      Identity Provider        │◄────┤   Spring Boot 3.3 (Resource   │
     │    (Realm: devtrack)          │  3. │            Server)            │
     └───────────────┬───────────────┘ JWKS└─────────────┬─────────────────┘
                     │                 Fetch             │
                     │                                   │ 5. JPA / SQL
                     │                                   ▼
                     │                     ┌───────────────────────────────┐
                     └────────────────────►│       PostgreSQL 16           │
                       (keycloak_db)       │  Flyway (devtrack_db)         │
                                           └───────────────────────────────┘
```

### Key Architectural Highlights

1. **OAuth 2.0 Authorization Code Flow with PKCE (S256)**:
   - The browser initiates PKCE-protected authentication directly against Keycloak (`http://localhost:8080/realms/devtrack`).
   - Self-registration and password management are handled by Keycloak's secure hosted interfaces. Passwords are never sent directly to or stored by the Spring Boot backend.
   - The backend validates the JWT against the issuer (`http://localhost:8080/realms/devtrack`) while retrieving cryptographic JWKs over the Docker network (`http://keycloak:8080/realms/devtrack/protocol/openid-connect/certs`).
   - The backend strictly enforces that the access token's audience claim (`aud`) contains `devtrack-backend`.
2. **Deterministic Ownership & Multi-Tenancy**:
   - The task owner identifier is resolved strictly on the backend from the verified JWT `sub` (subject) claim via `SecurityUtils.getCurrentUserId()`.
   - Client-supplied owner IDs are rejected and ignored. Email addresses are never used as authorization identifiers.
   - If a user queries, modifies, or deletes a task owned by another user, an HTTP 403 Forbidden (`TaskOwnershipException`) is returned.
3. **Admin Privilege Boundary (Privacy by Design)**:
   - Administrators (`ROLE_ADMIN`) can view aggregate platform metrics via `/api/admin/metrics`.
   - Administrators **cannot** inspect or modify private tasks owned by individual developers unless they own the task.
4. **Persistent Keycloak Architecture**:
   - Keycloak is backed by PostgreSQL (`keycloak_db`) on the persistent `postgres_data` volume, preventing loss of accounts upon container restarts.
5. **Finite State Machine Status Validation**:
   - Enforced by `TaskStatusTransitionEngine`:
     - `OPEN` ➔ `IN_PROGRESS`, `COMPLETED`
     - `IN_PROGRESS` ➔ `IN_REVIEW`, `OPEN`, `COMPLETED`
     - `IN_REVIEW` ➔ `COMPLETED`, `IN_PROGRESS`
     - `COMPLETED` ➔ `OPEN`, `IN_PROGRESS`

---

## Application URLs

| Service | Host URL | Description |
| :--- | :--- | :--- |
| **DevTrack Frontend** | [http://localhost:5173](http://localhost:5173) | Single-page application with dashboard & auth |
| **DevTrack REST API** | [http://localhost:8081/api](http://localhost:8081/api) | Protected Spring Boot API endpoints |
| **API Documentation (Swagger)** | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | Interactive OpenAPI documentation |
| **Backend Health Check** | [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) | Public Spring Boot Actuator health endpoint |
| **Keycloak Administration** | [http://localhost:8080](http://localhost:8080) | Keycloak Master & DevTrack administration |
| **Keycloak Account Console** | [http://localhost:8080/realms/devtrack/account](http://localhost:8080/realms/devtrack/account) | User self-service profile and password reset |
| **OIDC Discovery Document** | [http://localhost:8080/realms/devtrack/.well-known/openid-configuration](http://localhost:8080/realms/devtrack/.well-known/openid-configuration) | OpenID Connect discovery metadata |

---

## User Onboarding & Authentication Workflow

### 1. Registering a New User (Self-Registration)
1. Open the DevTrack frontend at [http://localhost:5173](http://localhost:5173).
2. Click **Create Account** in the navbar or on the landing hero.
3. The browser redirects to Keycloak's hosted registration page (`/realms/devtrack/protocol/openid-connect/registrations`) with PKCE parameters.
4. Fill in:
   - **First name**
   - **Last name**
   - **Email** (must be unique)
   - **Username** (must be unique)
   - **Password** and confirm password
5. Submit the registration form. Keycloak creates the user in PostgreSQL and automatically assigns the default role `USER` via `default-roles-devtrack`. (In development, `verifyEmail: false` allows immediate access without an SMTP server).
6. Keycloak redirects back to the frontend callback (`http://localhost:5173`) with the authorization code.
7. The React SPA exchanges the code via PKCE for access and refresh tokens.
8. The user immediately sees their empty private dashboard and can create their first task!

### 2. Logging In
1. Open [http://localhost:5173](http://localhost:5173).
2. Click **Sign In** in the navbar or the landing hero.
3. Keycloak's hosted login page is displayed. Enter your credentials.
4. Upon validation, Keycloak redirects back with the authorization code.
5. The SPA initializes the session, stores tokens purely in-memory, extracts the subject ID (`sub`), and loads your personal tasks.

### 3. Logging Out
1. Click the **Logout** button in the upper-right user panel of the navbar.
2. The frontend clears in-memory tokens and calls `keycloak.logout()`, terminating the session at Keycloak and returning to the clean unauthenticated landing page.

---

## Role Assignment and Administrative Provisioning

### Roles Overview
* **`USER`**: Granted automatically to all self-registered users. Permitted to create, view, update, and delete their own tasks.
* **`ADMIN`**: Privileged role granting access to system-wide health and telemetry metrics (`/api/admin/metrics`). Ordinary users cannot assign this role to themselves.

### Secure Administrator Provisioning

Never commit administrator passwords or client secrets to version control. To provision an administrator account:

#### Method A: Using the Automated Provisioning Script
Run the included provisioning script:
```bash
./scripts/create-admin.sh -u my_admin -e admin@devtrack.local -p MySecurePassword123!
```
*If flags are omitted, the script prompts interactively with hidden password input.*
The script connects to Keycloak, creates or promotes the specified user, and assigns the `ADMIN` and `USER` realm roles.

#### Method B: Using Keycloak Admin Console
1. Open [http://localhost:8080](http://localhost:8080) and log in to the **Master** realm with master credentials (`admin` / configured password).
2. Select the **`devtrack`** realm from the realm dropdown.
3. Go to **Users** ➔ **Add user** (or search for an existing self-registered user).
4. Enter the user details, set **Email verified** to `ON`, and click **Create**.
5. Under the **Credentials** tab, click **Set password**, enter a password, and toggle **Temporary** to `OFF`.
6. Under the **Role mapping** tab, click **Assign role**, filter by realm roles, select **`ADMIN`**, and click **Assign**.

---

## Identity & Database Design

1. **Owner Identifier**:
   - The task entity stores `owner_id VARCHAR(255)`.
   - The backend derives `owner_id` from the JWT `sub` claim (`jwt.getSubject()`).
   - Database operations enforce ownership at both the JPA Specification layer (`TaskSpecification.withFilters`) and the service layer (`getTaskAndVerifyOwnership`).
2. **No Local Passwords**:
   - The DevTrack database contains **zero** password hashes or authentication tables. Keycloak handles all credential storage and cryptographic token signing.
3. **Persistent Keycloak Database**:
   - The PostgreSQL container hosts both `devtrack_db` (application tasks) and `keycloak_db` (Keycloak identities, clients, and credentials) on the `postgres_data` volume.

---

## Quickstart with Docker Compose

1. **Prepare environment variables**:
   ```bash
   cp .env.example .env
   ```
   *Review `.env` and configure strong secrets before running in non-development environments.*

2. **Start the complete stack**:
   ```bash
   docker compose up --build -d
   ```

3. **Check container health**:
   ```bash
   docker compose ps
   ```
   Ensure `devtrack-postgres`, `devtrack-keycloak`, `devtrack-backend`, and `devtrack-frontend` report healthy status.

4. **Open the application**:
   - Frontend: [http://localhost:5173](http://localhost:5173)

---

## Development Seed Accounts

For quick local testing, the development realm import (`devtrack-realm.json`) includes:

| Username | Password | Role | Permissions |
| :--- | :--- | :--- | :--- |
| `dev_user` | `password123` | `USER` | Personal tasks, personal dashboard metrics |
| `dev_admin` | `password123` | `ADMIN`, `USER` | Personal tasks + system telemetry metrics |

---

## Running Verification & Automated Tests

### 1. Backend Automated Tests (JUnit 5 + MockMvc + Mockito)
```bash
cd backend
mvn clean test
```
**Results: 44 tests passing**, including:
* Unauthorized requests return HTTP 401
* Authenticated `USER` can access `/api/tasks` and `/api/dashboard`
* Non-admin `USER` is denied access to `/api/admin/metrics` (HTTP 403)
* `ADMIN` can access `/api/admin/metrics` (HTTP 200)
* Non-owners are denied access to another user's private task (HTTP 403)
* **Admins are strictly denied access to another user's private task** (HTTP 403)
* Finite state machine valid and invalid status transitions
* JWT audience and Keycloak role converters

### 2. Frontend Typecheck and Production Build
```bash
cd frontend
npm run lint
npm run build
```
Validates TypeScript typings, component routing, and compiles the production Vite bundle without errors.

### 3. End-to-End Smoke Test
Run the automated test suite against the running Docker stack:
```bash
./scripts/smoke-test.sh
```
Verifies:
1. Backend Actuator health (`/actuator/health` ➔ 200)
2. Token generation from Keycloak via OIDC password grant
3. Authenticated task creation with Bearer token (`/api/tasks` ➔ 201)
4. RBAC enforcement: `dev_user` blocked from `/api/admin/metrics` (HTTP 403)
5. `dev_admin` successfully accesses `/api/admin/metrics` (HTTP 200)
6. Task ownership boundary: `dev_admin` blocked from `dev_user`'s private task (HTTP 403)
7. Keycloak self-registration endpoint availability (`/protocol/openid-connect/registrations` ➔ 200)
8. OpenID Connect discovery document verification (`/.well-known/openid-configuration` ➔ 200)

---

## Troubleshooting Guide

### 1. Redirect URI Mismatch (`Invalid parameter: redirect_uri`)
* **Cause**: The browser origin does not match the client's registered redirect URIs in Keycloak.
* **Fix**: Ensure the frontend is running on `http://localhost:5173`. If running on an alternative port, add `http://localhost:<PORT>/*` to `redirectUris` and `webOrigins` in `docker/keycloak/devtrack-realm.json` or in the Keycloak Admin Console.

### 2. Issuer Mismatch (`JWT iss claim does not match`)
* **Cause**: The token was issued by an issuer URL that differs from `KEYCLOAK_ISSUER`.
* **Fix**: The browser-facing URL is `http://localhost:8080/realms/devtrack`. The backend environment variable `KEYCLOAK_ISSUER` must match this exact value. Note that container-to-container calls use `KEYCLOAK_JWK_SET_URI: http://keycloak:8080/...` so Docker networking works seamlessly.

### 3. Audience Mismatch (`The required audience is missing or invalid in the JWT token`)
* **Cause**: The JWT does not contain `devtrack-backend` in the `aud` claim.
* **Fix**: Verify that the `audience-mapper` is present on the `devtrack-frontend` client in Keycloak, with `included.client.audience: devtrack-backend` and `access.token.claim: true`.

### 4. Role Mapping Issues (`403 Forbidden` on admin endpoints)
* **Cause**: Spring Security expects authorities with the `ROLE_` prefix (e.g. `ROLE_ADMIN`).
* **Fix**: `KeycloakJwtAuthenticationConverter` maps roles from `realm_access.roles` and `resource_access.<client>.roles` to uppercase `ROLE_<NAME>` authorities. Ensure the user is assigned the realm role `ADMIN`.

### 5. Resetting Local Development State
To reset PostgreSQL and Keycloak databases to a clean slate:
```bash
docker compose down -v
docker compose up --build -d
```

---

## License

This project is licensed under the [MIT License](LICENSE).
