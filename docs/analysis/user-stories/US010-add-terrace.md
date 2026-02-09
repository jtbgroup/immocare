# User Story US010: Add Terrace to Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US010 |
| **Story Name** | Add Terrace to Housing Unit |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 - Manage Housing Units |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** add terrace information to a housing unit  
**So that** I can track outdoor spaces associated with apartments

---

## Acceptance Criteria

### AC1: Add Terrace via Edit Form
**Given** a unit has no terrace  
**When** I edit the unit  
**And** I check "Has Terrace"  
**And** I enter surface 15.5 m² and orientation S  
**And** I save  
**Then** terrace is added to unit  
**And** unit details show terrace information

### AC2: Terrace Orientation Dropdown
**Given** I am adding a terrace  
**When** I click the orientation field  
**Then** I see dropdown with options: N, S, E, W, NE, NW, SE, SW

### AC3: Validation - Surface Required
**Given** I check "Has Terrace"  
**When** I leave surface empty  
**And** I try to save  
**Then** I see error "Terrace surface is required"

### AC4: Validation - Orientation Required
**Given** I check "Has Terrace"  
**When** I enter surface but no orientation  
**And** I try to save  
**Then** I see error "Terrace orientation is required"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
