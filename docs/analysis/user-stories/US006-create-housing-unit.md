# User Story US006: Create Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US006 |
| **Story Name** | Create Housing Unit |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 - Manage Housing Units |
| **Priority** | MUST HAVE |
| **Story Points** | 5 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** create a new housing unit in a building  
**So that** I can manage individual apartments and track their details

---

## Acceptance Criteria

### AC1: Display Creation Form
**Given** I am viewing a building's details  
**When** I click "Add Housing Unit"  
**Then** the housing unit creation form is displayed  
**And** the building is pre-selected  
**And** required fields are marked with asterisk

### AC2: Create Unit with Required Fields Only
**Given** I am on the unit creation form  
**When** I enter:
- Unit Number: "A101"
- Floor: 1  

**And** I click "Save"  
**Then** the unit is created  
**And** I see success message  
**And** I am redirected to unit details

### AC3: Create Unit with Terrace
**Given** I am on the unit creation form  
**When** I check "Has Terrace"  
**And** I enter:
- Terrace Surface: 12.5
- Terrace Orientation: S  

**And** I fill required fields  
**And** I click "Save"  
**Then** the unit is created with terrace information

### AC4: Validation - Terrace Surface Required
**Given** I am creating a unit  
**When** I check "Has Terrace"  
**But** I leave terrace surface empty  
**And** I click "Save"  
**Then** I see error "Terrace surface is required when Has Terrace is checked"

### AC5: Validation - Duplicate Unit Number
**Given** unit "A101" exists in the building  
**When** I try to create another unit "A101" in same building  
**Then** I see error "Unit number must be unique within this building"

### AC6: Owner Inheritance
**Given** the building has owner "Jean Dupont"  
**When** I create a unit without specifying owner  
**Then** the unit inherits owner "Jean Dupont"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
