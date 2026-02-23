# User Story US022: Edit a Rent Record

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US022 |
| **Story Name** | Edit a Rent Record |
| **Epic** | Rent Management |
| **Related UC** | UC005 - Manage Rents |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN
**I want to** edit an existing rent record
**So that** I can correct mistakes or adjust amounts and dates

---

## Acceptance Criteria

### AC1: Edit Button in History
**Given** a unit has rent history
**When** I open the history panel
**Then** each row has an ✏️ edit button

### AC2: Form Pre-filled
**Given** I click ✏️ on a rent record
**Then** the inline form opens pre-filled with:
- Current monthly rent amount
- Current effective from date
- Current notes

### AC3: Show Change Preview
**Given** I am editing a record and modify the amount
**When** the record has a previous (older) record
**Then** I see a live change preview: "+€50.00 (+5.88%)" in green or "-€50.00 (-5.56%)" in red

### AC4: Adjacent Periods Recalculated on Save
**Given** I change the `effective_from` date of a record
**When** I save
**Then** the previous record's `effective_to` = new `effectiveFrom - 1 day`
**And** this record's `effective_to` = next record's `effectiveFrom - 1 day` (or NULL if most recent)

### AC5: Current Rent Card Updated
**Given** I edited the most recent rent record
**When** the save completes
**Then** the current rent card reflects the new amount and date

### AC6: Validation — Positive Amount
**Given** I am editing a rent record
**When** I enter amount ≤ 0
**Then** I see error "Rent must be positive"

### AC7: Validation — Date Required
**Given** I am editing a rent record
**When** I clear the effective from date
**Then** I see error "Effective from date is required"

### AC8: Validation — Not Too Far in Future
**Given** I am editing a rent record
**When** I set a date more than 1 year in the future
**Then** I see error "Effective from date cannot be more than 1 year in the future"

---

**Last Updated**: 2026-02-23
**Status**: ✅ Implemented
