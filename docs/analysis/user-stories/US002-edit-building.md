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
**Given** I am logged in as ADMIN  
**And** a building "Résidence Soleil" exists  
**When** I view the building details page  
**And** I click the "Edit" button  
**Then** the building edit form is displayed  
**And** all fields are pre-filled with current building data  
**And** the form looks identical to the creation form

---

### AC2: Edit Building Information Successfully
**Given** I am on the building edit form  
**And** the current owner name is "Jean Dupont"  
**When** I change the owner name to "Marie Martin"  
**And** I click "Save"  
**Then** the building is updated in the system  
**And** I see a success message "Building updated successfully"  
**And** I am redirected to the building details page  
**And** the owner name displays as "Marie Martin"  
**And** other fields remain unchanged

---

### AC3: Edit Multiple Fields
**Given** I am on the building edit form  
**When** I change:
- Street Address: from "123 Rue de la Loi" to "456 Avenue Louise"
- Postal Code: from "1000" to "1050"  

**And** I click "Save"  
**Then** both fields are updated  
**And** the building details reflect both changes

---

### AC4: Validation - Required Field Cleared
**Given** I am on the building edit form  
**When** I clear the "City" field (remove all text)  
**And** I click "Save"  
**Then** the form is NOT submitted  
**And** I see an error message "City is required"  
**And** the form remains displayed with my current data

---

### AC5: Validation - Field Length Exceeded
**Given** I am on the building edit form  
**When** I enter a postal code with 25 characters (limit is 20)  
**And** I click "Save"  
**Then** the form is NOT submitted  
**And** I see an error message "Postal code must be 20 characters or less"

---

### AC6: Cancel Editing
**Given** I am on the building edit form  
**And** I have modified the owner name  
**When** I click "Cancel"  
**Then** a confirmation dialog appears "You have unsaved changes. Are you sure you want to cancel?"  
**When** I click "Yes"  
**Then** I am redirected to the building details page  
**And** the owner name shows the original value (no changes saved)

---

### AC7: Add Owner to Building Without Owner
**Given** a building exists with no owner (ownerName = NULL)  
**When** I edit the building and enter owner name "Pierre Durand"  
**And** I save  
**Then** the owner is added  
**And** the building details display "Owner: Pierre Durand"

---

### AC8: Remove Owner from Building
**Given** a building exists with owner "Pierre Durand"  
**When** I edit the building and clear the owner name field  
**And** I save  
**Then** the owner is removed (ownerName = NULL)  
**And** the building details show "Owner: Not specified"

---

### AC9: Audit Trail Updated
**Given** I edit a building successfully  
**When** I view the building details  
**Then** the "Updated at" timestamp reflects the current date/time  
**And** the "Created by" and "Created at" remain unchanged

---

## Technical Notes

### API Endpoint
```
PUT /api/v1/buildings/{id}
Content-Type: application/json

{
  "name": "Résidence Soleil",
  "streetAddress": "456 Avenue Louise",
  "postalCode": "1050",
  "city": "Brussels",
  "country": "Belgium",
  "ownerName": "Marie Martin"
}
```

### Database
- Table: BUILDING
- Update: all editable fields, updated_at
- Do NOT update: id, created_by, created_at

### Validation Rules
- Same as creation (US001)
- All required fields must remain required
- Field lengths enforced

---

## UI Mockup Notes

### Edit Form
- Identical to creation form
- All fields pre-filled with current values
- Page title: "Edit Building: [Building Name]"
- "Save" button (primary)
- "Cancel" button (secondary)

### Behavior
- Real-time validation on blur
- Highlight changed fields (optional)
- Show unsaved changes indicator

---

## Dependencies

- US001 (Create Building) must be completed
- Building must exist in database
- User must have ADMIN role

---

## Testing Checklist

- [ ] Can navigate to edit form from building details
- [ ] Form pre-fills with current data
- [ ] Can edit single field
- [ ] Can edit multiple fields
- [ ] Cannot save with missing required fields
- [ ] Cannot save with invalid field lengths
- [ ] Can cancel with confirmation if data changed
- [ ] Can add owner to building without owner
- [ ] Can remove owner from building
- [ ] Updated_at timestamp is updated
- [ ] Created_by and created_at remain unchanged
- [ ] Success message shown
- [ ] Redirected to details page after save

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] UI matches mockup
- [ ] All acceptance criteria met
- [ ] Manual testing completed
- [ ] Documentation updated
- [ ] Merged to main branch

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
