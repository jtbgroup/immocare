# Skill: API Contract & Type Synchronization

The repository currently has no declared OpenAPI specification or client-generation pipeline. Keep contracts synchronized manually unless such a pipeline is deliberately introduced and documented.

Java DTOs must be records, grouped into one source file per domain or concept. TypeScript interfaces/types should mirror the API contract and remain in the matching frontend domain/model area.

## Type Mapping Matrix

| Java / Spring Type            | TypeScript / Angular Type | Example Values           |
| :---------------------------- | :------------------------ | :----------------------- |
| `String` / `UUID`             | `string`                  | `"user-123"`             |
| `Long` / `Integer` / `Double` | `number`                  | `42`                     |
| `Boolean`                     | `boolean`                 | `true`                   |
| `Instant` / `LocalDateTime`   | `string` (ISO-8601)       | `"2026-10-05T18:00:00Z"` |
| `List<T>` / `Set<T>`          | `T[]`                     | `[1, 2, 3]`              |
| `Enum`                        | String Union / TS Enum    | `'ACTIVE' \| 'INACTIVE'` |

## Pattern: Java Record to TypeScript Interface

### Java DTO (Backend)

```java
public final class UserDTOs {
    private UserDTOs() {}

    public record UserResponse(
        Long id,
        String email,
        UserRole role,
        Instant createdAt
    ) {}
}
```

### Generated TypeScript Interface (Frontend)

```typescript
export type UserRole = "ADMIN" | "USER" | "GUEST";

export interface UserResponse {
  id: number;
  email: string;
  role: UserRole;
  createdAt: string; // ISO-8601 Timestamp
}
```

## Conversion Rules

1. Model nullable response fields as `T | null`; use `?` only when a field may be absent from the serialized object. Do not treat Java `Optional<T>` alone as proof that JSON omits the field.
2. Convert Java enums to TypeScript string unions or existing project enum types, preserving the exact serialized values.
3. Ensure exact field names and nesting match the serialized record shape.
4. Java `Long` serializes to a JSON number and normally maps to TypeScript `number`; if identifiers can exceed JavaScript's safe integer range, define and document a string-based API representation instead.
5. `LocalDate`, `LocalDateTime`, `Instant`, and zoned timestamps must be checked against actual Jackson serialization and API expectations before choosing a TypeScript date/string representation. Do not assume all timestamps are UTC `Z`.
6. Verify route, HTTP verb, response status, estate scope, and authorization alongside field types.
7. Update or add contract tests when changing shared request/response fields.
