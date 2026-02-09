# Use Case UC002: Manage Housing Units

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC002 |
| **Use Case Name** | Manage Housing Units |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; At least one building exists |
| **Postconditions** | Housing unit data is created, updated, or deleted in the system |
| **Related Use Cases** | UC001 (Manage Buildings), UC003 (Manage Rooms) |

---

## Description

This use case describes how an administrator manages housing units (apartments) within buildings. Housing units represent individual rentable properties with specific characteristics including floor, rooms, surfaces, and optional outdoor spaces.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator with full access
- **Goal**: Create, view, edit, and delete housing units within buildings
- **Characteristics**: 
  - Knows building structure and unit details
  - Has information about unit configuration
  - May manage multiple buildings simultaneously

---

## Preconditions

1. User is logged in with ADMIN role
2. At least one building exists in the system
3. System is operational and database is accessible

---

## Basic Flow

### 1. View All Housing Units in a Building

**Trigger**: ADMIN navigates to building details page

1. System displays list of housing units for the selected building
2. Each unit shows:
   - Unit number
   - Floor and landing (if specified)
   - Total surface (if specified)
   - Number of rooms
   - Current rent (if defined)
   - Owner name (inherited or specific)
3. ADMIN can sort and filter:
   - Sort by unit number, floor, or surface
   - Filter by floor
   - Search by unit number
4. System shows "No units yet" if building is empty

**Result**: ADMIN sees all units in the building

---

### 2. View Housing Unit Details

**Trigger**: ADMIN clicks on a specific housing unit

1. System displays detailed information:
   - Unit ID
   - Building name (link to building)
   - Unit number
   - Floor
   - Landing number (if specified)
   - Total surface (m²)
   - Terrace information (has terrace, surface, orientation)
   - Garden information (has garden, surface, orientation)
   - Owner name (inherited or specific)
   - Created by and timestamps
2. System displays list of rooms
3. System displays current PEB score (if exists)
4. System displays current rent (if exists)
5. System displays current water meter (if exists)
6. ADMIN can:
   - Edit unit information
   - Delete unit (if no associated data)
   - Add/manage rooms
   - Manage PEB scores, rents, water meters

**Result**: ADMIN views complete unit information

---

### 3. Create New Housing Unit

**Trigger**: ADMIN clicks "Add Housing Unit" button on building details page

1. System displays housing unit creation form:
   - **Building** (pre-selected, read-only)
   - **Unit Number** (required, text, max 20 chars)
   - **Floor** (required, integer, -10 to 100)
   - **Landing Number** (optional, text, max 10 chars)
   - **Total Surface** (optional, decimal, m²)
   - **Has Terrace** (checkbox, default: false)
     - If checked: **Terrace Surface** (required, decimal, m²)
     - If checked: **Terrace Orientation** (dropdown: N, S, E, W, NE, NW, SE, SW)
   - **Has Garden** (checkbox, default: false)
     - If checked: **Garden Surface** (required, decimal, m²)
     - If checked: **Garden Orientation** (dropdown: N, S, E, W, NE, NW, SE, SW)
   - **Owner Name** (optional, text, max 200 chars)
     - Shows inherited value from building as placeholder

2. ADMIN fills in the form

3. ADMIN clicks "Save"

4. System validates input:
   - Required fields are filled
   - Unit number unique within building
   - Floor is valid integer
   - Surfaces are positive numbers
   - If has_terrace = true, terrace surface and orientation required
   - If has_garden = true, garden surface and orientation required

5. System saves the unit:
   - Generates unit ID
   - Links to building
   - Sets created_by to current user
   - If owner_name empty, inherits from building

6. System displays success message

7. System redirects to unit details page

**Result**: New housing unit is created

---

### 4. Edit Housing Unit Information

**Trigger**: ADMIN clicks "Edit" button on unit details page

1. System displays edit form with current values

2. ADMIN modifies fields

3. ADMIN clicks "Save"

4. System validates input (same rules as creation)

5. System updates the unit:
   - Updates modified fields
   - Sets updated_at timestamp
   - Validates unit number uniqueness if changed

6. System displays success message

7. System returns to unit details

**Result**: Unit information is updated

---

### 5. Delete Housing Unit

**Trigger**: ADMIN clicks "Delete" button on unit details page

1. System checks for associated data:
   - Rooms
   - PEB score history
   - Rent history
   - Water meter history
   - **IF any exist**: Go to Alternative Flow 5A

2. System displays confirmation dialog:
   - "Delete this housing unit?"
   - Unit number shown
   - Warning about permanence

3. ADMIN clicks "Confirm Delete"

4. System deletes the unit (cascade delete to all history)

5. System displays success message

6. System redirects to building details page

**Result**: Unit is permanently removed

---

## Alternative Flows

### Alternative Flow 3A: Validation Error During Creation

**Trigger**: Validation fails in step 4 of Basic Flow 3

1. System displays error messages:
   - "Unit number is required"
   - "Floor is required"
   - "Unit number must be unique within this building"
   - "Terrace surface required when Has Terrace is checked"
   - "Surface must be a positive number"
   - (etc.)

2. ADMIN corrects errors

3. Return to step 3 of Basic Flow 3

**Result**: ADMIN must fix errors before saving

---

### Alternative Flow 5A: Cannot Delete Unit with Associated Data

**Trigger**: Unit has rooms, PEB, rent, or meter data in step 1 of Basic Flow 5

1. System displays detailed error dialog:
   - "Cannot delete housing unit"
   - "This unit has associated data:"
     - "{X} room(s)"
     - "{X} PEB score(s)"
     - "{X} rent record(s)"
     - "{X} water meter(s)"
   - "Delete associated data first or archive this unit"

2. ADMIN clicks "OK"

3. System returns to unit details without deleting

**Result**: Unit is not deleted

---

### Alternative Flow 3B: Dynamic Terrace/Garden Fields

**Trigger**: ADMIN checks/unchecks "Has Terrace" or "Has Garden"

1. System dynamically shows/hides related fields:
   - Check "Has Terrace" → Show terrace surface and orientation
   - Uncheck "Has Terrace" → Hide and clear terrace fields
   - Same for garden

2. System validates only when checkbox is checked

**Result**: Form adapts to user selections

---

### Alternative Flow 6: Calculate Total Surface from Rooms

**Trigger**: ADMIN clicks "Calculate from Rooms" button (if implemented)

1. System sums all room surfaces for this unit

2. System populates total_surface field with calculated value

3. ADMIN can accept or modify the calculated value

**Result**: Total surface is calculated automatically

---

## Exception Flows

### Exception Flow 1: Database Error

**Trigger**: Database unavailable during operation

1. System displays error: "Database error. Please try again."

2. System logs error details

3. ADMIN can retry

**Result**: Operation fails without data modification

---

### Exception Flow 2: Concurrent Update Conflict

**Trigger**: Two users edit same unit simultaneously

1. First user saves successfully

2. Second user attempts to save

3. System detects updated_at changed

4. System displays: "Unit modified by another user. Please refresh."

5. ADMIN must reload

**Result**: Second update rejected

---

## Business Rules

### BR-UC002-01: Required Fields
All housing units must have:
- Building association
- Unit number
- Floor

**Rationale**: Minimum information to identify a specific unit

---

### BR-UC002-02: Unit Number Uniqueness
Unit number must be unique within the same building (not globally).

**Rationale**: Different buildings can have same unit numbers (e.g., "A101")

---

### BR-UC002-03: Owner Inheritance
If unit owner_name is NULL, inherit from building.owner_name.

**Rationale**: Avoid repetition when all units have same owner

---

### BR-UC002-04: Terrace/Garden Conditional Requirement
If has_terrace = true, terrace_surface and terrace_orientation are required.
If has_garden = true, garden_surface and garden_orientation are required.

**Rationale**: Ensure complete data when feature is present

---

### BR-UC002-05: Total Surface Flexibility
Total surface can be:
- Calculated from room surfaces (sum)
- Manually entered
- Left empty (NULL)

**Rationale**: Different data entry workflows; sometimes total known before rooms defined

---

### BR-UC002-06: Floor Range
Floor must be between -10 (basement levels) and 100 (high-rise buildings).

**Rationale**: Cover realistic building heights including underground parking

---

### BR-UC002-07: Cascade Delete Protection
A unit cannot be deleted if it has:
- Rooms
- PEB score history
- Rent history
- Water meter history

**Rationale**: Prevent accidental data loss

---

## Data Elements

### Input Data
- Building ID (selected)
- Unit number (string, max 20)
- Floor (integer, -10 to 100)
- Landing number (string, max 10, optional)
- Total surface (decimal, optional)
- Has terrace (boolean)
- Terrace surface (decimal, conditional)
- Terrace orientation (enum, conditional)
- Has garden (boolean)
- Garden surface (decimal, conditional)
- Garden orientation (enum, conditional)
- Owner name (string, max 200, optional)

### Output Data
- Unit ID (generated)
- All input data
- Inherited owner name (if not specified)
- Room count
- Created by and timestamps
- Updated at timestamp

---

## User Interface Requirements

### Housing Units List (in Building Details)
- Table or card view
- Columns: Unit Number, Floor, Landing, Surface, Rooms, Rent
- Sort and filter options
- "Add Housing Unit" button

### Housing Unit Details Page
- Unit information panel
- Building link
- Rooms section with list
- PEB, Rent, Meter sections (with "Manage" buttons)
- "Edit" and "Delete" buttons

### Housing Unit Form (Create/Edit)
- Building name (read-only on edit)
- All fields with appropriate input types
- Conditional fields (terrace/garden) show/hide dynamically
- Orientation dropdowns with cardinal directions
- Surface fields with "m²" suffix
- Owner name with inherited value shown as placeholder
- Validation feedback
- "Save" and "Cancel" buttons

---

## Performance Requirements

- Units list must load in < 500ms (for up to 100 units per building)
- Unit details must load in < 500ms
- Create/Edit must complete in < 1 second
- Indexes required on:
  - housing_unit.id (PK)
  - housing_unit.building_id (FK, for filtering)
  - (housing_unit.building_id, housing_unit.unit_number) (unique constraint)

---

## Security Requirements

### Authorization
- Only ADMIN role can access
- All operations require active session

### Data Protection
- Unit data visible only to authenticated users
- Owner information considered sensitive

### Audit Trail
- Record created_by and created_at
- Record updated_at on modifications

---

## Test Scenarios

### TS-UC002-01: Create Unit Successfully
**Given**: Building exists  
**When**: ADMIN creates unit with required fields  
**Then**: Unit saved, success message shown

### TS-UC002-02: Create Unit with Duplicate Number
**Given**: Unit "A101" exists in building  
**When**: ADMIN creates another "A101" in same building  
**Then**: Error "Unit number must be unique"

### TS-UC002-03: Create Unit with Terrace
**Given**: ADMIN creating unit  
**When**: Checks "Has Terrace", fills surface and orientation  
**Then**: Unit created with terrace info

### TS-UC002-04: Terrace Validation Error
**Given**: ADMIN creating unit  
**When**: Checks "Has Terrace" but leaves surface empty  
**Then**: Error "Terrace surface required"

### TS-UC002-05: Owner Inheritance
**Given**: Building has owner "John Doe"  
**When**: ADMIN creates unit without owner  
**Then**: Unit inherits "John Doe" from building

### TS-UC002-06: Edit Unit Floor
**Given**: Unit exists on floor 3  
**When**: ADMIN changes floor to 5  
**Then**: Unit updated to floor 5

### TS-UC002-07: Delete Empty Unit
**Given**: Unit exists with no rooms or history  
**When**: ADMIN deletes unit  
**Then**: Unit removed successfully

### TS-UC002-08: Cannot Delete Unit with Rooms
**Given**: Unit has 3 rooms  
**When**: ADMIN attempts delete  
**Then**: Error shown, unit not deleted

### TS-UC002-09: View Unit Details
**Given**: Unit exists with 2 rooms  
**When**: ADMIN views details  
**Then**: All info displayed including room count

### TS-UC002-10: Floor Boundary Validation
**Given**: ADMIN creating unit  
**When**: Enters floor = 150  
**Then**: Error "Floor must be between -10 and 100"

---

## Related User Stories

- **US006**: Create housing unit in a building
- **US007**: Edit housing unit information
- **US008**: Delete housing unit
- **US009**: View housing unit details
- **US010**: Add terrace to housing unit
- **US011**: Add garden to housing unit

---

## Notes

- Unit number format is flexible (A101, 1.A, 101, etc.)
- Landing is optional - some buildings don't have multiple staircases
- Total surface can be approximate or exact
- Terrace/garden orientations use cardinal directions
- Owner can be overridden at unit level for mixed-ownership buildings
- Future: Consider unit photos, floor plans

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: ✅ Ready for Implementation
