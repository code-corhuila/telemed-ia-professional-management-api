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

This service implements the Professional Management bounded context for TeleMed IA. It manages professional profiles as a dedicated API service, while keeping the database schema and ownership in the separate `telemed-ia-professional-management-db` repository.

## Bounded context

Professional Management owns only the following business entities:

- Professional

Every professional is a general practitioner (`professionalType` `GENERAL_PRACTITIONER`, DEC-005): there is no specialty catalog.

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

Hexagonal architecture laid out as in Anexo C, under `co.edu.corhuila.telemedia.professionalmanagement`:

| Layer | Package |
|---|---|
| Domain | `domain.model` |
| Input ports (use cases, commands, queries) | `application.port.in` |
| Output ports | `application.port.out` |
| Use cases | `application.usecase` |
| HTTP adapter | `adapter.in.http` |
| Persistence adapter | `adapter.out.persistence` |
| Identity adapter | `adapter.out.identity` |
| Composition root | `app` |

The domain and the use cases import no framework (numeral 5.3.3; checked by
`FrameworkFreeCoreTest`): the composition root (`app.ProfessionalManagementConfiguration`)
creates the use cases and wires them to the adapters.

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

The persistence adapter is tested against PostgreSQL, not a substitute (numeral 5.3):
`ProfessionalRepositoryPostgresTest` uses `TEST_DATABASE_URL`, a disposable database with the
migrations of `telemed-ia-professional-management-db` applied, and Hibernate validates the
mapping against that schema. Without the variable those tests are skipped (Anexo I); the HTTP
tests run on an in-memory database.

## Run the API

```bash
mvn spring-boot:run
```

The application listens on port 8080 by default or on `SERVER_PORT` if set.

## Run in the platform

[`deploy/compose.yml`](deploy/compose.yml) declares the service `professional-management-api` on the
shared `platform` network, listening on 8080. It uses `expose`, not `ports`: only the API Gateway is
published to the host (Anexo G), which reaches the service as `http://professional-management-api:8080`.
It is the file `telemed-ia-infra-postgres` includes for this service (Anexo G). `JWT_PUBLIC_KEY` and
`PROFESSIONAL_MANAGEMENT_APP_PASSWORD` come from its environment file; the database is the shared
`postgres` service, with the migrations of `telemed-ia-professional-management-db` applied.

```bash
docker compose -f deploy/compose.yml up -d --build
```

## REST endpoints

| Method and route | Purpose | Request body | Successful response | Relevant errors |
|---|---|---|---|---|
| `GET /health` | Liveness, without a token (Anexo C). | — | `200 OK` `{"status":"UP"}` | — |
| `GET /api/professionals?page=&limit=` | List professionals from the most recently registered to the oldest (DEC-010), one page at a time (`page` from 1, `limit` 1 to 100, default 20), each with `fullName` of its account, and `email` only for `ADMIN` (DEC-008, DEC-009). | — | `200 OK` `{data, meta: {page, limit, total, totalPages}}` | `400 Bad Request` (page or limit out of range) |
| `GET /api/professionals/{professionalId}` | Get a professional by its UUID, with `fullName` of its account, and `email` only for `ADMIN` (DEC-008, DEC-009). | — | `200 OK` | `400 Bad Request` (not a UUID), `404 Not Found` |
| `PATCH /api/professionals/{professionalId}/status` | Change the profile status (FR-011, ADR-013, DEC-007). `ADMIN` only; the account and appointment availability are not touched. | `{"status": "ACTIVE" \| "INACTIVE"}`, no other field | `200 OK` with the professional; the same status again changes nothing | `400`, `403`, `404` |
| `POST /api/professionals/admin/register` | Register a professional: the account in the identity domain and an `ACTIVE` general practitioner profile (DEC-003). `ADMIN` only. | header `Idempotency-Key` (8 to 128 characters); `fullName`, `email`, `identityDocument`, `password` (8 or more), `licenseNumber`, `yearsExperience` | `201 Created` with `Location`; `200 OK` with the same professional when the key is repeated | `400` (each invalid field, the header included), `403`, `409` (e-mail, document or license already registered), `422` (key reused with another body), `503` (identity unavailable) |

## Authentication

Every route except `GET /health` requires `Authorization: Bearer <token>`, validated by the service
itself (numeral 5.3.7): `alg` exactly `RS256`, the RSA signature with the identity service's public
key (`JWT_PUBLIC_KEY` or `JWT_PUBLIC_KEY_FILE`), `exp` with 30 s of clock skew, and `sub`. Roles come
from the `role` claim (or a `roles` array). Any failure is `401 UNAUTHORIZED`. Registering a
professional is administrative (DEC-003): the use case answers
`403 FORBIDDEN` to any caller without the `ADMIN` role. Any authenticated caller reads professionals.

The account is created at `POST /internal/accounts` of the identity service (identity-and-access-api
#122), inside the platform network, with a service token of the client `professional-management`
(`POST /internal/service-token`) and the same `Idempotency-Key`. Identity's answers map to `409`
(e-mail or document registered), `422` (key reused with other data) and `400` (data it rejects, such
as a password over its limit). When identity cannot be reached or refuses the client, the answer is
`503 SERVICE_UNAVAILABLE` and nothing is stored. Without `SERVICE_CLIENT_SECRET` the service starts
and every registration answers `503` (`UnavailableIdentityAccounts`).

The list and the detail read the full name and e-mail of the accounts from the same service, one
call per page (`GET /internal/accounts?ids=...`, identity-and-access-api #128). When identity does
not answer, or without the secret, the professionals are returned without `fullName` and `email`.
The e-mail is returned only when the caller is an administrator; patients and professionals
receive the name without it (DEC-009).

## Errors and correlation

Every error answers with the shared envelope of Anexo C, `{error, message, details?, traceId}`:
`VALIDATION_ERROR` (400, one `details` entry per invalid field), `NOT_FOUND` (404, also for unknown
routes), `CONFLICT` (409), `BUSINESS_RULE_VIOLATION` (422) and `INTERNAL_ERROR` (500, neutral
message; the cause goes to the log only). The service reuses the received `X-Correlation-Id` or
generates one, returns it, uses it as `traceId`, and writes it on every JSON log line.

## Explicit limits

Declared in `src/main/resources/application.yml` (numeral 5.3.10): 5 s to receive the request
headers, 60 s for idle keep-alive connections, a pool of 10 connections with 5 s of wait,
`statement_timeout` of 5 s on every connection, graceful shutdown with 20 s, and 5 s per call to
the identity service (`IDENTITY_REQUEST_TIMEOUT`).

## Swagger / OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Database repository relationship

This repository does not create database migrations or duplicate the schema. The PostgreSQL database and its Liquibase migrations live in the separate repository `telemed-ia-professional-management-db`.

This application expects the external database to already exist and to honor the constraints required by the Professional Management bounded context.
