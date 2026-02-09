# User Story US021: Set Initial Rent

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US021 |
| **Story Name** | Set Initial Rent |
| **Epic** | Rent Management |
| **Related UC** | UC005 - Manage Rents |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** set the initial rent amount for a housing unit  
**So that** I can establish the baseline rental price

---

## Acceptance Criteria

### AC1: Display Set Rent Form
**Given** a unit has no rent defined  
**When** I view unit details  
**Then** Rent section shows "No rent recorded"  
**And** "Set Rent" button is visible

### AC2: Set First Rent
**Given** I click "Set Rent"  
**When** I enter:
- Monthly Rent: €850.00
- Effective From: 2024-01-01  

**And** I save  
**Then** rent is saved  
**And** unit details show "€850.00/month"  
**And** effective from date shown

### AC3: Rent with Notes
**Given** I am setting rent  
**When** I add notes "Initial market rate"  
**And** I save  
**Then** notes are stored  
**And** visible in rent history

### AC4: Validation - Positive Amount
**Given** I am setting rent  
**When** I enter amount ≤ 0  
**Then** I see error "Rent must be positive"

### AC5: Validation - Date Required
**Given** I am setting rent  
**When** I leave effective from empty  
**Then** I see error "Effective from date is required"

### AC6: Currency Display
**Given** rent is €850.00  
**When** viewing unit details  
**Then** amount displays with € symbol  
**And** formatted as "€850.00/month"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
