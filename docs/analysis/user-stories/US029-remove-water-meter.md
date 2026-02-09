# User Story US029: Remove Water Meter

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US029 |
| **Story Name** | Remove Water Meter |
| **Epic** | Water Meter Management |
| **Related UC** | UC006 - Manage Water Meters |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** remove a water meter without replacing it  
**So that** I can handle cases where a meter is disconnected

---

## Acceptance Criteria

### AC1: Display Remove Meter Dialog
**Given** a unit has active meter  
**When** I click "Remove Meter"  
**Then** confirmation dialog appears:
- "Remove water meter?"
- Meter number shown
- "Unit will have no active meter"
- "You can assign a new meter later"

### AC2: Remove Meter Successfully
**Given** active meter "WM-2024-001"  
**When** I confirm removal  
**And** enter removal date 2024-12-31  
**Then** meter's removal_date = 2024-12-31  
**And** unit has no active meter  
**And** Water Meter section shows "No water meter assigned"

### AC3: Removal Date Prompt
**Given** I am removing meter  
**When** confirmation dialog shows  
**Then** I see date picker for removal date  
**And** default is today's date

### AC4: Validation - Removal Date After Installation
**Given** meter installed 2024-06-01  
**When** I try removal date 2024-01-01  
**Then** I see error "Removal date must be after installation date"

### AC5: Cancel Removal
**Given** removal dialog is displayed  
**When** I click "Cancel"  
**Then** dialog closes  
**And** meter is NOT removed

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
