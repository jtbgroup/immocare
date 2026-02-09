# User Story US008: Delete Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US008 |
| **Story Name** | Delete Housing Unit |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 - Manage Housing Units |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** delete a housing unit  
**So that** I can remove units that no longer exist or were created by mistake

---

## Acceptance Criteria

### AC1: Delete Empty Unit
**Given** a unit exists with no rooms, PEB, rent, or meter data  
**When** I click "Delete" on unit details page  
**And** I confirm deletion  
**Then** the unit is permanently removed  
**And** I am redirected to building details  
**And** unit is no longer in the list

### AC2: Cannot Delete Unit with Rooms
**Given** a unit has 3 rooms  
**When** I try to delete it  
**Then** I see error "Cannot delete housing unit"  
**And** message shows "This unit has 3 room(s)"  
**And** unit is NOT deleted

### AC3: Cannot Delete Unit with PEB History
**Given** a unit has PEB score history  
**When** I try to delete it  
**Then** I see error listing associated data  
**And** unit is NOT deleted

### AC4: Confirmation Dialog
**Given** I click delete on an empty unit  
**When** the confirmation dialog appears  
**And** I click "Cancel"  
**Then** the dialog closes  
**And** the unit is NOT deleted

### AC5: Building Unit Count Updated
**Given** a building has 5 units  
**When** I delete one unit successfully  
**Then** building details show "4 units"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
