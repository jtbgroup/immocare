---
title: Technology Stack and Deployment
type: technical reference
status: current
last_verified: 2026-11-05
verified_against: b99e38f
---

# Technology Stack and Deployment

This page records the confirmed target architecture and distinguishes it from
implementation details that have not yet been aligned. The repository and this page
are the source of truth for stack and deployment decisions.

## Application architecture

- The application is maintained as a monorepo with the Angular frontend in
  `frontend/` and the Spring Boot backend in `backend/`.
- Production presents the combined application through one public HTTP entry point.
  The current multi-stage [`Dockerfile`](../../Dockerfile) packages the Angular
  application with Nginx and the Spring Boot JAR.
- The production Compose configuration publishes the application on port `8090`.

## Target stack

| Area | Confirmed target |
|---|---|
| Backend language | Java 21 or later |
| Backend framework | Spring Boot 4 or later |
| Authentication | Database-backed local authentication with server-side sessions |
| Password hashing | BCrypt with work factor 12 |
| Persistence | Spring Data JPA |
| Schema migrations | Flyway, with sequential versioned and descriptive migration names |
| Database | PostgreSQL 17 or later |
| Frontend | Angular 19 or later |
| Frontend components | Standalone components |
| Frontend routing | Lazy-load feature routes/components using Angular's standalone routing APIs |
| Frontend templates | Use Angular built-in control flow such as `@if` and `@for` |

Angular Material is not a required UI library.

## Deployment and development

Docker Compose remains a supported part of the project. The repository currently
contains separate development and production Compose configurations; development is
intended to provide frontend and backend hot reload. The production image is built
with a multi-stage Dockerfile and is exposed on port `8090`.

The confirmed development port targets are `4300` for the frontend and `8080` for
the backend. The current development Compose configuration instead exposes `4200`
and `8081`, alongside the Nginx entry point on `8090`. Aligning the Compose
configurations into a single supported environment while retaining Docker Compose
is tracked in the [project backlog](../../backlog/backlog.md).

The current Compose files use PostgreSQL 16 images, below the confirmed PostgreSQL
17+ target. This is a known difference between the target stack and the current
configuration.

## Authentication extensibility

Database-backed local authentication with server-side sessions remains the initial
authentication requirement. Adding OAuth2/Keycloak support is a future item in the
[project backlog](../../backlog/backlog.md); its implementation constraints will be
decided when that work is planned.

## Documentation and traceability

Canonical business and technical references live under `documentation/` and follow
the [documentation methodology](../methodology.md). Existing use-case, user-story,
and generation-prompt files under `docs/` are retained as non-canonical historical
analysis artifacts; new work does not require creating more of them.

Flyway migrations use descriptive names and are not required to map one-to-one to
use cases. Document material schema or persistence behavior changes in the relevant
canonical technical reference.

## Current implementation sources

- [Backend build and Java target](../../backend/pom.xml)
- [Frontend dependencies](../../frontend/package.json)
- [Lazy-loaded frontend routes](../../frontend/src/app/app.routes.ts)
- [Security and password encoder configuration](../../backend/src/main/java/com/immocare/config/SecurityConfig.java)
- [Development Compose configuration](../../docker-compose.dev.yml)
- [Production Compose configuration](../../docker-compose.yml)
- [Production multi-stage image](../../Dockerfile)
