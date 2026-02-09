# User Story US023: View Rent History

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US023 |
| **Story Name** | View Rent History |
| **Epic** | Rent Management |
| **Related UC** | UC005 - Manage Rents |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** view the complete rent history of a housing unit  
**So that** I can see all past rent amounts and changes

---

## Acceptance Criteria

### AC1: Display Current Rent
**Given** a unit has current rent €900.00 from 2024-07-01  
**When** I view unit details  
**Then** Rent section shows:
- "€900.00/month"
- "Effective from: 2024-07-01"
- "View History" link

### AC2: View Complete History
**Given** a unit has rent history:
- €800 (2024-01-01 to 2024-06-30)
- €900 (2024-07-01 to present)  

**When** I click "View History"  
**Then** history table shows both periods  
**And** sorted by date (newest first)

### AC3: History Table Columns
**Given** I am viewing rent history  
**Then** table shows:
- Monthly Rent (€)
- Effective From
- Effective To (or "Current")
- Duration (calculated)
- Notes

### AC4: Calculate Duration
**Given** rent period from 2024-01-01 to 2024-06-30  
**When** viewing history  
**Then** duration shows "6 months"

### AC5: Show Last Change
**Given** rent changed from €800 to €900  
**When** viewing unit details  
**Then** I see "Last change: +€100 on 2024-07-01"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
