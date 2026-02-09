# User Story US026: Assign Water Meter

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US026 |
| **Story Name** | Assign Water Meter |
| **Epic** | Water Meter Management |
| **Related UC** | UC006 - Manage Water Meters |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** assign a water meter to a housing unit  
**So that** I can track which meter serves which apartment

---

## Acceptance Criteria

### AC1: Display Assign Meter Form
**Given** a unit has no water meter  
**When** I view unit details  
**Then** Water Meter section shows "No water meter assigned"  
**And** "Assign Meter" button is visible

### AC2: Assign First Meter
**Given** I click "Assign Meter"  
**When** I enter:
- Meter Number: WM-2024-001
- Location: Kitchen under sink
- Installation Date: 2024-01-01  

**And** I save  
**Then** meter is assigned  
**And** unit details show meter information

### AC3: Meter Without Location
**Given** I am assigning meter  
**When** I enter meter number and date only  
**And** leave location empty  
**And** I save  
**Then** meter is saved  
**And** location is NULL

### AC4: Validation - Meter Number Required
**Given** I am assigning meter  
**When** I leave meter number empty  
**Then** I see error "Meter number is required"

### AC5: Validation - Installation Date Required
**Given** I am assigning meter  
**When** I leave installation date empty  
**Then** I see error "Installation date is required"

### AC6: Validation - Date Not in Future
**Given** I am assigning meter  
**When** I enter date in future  
**Then** I see error "Installation date cannot be in future"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
