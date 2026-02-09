# User Story US003: Delete Building

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US003 |
| **Story Name** | Delete Building |
| **Epic** | Building Management |
| **Related UC** | UC001 - Manage Buildings |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN  
**I want to** delete a building  
**So that** I can remove properties that are no longer managed

---

## Acceptance Criteria

### AC1: Delete Empty Building Successfully
**Given** I am logged in as ADMIN  
**And** a building "Old Property" exists with no housing units  
**When** I view the building details page  
**And** I click the "Delete" button  
**Then** a confirmation dialog appears with:
- Title: "Delete Building?"
- Message: "Are you sure you want to delete 'Old Property'?"
- Warning: "This action cannot be undone"
- Buttons: "Cancel" and "Delete"  

**When** I click "Delete"  
**Then** the building is permanently removed from the system  
**And** I see a success message "Building deleted successfully"  
**And** I am redirected to the Buildings list page  
**And** "Old Property" is no longer in the list

---

### AC2: Cannot Delete Building with Housing Units
**Given** I am logged in as ADMIN  
**And** a building "Active Property" exists with 3 housing units  
**When** I view the building details page  
**And** I click the "Delete" button  
**Then** an error dialog appears with:
- Title: "Cannot Delete Building"
- Message: "This building contains 3 housing unit(s)"
- Instructions: "Delete all housing units first, or archive the building instead"
- Button: "OK"  

**When** I click "OK"  
**Then** the dialog closes  
**And** I remain on the building details page  
**And** the building is NOT deleted

---

### AC3: Cancel Deletion
**Given** I am on a building details page  
**When** I click the "Delete" button  
**And** the confirmation dialog appears  
**And** I click "Cancel"  
**Then** the dialog closes  
**And** I remain on the building details page  
**And** the building is NOT deleted

---

### AC4: Verify Deletion from List
**Given** I have successfully deleted building "Old Property"  
**When** I navigate to the Buildings list page  
**Then** "Old Property" does not appear in the list  
**And** the total building count is reduced by 1

---

### AC5: Verify Deletion from Database
**Given** I have successfully deleted building "Old Property"  
**When** the system queries the database  
**Then** the building record no longer exists  
**And** the building ID cannot be retrieved

---

### AC6: Cannot Access Deleted Building
**Given** I have deleted building with ID 123  
**When** I try to navigate directly to `/buildings/123`  
**Then** I see a "404 Not Found" error  
**Or** I see a message "Building not found"

---

## Technical Notes

### API Endpoint
```
DELETE /api/v1/buildings/{id}

Response 200 OK:
{
  "message": "Building deleted successfully"
}

Response 400 Bad Request (has units):
{
  "error": "Cannot delete building",
  "message": "Building has 3 housing unit(s)",
  "unitCount": 3
}

Response 404 Not Found:
{
  "error": "Building not found"
}
```

### Database
- Table: BUILDING
- Action: DELETE FROM building WHERE id = ?
- Check: Count housing units first
- If count > 0, reject deletion

### Business Rules
- Building can only be deleted if it has zero housing units
- Deletion is permanent (no soft delete in Phase 1)
- No cascade delete of units (must be deleted first)

---

## UI Mockup Notes

### Delete Button
- Location: Top right of building details page
- Style: Red button with trash icon
- Label: "Delete"

### Confirmation Dialog
- Modal overlay
- Building name shown in bold
- Warning icon (red triangle)
- "This action cannot be undone" in red text
- "Cancel" button (gray, left)
- "Delete" button (red, right)

### Error Dialog (Has Units)
- Modal overlay
- Error icon (red X)
- Unit count displayed clearly
- Helpful instructions
- Single "OK" button

---

## Dependencies

- US001 (Create Building) must be completed
- Building must exist in database
- User must have ADMIN role
- US006 (Create Housing Unit) affects this story (for testing prevention)

---

## Testing Checklist

- [ ] Can delete building with no housing units
- [ ] Cannot delete building with housing units
- [ ] Confirmation dialog appears before deletion
- [ ] Can cancel deletion
- [ ] Success message shown after deletion
- [ ] Redirected to list page after deletion
- [ ] Building removed from database
- [ ] Building removed from UI list
- [ ] Cannot access deleted building by URL
- [ ] Error message clear when deletion prevented
- [ ] Unit count displayed in error message

---

## Edge Cases

### Edge Case 1: Concurrent Deletion
**Given** Two admins view the same building  
**When** Admin A deletes the building  
**And** Admin B tries to delete it seconds later  
**Then** Admin B sees "Building not found" error

### Edge Case 2: Unit Added During Deletion Flow
**Given** Admin viewing building with 0 units  
**When** Admin clicks Delete  
**And** Another admin adds a unit before confirmation  
**And** First admin clicks "Delete" in confirmation  
**Then** Deletion is rejected with "Building has 1 unit(s)" error

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] UI matches mockup
- [ ] All acceptance criteria met
- [ ] All edge cases handled
- [ ] Manual testing completed
- [ ] Documentation updated
- [ ] Merged to main branch

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
