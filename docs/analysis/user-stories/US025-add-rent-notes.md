# User Story US025: Add Notes to Rent Changes

| Attribute | Value |
|-----------|-------|
| **Story ID** | US025 |
| **Epic** | Rent Management |
| **Related UC** | UC005 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 1 |

**As an** ADMIN **I want to** add notes when changing rent **so that** I can document the reason for the change.

## Acceptance Criteria

**AC1:** Add notes "Initial market rate" when setting rent → notes stored and shown in history.
**AC2:** Add notes "Annual indexation +5%" when updating rent → notes stored with new record.
**AC3:** History table shows notes column; empty if none.
**AC4:** Leave notes empty → rent saved successfully (notes optional, stored as NULL).
**AC5:** (Optional) Suggest common templates: "Annual indexation", "Market adjustment", "After renovation", "Tenant negotiation".

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
