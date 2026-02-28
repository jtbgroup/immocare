# User Story US048: Person Picker (Search)

| Attribute | Value |
|-----------|-------|
| **Story ID** | US048 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** search for persons via an autocomplete picker **so that** I can quickly find and link a person when assigning owners or tenants.

## Acceptance Criteria

**AC1:** Type 1 char → no suggestions. Type 2+ chars → up to 10 matching persons shown within 300ms.
**AC2:** Search matches last name, first name, or national ID (case-insensitive, partial).
**AC3:** Each suggestion shows: full name + city + (optional) national ID last 4 digits.
**AC4:** No match → "No person found. Create new?" shortcut shown.
**AC5:** Select a suggestion → person linked, picker closed.

**Endpoint:** `GET /api/v1/persons/search?q={term}&limit=10` — min 2 chars, returns PersonSummaryDTO.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
