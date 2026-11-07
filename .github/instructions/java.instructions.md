---
description: "Use when creating or changing Java backend code, Spring controllers, services, repositories, DTOs, persistence, or tests."
name: "Java backend conventions"
applyTo: ["backend/**/*.java", "backend/**/*.sql", "backend/pom.xml"]
---

# Java backend conventions

- Follow the target Java 21+ and Spring Boot 4+ stack. Use Spring Security with
  database-backed local authentication and server-side sessions; encode passwords
  with BCrypt work factor 12.
- Use Spring Data JPA and Flyway with PostgreSQL 17+ as the database target.
- Follow the existing `Controller -> Service -> Repository` boundaries and nearby
  project patterns; avoid unrelated refactoring.
- Use Java records for new request and response DTOs. Never expose JPA entities from
  REST controllers, and validate request DTOs with Jakarta Bean Validation.
- Give Flyway migrations sequential versions and descriptive domain names. Do not
  require use-case IDs or a one-migration-per-use-case mapping.
- Scope estate-owned operations to the requested estate, enforce the appropriate
  authorization, and verify ownership of referenced resources.
- Preserve the established exception and API error-response conventions.
- Add or update focused tests for behavior, validation, authorization, and data
  isolation affected by the change.
- Update the relevant canonical technical documentation when a change materially
  affects API or persistence behavior.
- For entity/DTO mapping and persistence behavior, load
  [`spring-jpa-mapping`](../skills/spring-jpa-mapping/SKILL.md). For authorization
  details, load [`estate-access-control`](../skills/estate-access-control/SKILL.md).
