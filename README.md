# DevTrack — Production Developer Task Management Platform

[![CI Pipeline](https://github.com/devtrack/devtrack/actions/workflows/ci.yml/badge.svg)](https://github.com/devtrack/devtrack/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Keycloak](https://img.shields.io/badge/Keycloak-24.0.5-red.svg)](https://www.keycloak.org/)
[![React 18](https://img.shields.io/badge/React-18.3-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg)](https://www.typescriptlang.org/)

**DevTrack** is an enterprise-grade developer task management platform designed to reflect real-world production engineering practices. It features clean architecture, strict tenant isolation, finite-state-machine status transitions, OAuth 2.0 / OIDC authentication with Keycloak (PKCE flow), role-based access control (RBAC), and container orchestration.

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
     └───────────────────────────────┘ JWKS└─────────────┬─────────────────┘
                                           Fetch         │
                                                         │ 5. JPA / SQL
                                                         ▼
                                           ┌───────────────────────────────┐
                                           │       PostgreSQL 16           │
                                           │     Flyway Migrations         │
                                           └───────────────────────────────┘
```

### Key Architectural Highlights

1. **OAuth 2.0 Resource Server & Dual URLs**:
   - The browser communicates with Keycloak via `http://localhost:8080/realms/devtrack`.
   - The backend validates the JWT against the issuer `http://localhost:8080/realms/devtrack` while retrieving cryptographic JWKs over the Docker network at `http://keycloak:8080/realms/devtrack/protocol/openid-connect/certs`.
   - The backend strictly verifies that the token's audience claim (`aud`) contains `devtrack-backend`.
2. **Deterministic Ownership & Multi-Tenancy**:
   - The task owner identifier is resolved strictly on the backend from the verified JWT `sub` claim via `SecurityUtils.getCurrentUserId()`.
   - Client-supplied owner IDs are completely ignored.
   - If a user queries or mutates a task owned by another user, an HTTP 403 Forbidden (`TaskOwnershipException`) is returned.
3. **Admin Privilege Boundary (Privacy by Design)**:
   - Administrators (`ROLE_ADMIN`) can view aggregate platform metrics via `/api/admin/metrics`.
   - Administrators **cannot** inspect or modify private tasks owned by individual developers unless granted specific task ownership.
4. **Finite State Machine Status Validation**:
   - Enforced by `TaskStatusTransitionEngine`:
     - `OPEN` ➔ `IN_PROGRESS`, `COMPLETED`
     - `IN_PROGRESS` ➔ `IN_REVIEW`, `OPEN`, `COMPLETED`
     - `IN_REVIEW` ➔ `COMPLETED`, `IN_PROGRESS`
     - `COMPLETED` ➔ `OPEN`, `IN_PROGRESS`
   - Invalid status transitions throw `InvalidStatusTransitionException` returning HTTP 400 with a list of permissible transitions.

---

## Repository Structure

```
devTrack/
├── .github/workflows/ci.yml           # Automated CI workflow
├── backend/                           # Spring Boot 3.3 application
│   ├── src/main/java/com/devtrack/
│   │   ├── config/                    # Security, Web, OpenApi configuration
│   │   ├── controller/                # REST Controllers
│   │   ├── dto/                       # Request & Response records
│   │   ├── entity/                    # JPA Entities
│   │   ├── exception/                 # Centralized exception handling
│   │   ├── mapper/                    # Entity-to-DTO mappers
│   │   ├── repository/                # Spring Data JPA repositories & specs
│   │   ├── security/                  # Keycloak role converter & audience validator
│   │   └── service/                   # TaskService & TransitionEngine
│   ├── src/main/resources/
│   │   ├── db/migration/              # Flyway SQL migrations
│   │   ├── application.yml            # Base properties
│   │   ├── application-dev.yml        # Development profile
│   │   └── application-prod.yml       # Production profile
│   ├── Dockerfile                     # Multi-stage Maven/JRE build
│   └── pom.xml
├── frontend/                          # React + TypeScript + Vite SPA
│   ├── src/
│   │   ├── api/                       # Typed HTTP client & API endpoints
│   │   ├── components/                # Reusable UI components & SCSS
│   │   ├── context/                   # AuthContext (Keycloak integration)
│   │   ├── pages/                     # Dashboard & Admin Telemetry
│   │   └── styles/                    # Sass design system
│   ├── Dockerfile                     # Multi-stage Node/Nginx build
│   └── package.json
├── docker/
│   ├── keycloak/devtrack-realm.json   # Exported Keycloak realm & clients
│   └── postgres/init-dev.sql          # Postgres initialization script
├── scripts/
│   └── smoke-test.sh                  # Automated end-to-end integration test
├── docker-compose.yml                 # Orchestration with healthchecks
├── .env.example                       # Environment template
└── LICENSE                            # MIT License
```

---

## Prerequisites

* [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) (v2.20+)
* Or for local development:
  * Java 21 JDK
  * Apache Maven 3.9+
  * Node.js 20+ and npm 10+

---

## Quickstart with Docker Compose

1. **Clone the repository and prepare environment variables**:
   ```bash
   cp .env.example .env
   ```

2. **Start the complete stack**:
   ```bash
   docker compose up --build -d
   ```

3. **Check container status and health**:
   ```bash
   docker compose ps
   ```
   *Expected output*: `devtrack-postgres`, `devtrack-keycloak`, `devtrack-backend`, and `devtrack-frontend` should all show state `healthy` or `running`.

4. **Access the applications**:
   * **DevTrack Frontend**: [http://localhost:5173](http://localhost:5173)
   * **DevTrack REST API (OpenAPI / Swagger)**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
   * **Backend Health Check**: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
   * **Keycloak Administration**: [http://localhost:8080](http://localhost:8080) (Admin credentials: `admin` / `admin_secret_change_me_in_prod`)

---

## Pre-configured Development Users

The realm (`devtrack-realm.json`) automatically provisions the following accounts:

| Username | Password | Assigned Role | Capabilities |
| :--- | :--- | :--- | :--- |
| `dev_user` | `password123` | `USER` | Manage personal tasks, view personal dashboard |
| `dev_admin` | `password123` | `ADMIN`, `USER` | Manage personal tasks + inspect system telemetry |

---

## Running Automated Tests

### 1. Backend Automated Tests (JUnit 5 + Mockito + MockMvc)
```bash
cd backend
mvn clean test
```
*Executes 42 unit and slice tests covering:*
* Status transition validation and illegal transition exceptions
* DTO Jakarta Bean Validation
* Service-level task ownership isolation
* Keycloak JWT realm & client role conversion to Spring authorities
* Audience validator logic
* Web MVC security filter and endpoint authorization rules

### 2. Frontend Typecheck and Build
```bash
cd frontend
npm run lint
npm run build
```

### 3. End-to-End Smoke Test
Run the automated test suite against the running Docker stack:
```bash
./scripts/smoke-test.sh
```
The smoke test script verifies:
1. Backend health via Actuator.
2. Token generation from Keycloak using OIDC credentials.
3. Creation of a task with Bearer authentication.
4. Role-based rejection (HTTP 403) when `dev_user` attempts to access admin metrics.
5. Successful access (HTTP 200) when `dev_admin` queries admin metrics.
6. Multi-tenant privacy boundary: verifies that `dev_admin` receives HTTP 403 when trying to access `dev_user`'s private task.

---

## API Reference

| Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/actuator/health` | Public | System and database health status |
| `GET` | `/swagger-ui.html` | Public | OpenAPI 3 Interactive Documentation |
| `GET` | `/api/tasks` | `ROLE_USER` | Paginated list of tasks owned by caller |
| `POST` | `/api/tasks` | `ROLE_USER` | Create a new task owned by caller |
| `GET` | `/api/tasks/{id}` | `ROLE_USER` | Retrieve task details (owner-only) |
| `PUT` | `/api/tasks/{id}` | `ROLE_USER` | Update task details (owner-only) |
| `PATCH` | `/api/tasks/{id}/status` | `ROLE_USER` | Transition task status (owner-only) |
| `DELETE` | `/api/tasks/{id}` | `ROLE_USER` | Delete task (owner-only) |
| `GET` | `/api/dashboard` | `ROLE_USER` | Retrieve dashboard metrics for caller |
| `GET` | `/api/admin/metrics` | `ROLE_ADMIN` | Aggregated platform telemetry |

---

## Database Migrations (Flyway)

DevTrack uses Flyway to manage PostgreSQL schema evolution.
* Migrations are located in: `backend/src/main/resources/db/migration/`
* `V1__init_schema.sql`: Sets up the `tasks` table with check constraints, UUID primary keys, and indexes for multi-tenant querying.
* Production databases run with `hibernate.ddl-auto: validate` — schema is never generated automatically at runtime.

To add a future migration, create `V2__<description>.sql` in the migrations folder.

---

## Troubleshooting Guide

### 1. Keycloak startup delayed
Keycloak may take 30–45 seconds to initialize on initial boot while importing the realm. The `docker-compose.yml` healthcheck uses a 30s `start_period` with 10 retries. The backend will wait until Keycloak is ready before starting.

### 2. CORS or Redirect URI Mismatch
If running the frontend on a port other than `5173`, update `docker/keycloak/devtrack-realm.json` under `redirectUris` and `webOrigins`, and update `CORS_ALLOWED_ORIGINS` in `.env`.

### 3. Resetting the Local Environment
To completely reset the database and Keycloak data:
```bash
docker compose down -v
docker compose up --build -d
```

---

## License

This project is licensed under the [MIT License](LICENSE).
