# User Story US011: Add Garden to Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US011 |
| **Story Name** | Add Garden to Housing Unit |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 - Manage Housing Units |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** add garden information to a housing unit  
**So that** I can track private outdoor garden spaces

---

## Acceptance Criteria

### AC1: Add Garden via Edit Form
**Given** a unit has no garden  
**When** I edit the unit  
**And** I check "Has Garden"  
**And** I enter surface 25.0 m² and orientation W  
**And** I save  
**Then** garden is added to unit  
**And** unit details show garden information

### AC2: Garden Orientation Dropdown
**Given** I am adding a garden  
**When** I click the orientation field  
**Then** I see dropdown with options: N, S, E, W, NE, NW, SE, SW

### AC3: Unit with Both Terrace and Garden
**Given** a unit has a terrace  
**When** I add a garden  
**Then** both terrace and garden information are stored  
**And** unit details show both

### AC4: Validation - Surface Required
**Given** I check "Has Garden"  
**When** I leave surface empty  
**And** I try to save  
**Then** I see error "Garden surface is required"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
