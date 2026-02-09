# User Story US027: Replace Water Meter

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US027 |
| **Story Name** | Replace Water Meter |
| **Epic** | Water Meter Management |
| **Related UC** | UC006 - Manage Water Meters |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** replace a water meter with a new one  
**So that** I can track meter changes when they are upgraded or broken

---

## Acceptance Criteria

### AC1: Display Replace Meter Form
**Given** a unit has active meter "WM-2024-001"  
**When** I click "Replace Meter"  
**Then** replace form is displayed  
**And** current meter shown as read-only  
**And** new meter fields are empty

### AC2: Replace Meter Successfully
**Given** current meter is "WM-2024-001" from 2024-01-01  
**When** I enter:
- New Meter Number: WM-2024-002
- Installation Date: 2024-06-01
- Reason: Broken  

**And** I save  
**Then** old meter's removal_date = 2024-06-01  
**And** new meter becomes active  
**And** both preserved in history

### AC3: Automatic Removal Date
**Given** I am replacing meter  
**When** new meter installation = 2024-06-01  
**And** I save  
**Then** old meter removal_date automatically set to 2024-06-01

### AC4: Replacement Reason Dropdown
**Given** I am replacing meter  
**When** I click reason field  
**Then** I see options:
- Broken
- End of life
- Upgrade
- Calibration issue
- Other

### AC5: Validation - Different Meter Number
**Given** current meter is "WM-001"  
**When** I enter same number "WM-001" as new meter  
**Then** I see warning "New meter number same as current"  
**And** can still continue if confirmed

### AC6: Validation - Installation Not Before Current
**Given** current meter from 2024-06-01  
**When** I try installation 2024-01-01  
**Then** I see error "Installation cannot be before current meter"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
