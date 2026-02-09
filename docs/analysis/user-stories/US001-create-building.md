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
**When** I navigate to the Buildings page  
**And** I click the "Create Building" button  
**Then** the building creation form is displayed  
**And** all required fields are marked with an asterisk (*)  
**And** the Country field has "Belgium" as default value

---

### AC2: Create Building with All Required Fields
**Given** I am on the building creation form  
**When** I enter the following data:
- Building Name: "Résidence Soleil"
- Street Address: "123 Rue de la Loi"
- Postal Code: "1000"
- City: "Brussels"
- Country: "Belgium"  

**And** I click "Save"  
**Then** the building is created in the system  
**And** I see a success message "Building created successfully"  
**And** I am redirected to the building details page  
**And** the building details show all the information I entered

---

### AC3: Create Building with Optional Owner
**Given** I am on the building creation form  
**When** I fill all required fields  
**And** I add Owner Name: "Jean Dupont"  
**And** I click "Save"  
**Then** the building is created with the owner name  
**And** the building details display "Owner: Jean Dupont"

---

### AC4: Validation - Missing Required Field
**Given** I am on the building creation form  
**When** I fill some fields but leave "City" empty  
**And** I click "Save"  
**Then** the form is NOT submitted  
**And** I see an error message "City is required" next to the City field  
**And** the form remains displayed with my entered data

---

### AC5: Validation - Field Length Exceeded
**Given** I am on the building creation form  
**When** I enter a Building Name with 150 characters (limit is 100)  
**And** I click "Save"  
**Then** the form is NOT submitted  
**And** I see an error message "Building name must be 100 characters or less"  
**And** the form remains displayed

---

### AC6: Cancel Building Creation
**Given** I am on the building creation form  
**And** I have entered some data  
**When** I click "Cancel"  
**Then** a confirmation dialog appears asking "You have unsaved changes. Are you sure you want to cancel?"  
**When** I click "Yes"  
**Then** I am redirected to the Buildings list page  
**And** no building is created

---

### AC7: Audit Trail Captured
**Given** I create a building successfully  
**When** I view the building details  
**Then** the following audit information is displayed:
- Created by: [my username]
- Created at: [timestamp]

---

## Technical Notes

### API Endpoint
```
POST /api/v1/buildings
Content-Type: application/json

{
  "name": "Résidence Soleil",
  "streetAddress": "123 Rue de la Loi",
  "postalCode": "1000",
  "city": "Brussels",
  "country": "Belgium",
  "ownerName": "Jean Dupont"  // optional
}
```

### Database
- Table: BUILDING
- Auto-generate: id, created_at, updated_at
- Set: created_by (from authenticated user)

### Validation Rules
- name: required, max 100 chars
- streetAddress: required, max 200 chars
- postalCode: required, max 20 chars
- city: required, max 100 chars
- country: required, max 100 chars
- ownerName: optional, max 200 chars

---

## UI Mockup Notes

### Form Layout
- Vertical form layout
- Labels on top of fields
- Required fields marked with red asterisk
- Help text below complex fields
- "Save" button (primary, blue)
- "Cancel" button (secondary, gray)

### Field Types
- All fields: text input
- Country: text input (future: dropdown with common countries)

---

## Dependencies

- User authentication system must be functional
- Database schema for BUILDING table created
- User must have ADMIN role

---

## Testing Checklist

- [ ] Can display creation form
- [ ] Can create building with all required fields
- [ ] Can create building with optional owner
- [ ] Cannot submit with missing required fields
- [ ] Cannot submit with fields exceeding max length
- [ ] Can cancel creation with confirmation
- [ ] Audit trail (created_by, created_at) is captured
- [ ] Success message displayed after creation
- [ ] Redirected to building details after creation
- [ ] Data persists in database
- [ ] Owner name can be left empty (NULL)

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] UI matches mockup/design
- [ ] All acceptance criteria met
- [ ] Manual testing completed
- [ ] Documentation updated
- [ ] Merged to main branch

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
