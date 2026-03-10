# User Story US030: Manage Person Bank Accounts (IBAN)

| Attribute | Value |
|-----------|-------|
| **Story ID** | US030 |
| **Epic** | Person Management |
| **Related UC** | UC006, UC016 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** register one or more IBANs for a person **so that** transaction imports can automatically suggest the related lease when a counterparty account matches.

## Acceptance Criteria

**AC1:** Person details page shows a "Bank Accounts (IBAN)" section listing all registered IBANs: IBAN (formatted with spaces), label (or "—"), primary badge (★ Primary).

**AC2:** "Add IBAN" button opens an inline form with fields: IBAN* (required), label (optional), Primary checkbox. Save → IBAN normalised (uppercase, spaces removed) and stored. Section refreshed.

**AC3:** Duplicate IBAN (same IBAN already registered on any person) → error "This IBAN is already registered."

**AC4:** At most one IBAN per person can be flagged as primary. Setting a new primary automatically clears the previous one.

**AC5:** Edit button per row → inline form pre-filled. All fields editable. Same uniqueness rules apply.

**AC6:** Delete button per row → confirmation dialog → HTTP 204. No cascade restriction on the IBAN itself.

**AC7:** During transaction import (UC015), `counterparty_account` is matched case-insensitively against all known IBANs. If a match is found, the system suggests the most suitable lease of the matched person (see UC016 — lease suggestion algorithm).

**Endpoints:**
- `GET /api/v1/persons/{personId}/bank-accounts` — HTTP 200
- `POST /api/v1/persons/{personId}/bank-accounts` — HTTP 201
- `PUT /api/v1/persons/{personId}/bank-accounts/{id}` — HTTP 200
- `DELETE /api/v1/persons/{personId}/bank-accounts/{id}` — HTTP 204

**Last Updated:** 2026-03-10 | **Status:** ✅ Implemented
