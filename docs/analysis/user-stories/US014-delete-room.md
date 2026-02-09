# User Story US014: Delete Room

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US014 |
| **Story Name** | Delete Room |
| **Epic** | Room Management |
| **Related UC** | UC003 - Manage Rooms |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** delete a room from a housing unit  
**So that** I can remove rooms that were added by mistake or no longer exist

---

## Acceptance Criteria

### AC1: Delete Room Successfully
**Given** a unit has 3 rooms  
**When** I click delete icon next to a room  
**And** I confirm deletion  
**Then** the room is removed  
**And** room list shows 2 rooms  
**And** total surface is recalculated

### AC2: Confirmation Dialog
**Given** I click delete on a room  
**Then** a confirmation dialog appears:
- "Delete this room?"
- Room type and surface shown
- "This action cannot be undone"  

**When** I click "Confirm"  
**Then** room is deleted

### AC3: Cancel Deletion
**Given** confirmation dialog is displayed  
**When** I click "Cancel"  
**Then** dialog closes  
**And** room is NOT deleted

### AC4: Total Surface Recalculation
**Given** a unit has rooms: 20, 15, 10 m² (total 45)  
**When** I delete the 15 m² room  
**Then** total surface shows 30 m²

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
