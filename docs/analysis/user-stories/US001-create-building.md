# User Story US001: Create Building

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US001 |
| **Story Name** | Create Building |
| **Epic** | Building Management |
| **Related UC** | UC001 - Manage Buildings |
| **Priority** | MUST HAVE |
| **Story Points** | 5 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN
**I want to** create a new building
**So that** I can add housing units to it and manage the property

---

## Acceptance Criteria

### AC1: Display Building Creation Form
**Given** I am logged in as ADMIN
**When** I navigate to the Buildings page and click "Create Building"
**Then** the building creation form is displayed
**And** all required fields are marked with an asterisk (*)
**And** the Country field has "Belgium" as default value

### AC2: Create Building with All Required Fields
**Given** I am on the building creation form
**When** I enter: Name "Résidence Soleil", Address "123 Rue de la Loi", Postal Code "1000", City "Brussels", Country "Belgium"
**And** I click "Save"
**Then** the building is created
**And** I see success message "Building created successfully"
**And** I am redirected to the building details page

### AC3: Create Building with Optional Owner
**Given** I am on the building creation form
**When** I fill all required fields and select a person as owner via the picker
**And** I click "Save"
**Then** the building is created with the owner linked
**And** the building details display the owner's full name

### AC4: Validation - Missing Required Field
**Given** I am on the building creation form
**When** I leave "City" empty and click "Save"
**Then** the form is NOT submitted
**And** I see error "City is required"

### AC5: Validation - Field Length Exceeded
**Given** I enter a Building Name with 150 characters (limit is 100)
**When** I click "Save"
**Then** I see error "Building name must be 100 characters or less"

### AC6: Cancel Building Creation
**Given** I am on the building creation form with data entered
**When** I click "Cancel"
**Then** a confirmation dialog appears: "You have unsaved changes. Are you sure you want to cancel?"
**And** if I confirm, I am returned to the buildings list without creating a building

---

## Technical Notes

- Endpoint: `POST /api/v1/buildings`
- `ownerId` optional — links to existing `Person`
- `country` defaults to "Belgium" if not provided
- Returns HTTP 201 on success; HTTP 409 if `ownerId` does not exist

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] All acceptance criteria met

**Last Updated**: 2024-01-15
**Status**: Ready for Development
