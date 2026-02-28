# User Story US044: Create Person

| Attribute | Value |
|-----------|-------|
| **Story ID** | US044 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** create a person record **so that** I can assign them as owner or tenant.

## Acceptance Criteria

**AC1:** Click "Add Person" → form with: last name*, first name*, optional: birth date, birth place, national ID, GSM, email, address fields. Country defaults to Belgium.
**AC2:** Fill required fields → save → "Person created successfully", redirected to person details.
**AC3:** National ID already used → error "This national ID is already assigned to another person".
**AC4:** Invalid email format → error "Please enter a valid email address".
**AC5:** Cancel → no person created.

**Endpoint:** `POST /api/v1/persons` — HTTP 201.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
