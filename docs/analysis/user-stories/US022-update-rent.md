# User Story US022: Update Rent Amount

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US022 |
| **Story Name** | Update Rent Amount |
| **Epic** | Rent Management |
| **Related UC** | UC005 - Manage Rents |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** update the rent amount when it changes  
**So that** I can track rent increases or decreases over time

---

## Acceptance Criteria

### AC1: Display Update Rent Form
**Given** a unit has current rent €850.00  
**When** I click "Update Rent"  
**Then** update form is displayed  
**And** current rent shown as read-only  
**And** new rent field is empty

### AC2: Increase Rent
**Given** current rent is €850.00  
**When** I enter new rent €900.00  
**And** effective from 2024-07-01  
**And** I save  
**Then** new rent becomes current  
**And** old rent gets effective_to = 2024-06-30  
**And** both preserved in history

### AC3: Show Change Calculation
**Given** I am updating from €850 to €900  
**When** I enter new amount  
**Then** I see calculated change: "+€50.00 (+5.88%)" in green

### AC4: Decrease Rent
**Given** current rent is €900.00  
**When** I enter €850.00  
**Then** change shows "-€50.00 (-5.56%)" in red  
**And** update is saved

### AC5: Automatic Period Closure
**Given** current rent from 2024-01-01 with no end date  
**When** I add new rent from 2024-07-01  
**Then** old rent's effective_to = 2024-06-30  
**And** new rent's effective_to = NULL

### AC6: Validation - New Rent Required
**Given** I am updating rent  
**When** I leave new rent empty  
**Then** I see error "New rent is required"

### AC7: Validation - Date Not Before Current
**Given** current rent from 2024-06-01  
**When** I try effective from 2024-01-01  
**Then** I see error "Cannot backdate before current period"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
