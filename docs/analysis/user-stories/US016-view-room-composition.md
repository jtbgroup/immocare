# User Story US016: View Room Composition

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US016 |
| **Story Name** | View Room Composition |
| **Epic** | Room Management |
| **Related UC** | UC003 - Manage Rooms |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** view the complete room composition of a housing unit  
**So that** I can see the layout and calculate total surface

---

## Acceptance Criteria

### AC1: Display Room List
**Given** a unit has 5 rooms  
**When** I view the unit details  
**Then** the Rooms section shows all 5 rooms  
**And** each room displays:
- Room type
- Approximate surface (m²)

### AC2: Display Total Calculated Surface
**Given** a unit has rooms: 20, 15, 10, 8, 5 m²  
**When** I view the Rooms section  
**Then** I see "Total: 58.00 m²" below the list

### AC3: Empty Room List
**Given** a unit has no rooms  
**When** I view unit details  
**Then** Rooms section shows "No rooms defined yet"  
**And** "Add Room" button is visible

### AC4: Group by Room Type (Optional)
**Given** a unit has 3 bedrooms, 1 kitchen, 2 bathrooms  
**When** viewing room list  
**Then** rooms can be grouped by type (optional feature)

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
