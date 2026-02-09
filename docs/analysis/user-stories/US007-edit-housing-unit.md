# User Story US007: Edit Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US007 |
| **Story Name** | Edit Housing Unit |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 - Manage Housing Units |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** edit housing unit information  
**So that** I can update details when unit characteristics change

---

## Acceptance Criteria

### AC1: Navigate to Edit Form
**Given** a housing unit exists  
**When** I view the unit details  
**And** I click "Edit"  
**Then** the edit form is displayed  
**And** all fields are pre-filled with current values

### AC2: Edit Total Surface
**Given** a unit has total surface 85.50 m²  
**When** I change it to 90.00 m²  
**And** I save  
**Then** the unit is updated  
**And** total surface shows 90.00 m²

### AC3: Add Terrace to Existing Unit
**Given** a unit has no terrace (has_terrace = false)  
**When** I check "Has Terrace"  
**And** I enter surface 10.5 m² and orientation E  
**And** I save  
**Then** the unit is updated with terrace information

### AC4: Remove Terrace from Unit
**Given** a unit has a terrace  
**When** I uncheck "Has Terrace"  
**And** I save  
**Then** terrace information is removed  
**And** has_terrace = false

### AC5: Override Building Owner
**Given** building has owner "Jean Dupont"  
**And** unit inherits this owner  
**When** I edit unit and enter owner "Marie Martin"  
**And** I save  
**Then** unit owner is "Marie Martin" (overriding building owner)

### AC6: Validation - Unit Number Uniqueness
**Given** units "A101" and "A102" exist in building  
**When** I edit "A102" and change unit number to "A101"  
**Then** I see error "Unit number must be unique"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
