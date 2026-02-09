# User Story US028: View Water Meter History

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US028 |
| **Story Name** | View Water Meter History |
| **Epic** | Water Meter Management |
| **Related UC** | UC006 - Manage Water Meters |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** view the complete water meter history of a housing unit  
**So that** I can see all past meter assignments and replacements

---

## Acceptance Criteria

### AC1: Display Current Meter
**Given** a unit has active meter "WM-2024-002"  
**When** I view unit details  
**Then** Water Meter section shows:
- Meter number: WM-2024-002
- Installation date
- Location (if specified)
- Status badge: "Active"

### AC2: View Complete History
**Given** a unit has meter history:
- WM-2024-001 (2024-01-01 to 2024-06-01)
- WM-2024-002 (2024-06-01 to present)  

**When** I click "View History"  
**Then** history table shows both meters  
**And** sorted by date (newest first)

### AC3: History Table Columns
**Given** I am viewing meter history  
**Then** table shows:
- Meter Number
- Location
- Installation Date
- Removal Date (or "Active")
- Duration
- Status

### AC4: Calculate Duration
**Given** meter from 2024-01-01 to 2024-06-01  
**When** viewing history  
**Then** duration shows "5 months"

### AC5: Status Indicators
**Given** viewing meter history  
**Then** active meter shows green "Active" badge  
**And** replaced meters show gray "Replaced" badge

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
