# telemed-ia-professional-management-api

> professional-management bounded context: service API

Part of the **LMS Library** distributed system — team `lms-library`, Grupo 2.
Governance and documentation live in [`library-docs`](https://github.com/code-corhuila/library-docs).

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

Full policy: `00-governance/branching-policy.md` in `library-docs`.

## Purpose

This service implements the Professional Management bounded context for TeleMed IA. It manages professionals and specialties as a dedicated API service, while keeping the database schema and ownership in the separate `telemed-ia-professional-management-db` repository.

## Bounded context

Professional Management owns only the following business entities:

- Professional
- Specialty

This service does not create users, roles, sessions, passwords or JWTs. Those belong to the Identity & Access bounded context.

## Technology stack

- Java 17
- Spring Boot 3.3.13
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

1. Ensure Java 17 and Maven are installed.
2. Provision a PostgreSQL database for this bounded context.
3. Export the required environment variables.
4. Start the API from the project root.

## Required environment variables

```bash
export DB_URL=jdbc:postgresql://localhost:5432/telemed_professional_management
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export SERVER_PORT=8080
```

## Run tests

```bash
mvn test
```

## Build the project

```bash
mvn package
```

## Run the API

```bash
mvn spring-boot:run
```

The application listens on port 8080 by default or on `SERVER_PORT` if set.

## REST endpoints

### Professionals

- `GET /api/professionals`
- `GET /api/professionals/{professionalId}`
- `POST /api/professionals`

### Specialties

- `GET /api/specialties`
- `POST /api/specialties`
- `PUT /api/specialties/{specialtyId}`

## Swagger / OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Database repository relationship

This repository does not create database migrations or duplicate the schema. The PostgreSQL database and its Liquibase migrations live in the separate repository `telemed-ia-professional-management-db`.

This application expects the external database to already exist and to honor the constraints required by the Professional Management bounded context.
