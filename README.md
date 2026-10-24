# telemed-ia-professional-management-api

> professional-management bounded context: service API

Part of the **TeleMed IA** distributed system — Grupo 2.
Governance and documentation live in [`telemed-ia-docs`](https://github.com/code-corhuila/telemed-ia-docs).

## Branching

Three permanent branches. **None of them accepts a direct commit** — you enter through a child
branch and leave through a Pull Request.

```
develop  <--PR--  feat/... fix/... chore/...
qa       <--PR--  qa/...
main     <--PR--  release/...  hotfix/...
```

Promotion happens **by re-application** (`git cherry-pick -x`), never by merging one permanent
branch into another: `merge develop -> qa` and `merge qa -> main` do not exist in this model.

`main` requires **1 approval from `ariel5253`**. On `develop` and `qa` the team sets its own review
rule.

Full policy: `00-governance/` in `telemed-ia-docs`.

## Purpose

This service implements the Professional Management bounded context for TeleMed IA. It manages professionals and specialties as a dedicated API service, while keeping the database schema and ownership in the separate `telemed-ia-professional-management-db` repository.

## Bounded context

Professional Management owns only the following business entities:

- Professional
- Specialty

This service does not create users, roles, sessions, passwords or JWTs. Those belong to the Identity & Access bounded context.

## Technology stack

- Java 21 (Anexo I)
- Spring Boot 3.5.6
- Maven
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL driver
- Spring Boot Actuator
- Spring Boot Test
- OpenAPI / Swagger (springdoc)

## Architecture

The project follows hexagonal architecture with separation between:

- domain
- application
- ports
- infrastructure/adapters
- interfaces
- configuration

The domain layer does not depend on Spring, JPA or REST components, and persistence entities remain in the infrastructure layer.

## Local setup

1. Ensure Java 21 and Maven are installed.
2. Start the shared PostgreSQL instance of `telemed-ia-infra-postgres` and apply the
   migrations of `telemed-ia-professional-management-db` (Anexo J).
3. Export the required environment variables.
4. Start the API from the project root.

## Required environment variables

Every variable the service reads is listed, with sample values only, in [`.env.example`](.env.example).
Real values come from the environment file of `telemed-ia-infra-postgres`; never commit them.

## Build and test

The same command as the CI (`.github/workflows/ci.yml`):

```bash
mvn -B verify
```

## Run the API

```bash
mvn spring-boot:run
```

The application listens on port 8080 by default or on `SERVER_PORT` if set.

## REST endpoints

| Method and route | Purpose | Request body | Successful response | Relevant errors |
|---|---|---|---|---|
| `GET /health` | Liveness, without a token (Anexo C). | — | `200 OK` `{"status":"UP"}` | — |
| `GET /api/professionals` | List professionals. | — | `200 OK` | — |
| `GET /api/professionals/{professionalId}` | Get a professional by ID. | — | `200 OK` | `404 Not Found` |
| `POST /api/professionals` | Register a professional. | `identityUserId` (number), `licenseNumber` (string), `specialtyId` (number), `yearsExperience` (integer) | `201 Created` | `400 Bad Request`, `404 Not Found` (specialty), `409 Conflict` (duplicate identity or license) |
| `GET /api/specialties` | List specialties. | — | `200 OK` | — |
| `POST /api/specialties` | Create a specialty. | `name` (string), `description` (string or null) | `201 Created` | `400 Bad Request`, `409 Conflict` (duplicate name) |
| `PUT /api/specialties/{specialtyId}` | Update a specialty. | `name` (string), `description` (string or null) | `200 OK` | `400 Bad Request`, `404 Not Found`, `409 Conflict` (duplicate name) |

## Authentication

Every route except `GET /health` requires `Authorization: Bearer <token>`, validated by the service
itself (numeral 5.3.7): `alg` exactly `RS256`, the RSA signature with the identity service's public
key (`JWT_PUBLIC_KEY` or `JWT_PUBLIC_KEY_FILE`), `exp` with 30 s of clock skew, and `sub`. Roles come
from the `role` claim (or a `roles` array). Any failure is `401 UNAUTHORIZED`. Registering a
professional and changing the catalog are administrative (DEC-003): the use cases answer
`403 FORBIDDEN` to any caller without the `ADMIN` role. Any authenticated caller reads professionals.

## Errors and correlation

Every error answers with the shared envelope of Anexo C, `{error, message, details?, traceId}`:
`VALIDATION_ERROR` (400, one `details` entry per invalid field), `NOT_FOUND` (404, also for unknown
routes), `CONFLICT` (409), `BUSINESS_RULE_VIOLATION` (422) and `INTERNAL_ERROR` (500, neutral
message; the cause goes to the log only). The service reuses the received `X-Correlation-Id` or
generates one, returns it, uses it as `traceId`, and writes it on every JSON log line.

## Swagger / OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Database repository relationship

This repository does not create database migrations or duplicate the schema. The PostgreSQL database and its Liquibase migrations live in the separate repository `telemed-ia-professional-management-db`.

This application expects the external database to already exist and to honor the constraints required by the Professional Management bounded context.
