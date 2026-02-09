# Use Case UC001: Manage Buildings

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC001 |
| **Use Case Name** | Manage Buildings |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User must be authenticated with ADMIN role |
| **Postconditions** | Building data is created, updated, or deleted in the system |
| **Related Use Cases** | UC002 (Manage Housing Units) |

---

## Description

This use case describes how an administrator manages buildings in the ImmoCare system. Buildings are the top-level containers for housing units and represent physical properties with addresses and optional ownership information.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator with full access
- **Goal**: Create, view, edit, and delete buildings
- **Characteristics**: 
  - Familiar with property management
  - Has complete address information for buildings
  - May or may not know building ownership details

---

## Preconditions

1. User is logged in to the system
2. User has ADMIN role
3. System is operational and database is accessible

---

## Basic Flow

### 1. View All Buildings

**Trigger**: ADMIN navigates to the Buildings page

1. System displays a list of all buildings in the system
2. Each building shows:
   - Building name
   - Full address (street, postal code, city, country)
   - Owner name (if specified)
   - Number of housing units (count)
3. ADMIN can sort and filter the list:
   - Sort by name, city, or creation date
   - Filter by city or country
   - Search by name or address
4. System provides pagination if more than 20 buildings

**Result**: ADMIN can see all buildings and navigate to details

---

### 2. View Building Details

**Trigger**: ADMIN clicks on a specific building from the list

1. System displays detailed information:
   - Building ID
   - Building name
   - Street address
   - Postal code
   - City
   - Country
   - Owner name (if specified)
   - Created by (username)
   - Created at (timestamp)
   - Updated at (timestamp)
2. System displays list of housing units in this building:
   - Unit number
   - Floor
   - Current rent (if defined)
   - Occupancy status (future feature)
3. ADMIN can:
   - Edit building information
   - Delete building (if no housing units exist)
   - Navigate to housing unit details
   - Add new housing unit

**Result**: ADMIN views complete building information

---

### 3. Create New Building

**Trigger**: ADMIN clicks "Create Building" button

1. System displays building creation form with fields:
   - **Building Name** (required, text, max 100 chars)
   - **Street Address** (required, text, max 200 chars)
   - **Postal Code** (required, text, max 20 chars)
   - **City** (required, text, max 100 chars)
   - **Country** (required, text, max 100 chars, default: "Belgium")
   - **Owner Name** (optional, text, max 200 chars)

2. ADMIN fills in the form fields

3. ADMIN clicks "Save" button

4. System validates input:
   - All required fields are filled
   - Text lengths are within limits
   - No special validation on address format (keep flexible)

5. System saves the building to the database:
   - Generates new building ID
   - Sets created_by to current user
   - Sets created_at and updated_at timestamps

6. System displays success message: "Building created successfully"

7. System redirects to building details page

**Result**: New building is created and stored in the system

---

### 4. Edit Building Information

**Trigger**: ADMIN clicks "Edit" button on building details page

1. System displays building edit form pre-filled with current values:
   - All fields editable except ID and audit fields
   - Same fields as creation form

2. ADMIN modifies one or more fields

3. ADMIN clicks "Save" button

4. System validates input (same rules as creation)

5. System updates the building in the database:
   - Updates modified fields
   - Sets updated_at to current timestamp
   - Does NOT change created_by or created_at

6. System displays success message: "Building updated successfully"

7. System returns to building details page with updated information

**Result**: Building information is updated

---

### 5. Delete Building

**Trigger**: ADMIN clicks "Delete" button on building details page

1. System checks if building has associated housing units:
   - **IF housing units exist**: Go to Alternative Flow 5A
   - **IF no housing units**: Continue

2. System displays confirmation dialog:
   - "Are you sure you want to delete this building?"
   - Building name shown for confirmation
   - Warning: "This action cannot be undone"

3. ADMIN clicks "Confirm Delete"

4. System deletes the building from the database

5. System displays success message: "Building deleted successfully"

6. System redirects to buildings list page

**Result**: Building is permanently removed from the system

---

## Alternative Flows

### Alternative Flow 3A: Validation Error During Creation

**Trigger**: Validation fails in step 4 of Basic Flow 3

1. System displays error messages next to relevant fields:
   - "Building name is required"
   - "Street address is required"
   - "Postal code is required"
   - "City is required"
   - "Country is required"
   - "Building name must be 100 characters or less"
   - (etc.)

2. ADMIN corrects the errors

3. Return to step 3 of Basic Flow 3

**Result**: ADMIN must correct errors before saving

---

### Alternative Flow 4A: Validation Error During Update

**Trigger**: Validation fails in step 4 of Basic Flow 4

1. Same behavior as Alternative Flow 3A

2. Return to step 3 of Basic Flow 4

**Result**: ADMIN must correct errors before saving

---

### Alternative Flow 5A: Cannot Delete Building with Housing Units

**Trigger**: Building has housing units in step 1 of Basic Flow 5

1. System displays error dialog:
   - "Cannot delete building"
   - "This building contains {count} housing unit(s)"
   - "Delete all housing units first, or archive the building instead"

2. ADMIN clicks "OK"

3. System returns to building details page without deleting

**Result**: Building is not deleted; ADMIN must remove housing units first

---

### Alternative Flow 6A: Cancel Operation

**Trigger**: ADMIN clicks "Cancel" button during create or edit

1. System displays confirmation if changes were made:
   - "You have unsaved changes. Are you sure you want to cancel?"

2. ADMIN clicks "Yes" or "No":
   - **Yes**: System discards changes and returns to previous page
   - **No**: System returns to form with data intact

**Result**: Operation is cancelled without saving

---

## Exception Flows

### Exception Flow 1: Database Error

**Trigger**: Database is unavailable during any operation

1. System displays error message: "Database error. Please try again later."

2. System logs error details for administrator review

3. ADMIN can retry the operation

**Result**: Operation fails; data is not modified

---

### Exception Flow 2: Concurrent Update Conflict

**Trigger**: Two users edit the same building simultaneously

1. First user saves successfully

2. Second user attempts to save

3. System detects that `updated_at` has changed since the record was loaded

4. System displays error: "This building was modified by another user. Please refresh and try again."

5. ADMIN must reload the page to see current data

**Result**: Second update is rejected to prevent data loss

---

### Exception Flow 3: Session Timeout

**Trigger**: User session expires during operation

1. System detects expired session

2. System displays message: "Your session has expired. Please log in again."

3. System redirects to login page

4. After login, ADMIN must restart the operation

**Result**: Operation is not completed; ADMIN must re-authenticate

---

## Business Rules

### BR-UC001-01: Required Fields
All buildings must have:
- Name
- Street address
- Postal code
- City
- Country

**Rationale**: Minimum information needed to uniquely identify a physical building

---

### BR-UC001-02: Owner Inheritance
If a building has an owner name, all housing units in that building inherit this owner unless explicitly overridden at the unit level.

**Rationale**: Simplifies data entry when all units have the same owner

---

### BR-UC001-03: Cascade Delete Protection
A building cannot be deleted if it contains housing units.

**Rationale**: Prevents accidental data loss; housing units should be explicitly deleted or archived first

---

### BR-UC001-04: Duplicate Building Names
Multiple buildings can have the same name (no uniqueness constraint).

**Rationale**: Different buildings in different cities may have the same name (e.g., "Central Apartments")

---

### BR-UC001-05: Address Flexibility
Address format is not strictly validated (no postal code format validation).

**Rationale**: Support international properties with varying address formats

---

## Data Elements

### Input Data
- Building name (string, max 100 chars)
- Street address (string, max 200 chars)
- Postal code (string, max 20 chars)
- City (string, max 100 chars)
- Country (string, max 100 chars)
- Owner name (string, max 200 chars, optional)

### Output Data
- Building ID (generated)
- All input data
- Created by (user ID and username)
- Created at (timestamp)
- Updated at (timestamp)
- Housing unit count

---

## User Interface Requirements

### Buildings List Page
- Table or card view of buildings
- Columns: Name, Address, City, Country, Owner, Units Count
- Sorting by any column
- Filtering by city, country
- Search box (searches name and address)
- Pagination (20 items per page)
- "Create Building" button

### Building Details Page
- Building information in read-only format
- "Edit" button (top right)
- "Delete" button (top right, with warning icon)
- List of housing units (or "No units yet" message)
- "Add Housing Unit" button

### Building Form (Create/Edit)
- Form layout with labeled fields
- Required field indicators (asterisk *)
- Field validation feedback (real-time)
- "Save" and "Cancel" buttons
- Success/error messages

---

## Performance Requirements

- Buildings list must load in < 1 second (for up to 1000 buildings)
- Building details page must load in < 500ms
- Create/Edit operations must complete in < 1 second
- Database queries must use indexes on:
  - building.id (primary key)
  - building.city (for filtering)
  - building.created_by (for audit)

---

## Security Requirements

### Authorization
- Only users with ADMIN role can access this use case
- All operations require authentication
- Session must be active

### Data Protection
- Building data is visible only to authenticated users
- Owner name is considered sensitive information (future: may need access control)

### Audit Trail
- Record who created each building (created_by)
- Record when each building was created (created_at)
- Record when each building was last modified (updated_at)

---

## Test Scenarios

### Test Scenario 1: Create Building Successfully
**Given**: ADMIN is logged in  
**When**: ADMIN creates a building with all required fields  
**Then**: Building is saved and ADMIN sees success message and building details

### Test Scenario 2: Create Building with Missing Required Field
**Given**: ADMIN is on building creation form  
**When**: ADMIN leaves "City" field empty and clicks Save  
**Then**: Error message "City is required" is displayed

### Test Scenario 3: Edit Building Successfully
**Given**: Building exists in the system  
**When**: ADMIN edits the owner name and saves  
**Then**: Building is updated with new owner name

### Test Scenario 4: Delete Building with No Units
**Given**: Building exists with no housing units  
**When**: ADMIN deletes the building  
**Then**: Building is removed and ADMIN sees buildings list

### Test Scenario 5: Attempt to Delete Building with Units
**Given**: Building exists with 3 housing units  
**When**: ADMIN attempts to delete the building  
**Then**: Error message is displayed and building is NOT deleted

### Test Scenario 6: Search Buildings by Name
**Given**: Multiple buildings exist  
**When**: ADMIN searches for "Central"  
**Then**: Only buildings with "Central" in their name or address are displayed

### Test Scenario 7: View Building Details
**Given**: Building exists with 2 housing units  
**When**: ADMIN views building details  
**Then**: Building info and list of 2 units are displayed

---

## Open Questions

1. **Q**: Should we validate postal code formats for specific countries?  
   **A**: No, keep flexible for international support (can add in Phase 2)

2. **Q**: Should we prevent duplicate buildings (same name and address)?  
   **A**: No, allow duplicates for now (edge case: same building entered twice by mistake)

3. **Q**: Should we support bulk import of buildings from CSV?  
   **A**: Not in Phase 1, add to backlog (BACKLOG-034)

4. **Q**: Should we support building photos or documents?  
   **A**: Not in Phase 1, add to backlog (BACKLOG-017)

---

## Related User Stories

- **US001**: As an ADMIN, I want to create a new building so that I can add housing units to it
- **US002**: As an ADMIN, I want to edit building information so that I can keep data up to date
- **US003**: As an ADMIN, I want to delete a building so that I can remove properties no longer managed
- **US004**: As an ADMIN, I want to view all buildings so that I can see my property portfolio
- **US005**: As an ADMIN, I want to search buildings so that I can quickly find a specific property

---

## Notes

- Buildings are the top-level entity in the system hierarchy
- Owner field at building level is optional but recommended
- Housing units will inherit building owner unless overridden
- Future: Consider adding building photo, documents, notes
- Future: Consider "archive" instead of "delete" for historical tracking

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: ✅ Ready for Implementation  
**Next Review**: After Phase 1 completion
