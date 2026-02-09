# User Story US012: Add Room to Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US012 |
| **Story Name** | Add Room to Housing Unit |
| **Epic** | Room Management |
| **Related UC** | UC003 - Manage Rooms |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** add rooms to a housing unit  
**So that** I can define the composition and layout of the apartment

---

## Acceptance Criteria

### AC1: Display Add Room Form
**Given** I am viewing a housing unit  
**When** I click "Add Room" button  
**Then** the room creation form is displayed  
**And** unit is pre-selected  
**And** room type dropdown shows all types

### AC2: Create Room Successfully
**Given** I am on add room form  
**When** I select type "Bedroom"  
**And** I enter surface 15.5 m²  
**And** I click "Save"  
**Then** the room is created  
**And** I see success message  
**And** room appears in unit's room list  
**And** total surface is recalculated

### AC3: Room Type Dropdown Options
**Given** I am adding a room  
**When** I click the room type dropdown  
**Then** I see all options:
- Living Room, Bedroom, Kitchen, Bathroom
- Toilet, Hallway, Storage, Office
- Dining Room, Other

### AC4: Validation - Type Required
**Given** I am adding a room  
**When** I leave type unselected  
**And** I click "Save"  
**Then** I see error "Room type is required"

### AC5: Validation - Surface Required
**Given** I am adding a room  
**When** I leave surface empty  
**Then** I see error "Surface is required"

### AC6: Add Multiple Bedrooms
**Given** a unit has 1 bedroom  
**When** I add another bedroom  
**Then** both bedrooms are saved  
**And** unit shows 2 bedrooms

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
