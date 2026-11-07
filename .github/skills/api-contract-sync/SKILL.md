---
name: api-contract-sync
description: "Keep Spring API DTOs, routes, authorization, and Angular models/services synchronized when changing a backend/frontend contract."
---

# API Contract and Type Synchronization

The repository currently has no declared OpenAPI specification or client-generation
pipeline. Keep contracts synchronized manually unless such a pipeline is
deliberately introduced and documented.

## Type mapping

| Java / Spring Type | TypeScript / Angular Type | Notes |
|---|---|---|
| `String` / `UUID` | `string` | Preserve serialized identifiers |
| `Long` / `Integer` / `Double` | `number` | Consider safe-integer limits for large identifiers |
| `Boolean` | `boolean` | |
| `Instant` / `LocalDateTime` | `string` | Confirm actual serialization and timezone |
| `List<T>` / `Set<T>` | `T[]` | |
| `Enum` | String union / TS enum | Preserve exact serialized values |

## Contract update procedure

1. Read the backend request/response DTOs, controller route, service behavior, and
   relevant authorization checks.
2. Update matching TypeScript models and Angular services to reflect exact field
   names, nesting, nullability, routes, HTTP verbs, and response behavior.
3. Model nullable response fields as `T | null`; use `?` only when a property may be
   absent from the serialized object. Java `Optional<T>` alone does not establish
   that a JSON field is omitted.
4. Confirm timestamp serialization with the actual Jackson configuration and API
   behavior. Do not assume all timestamps use UTC `Z`.
5. Review estate scoping and cross-estate isolation; frontend filtering is not
   authorization. Load
   [`estate-access-control`](../estate-access-control/SKILL.md) for estate-owned
   contracts.
6. Add or update focused backend and frontend contract tests when shared fields or
   behavior change.
7. If the contract behavior is documented, follow
   [`maintain-documentation`](../maintain-documentation/SKILL.md) and update links
   to the canonical contract documentation.

## DTO conventions

New API DTOs are Java records grouped by domain or concept in one Java source file.
TypeScript interfaces or types mirror the public serialized shape. Never expose JPA
entities from REST controllers.
