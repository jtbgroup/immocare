# User Story US005: Search Buildings

| Attribute | Value |
|-----------|-------|
| **Story ID** | US005 |
| **Epic** | Building Management |
| **Related UC** | UC001 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** search buildings **so that** I can quickly find a specific property.

## Acceptance Criteria

**AC1:** Type "Soleil" → only buildings with "Soleil" in name shown.
**AC2:** Type "Loi" → buildings with "Loi" in address shown.
**AC3:** Case-insensitive: "SOLEIL" finds "Résidence Soleil".
**AC4:** Partial match: "Aven" finds "Avenue Louise" and "Avenue Tervueren".
**AC5:** Clear search field → all buildings shown again.
**AC6:** No match for "XYZ" → "No buildings found matching 'XYZ'".
**AC7:** City filter + search combined → intersection of both filters applied.

**Endpoint:** `GET /api/v1/buildings?search={term}` — partial case-insensitive match on name + address. `GET /api/v1/buildings/cities` — distinct city list.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
