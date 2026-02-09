# User Story US013: Edit Room

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US013 |
| **Story Name** | Edit Room |
| **Epic** | Room Management |
| **Related UC** | UC003 - Manage Rooms |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** edit room information  
**So that** I can correct mistakes or update room details

---

## Acceptance Criteria

### AC1: Edit Room Type
**Given** a room exists as "Bedroom"  
**When** I click edit on the room  
**And** I change type to "Office"  
**And** I save  
**Then** room type is updated to "Office"  
**And** room list reflects the change

### AC2: Edit Room Surface
**Given** a room has surface 15.5 m²  
**When** I change surface to 18.0 m²  
**And** I save  
**Then** surface is updated  
**And** total unit surface is recalculated

### AC3: Inline Edit (Optional)
**Given** I am viewing room list  
**When** I click edit icon next to a room  
**Then** the row becomes editable  
**Or** a modal edit form appears  
**And** current values are pre-filled

### AC4: Cancel Edit
**Given** I am editing a room  
**And** I have changed the surface  
**When** I click "Cancel"  
**Then** changes are discarded  
**And** original values remain

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
