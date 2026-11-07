---
name: spring-jpa-mapping
description: "Use when implementing Spring JPA entities, repositories, DTOs, entity-to-DTO mapping, persistence relationships, or query loading behavior."
---

# Spring JPA and DTO Mapping

## DTO requirements

```java
public final class BuildingDTOs {
    private BuildingDTOs() {}

    public record BuildingResponse(Long id, String name) {}

    public record CreateBuildingRequest(String name) {}
}
```

All new API DTOs must be Java records. Group every request and response DTO for one
domain or concept into one source file. A `final` holder class with nested records is
the recommended pattern where public types must share one file. Keep unrelated
domains in separate files. Existing DTOs may be migrated in a dedicated refactoring,
not incidentally during an unrelated change.

## Mapping rules

1. Never expose JPA entities from REST controllers.
2. Map entities to response DTOs in the service/mapping layer, following nearby
   project patterns and using MapStruct where it is already appropriate.
3. Load all required relationships intentionally. Avoid traversing lazy relations
   after the persistence context is closed or causing N+1 queries.
4. Keep request validation on record components with Jakarta Bean Validation
   annotations.
5. Preserve API field names and nullability when mapping; add tests for non-obvious
   or nested mappings.
