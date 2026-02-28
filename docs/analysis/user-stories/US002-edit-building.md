# User Story US002: Edit Building

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US002 |
| **Story Name** | Edit Building |
| **Epic** | Building Management |
| **Related UC** | UC001 - Manage Buildings |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN
**I want to** edit building information
**So that** I can keep property data up to date when addresses or ownership changes

---

## Acceptance Criteria

### AC1: Navigate to Edit Form
**Given** a building exists
**When** I view the building details page and click "Edit"
**Then** the edit form is displayed
**And** all fields are pre-filled with current building data

### AC2: Edit Building Information Successfully
**Given** I am on the building edit form
**When** I change a field and click "Save"
**Then** the building is updated
**And** I see success message "Building updated successfully"
**And** the details page shows the new values

### AC3: Edit Multiple Fields
**Given** I am on the building edit form
**When** I change Street Address and Postal Code and click "Save"
**Then** both fields are updated

### AC4: Validation - Required Field Cleared
**Given** I clear the "City" field and click "Save"
**Then** I see error "City is required"
**And** the form is NOT submitted

### AC5: Validation - Field Length Exceeded
**Given** I enter a postal code with 25 characters (limit is 20)
**Then** I see error "Postal code must be 20 characters or less"

### AC6: Cancel Editing
**Given** I am on the building edit form with changes made
**When** I click "Cancel"
**Then** a confirmation dialog appears
**And** if I confirm, changes are discarded

### AC7: Add Owner to Building Without Owner
**Given** a building has no owner
**When** I select a person as owner and save
**Then** the owner is linked and shown in building details

### AC8: Remove Owner from Building
**Given** a building has an owner
**When** I clear the owner field and save
**Then** ownerId = NULL and building details show "Owner: Not specified"

### AC9: Audit Trail Updated
**Given** I edit a building successfully
**Then** the "Updated at" timestamp reflects the current date/time
**And** "Created by" and "Created at" remain unchanged

---

## Technical Notes

- Endpoint: `PUT /api/v1/buildings/{id}`
- `ownerId` can be set, changed, or cleared (null)
- Returns HTTP 404 if building not found; HTTP 409 if `ownerId` not found

---

**Last Updated**: 2024-01-15
**Status**: Ready for Development
