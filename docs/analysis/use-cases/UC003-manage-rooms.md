# Use Case UC003: Manage Rooms

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC003 |
| **Use Case Name** | Manage Rooms |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; Housing unit exists |
| **Postconditions** | Room data is created, updated, or deleted in the system |
| **Related Use Cases** | UC002 (Manage Housing Units) |

---

## Description

This use case describes how an administrator manages individual rooms within housing units. Rooms define the composition of a unit with types and approximate surfaces.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator
- **Goal**: Define room composition of housing units
- **Characteristics**: 
  - Knows unit layout
  - Has approximate room measurements

---

## Preconditions

1. User authenticated as ADMIN
2. At least one housing unit exists
3. System operational

---

## Basic Flow

### 1. View All Rooms in Housing Unit

**Trigger**: ADMIN views housing unit details

1. System displays list of rooms in the Rooms section
2. Each room shows:
   - Room type
   - Approximate surface (m²)
3. System shows calculated total: "Total: XX.XX m²"
4. System shows "No rooms defined yet" if empty
5. ADMIN can add, edit, or delete rooms

**Result**: ADMIN sees room composition

---

### 2. Create New Room

**Trigger**: ADMIN clicks "Add Room" button on unit details

1. System displays room creation form:
   - **Housing Unit** (pre-selected, read-only)
   - **Room Type** (required, dropdown)
   - **Approximate Surface** (required, decimal, m²)

2. Room Type options:
   - Living Room
   - Bedroom
   - Kitchen
   - Bathroom
   - Toilet
   - Hallway
   - Storage
   - Office
   - Dining Room
   - Other

3. ADMIN selects type and enters surface

4. ADMIN clicks "Save"

5. System validates:
   - Room type selected
   - Surface is positive number (> 0)
   - Surface is reasonable (< 1000 m²)

6. System saves room:
   - Generates room ID
   - Links to housing unit
   - Sets timestamps

7. System displays success message

8. System refreshes room list with new room

9. System updates total surface calculation

**Result**: New room is added to unit

---

### 3. Edit Room

**Trigger**: ADMIN clicks "Edit" icon next to room

1. System displays inline edit form or modal:
   - Room type (dropdown, current value selected)
   - Surface (input field, current value)

2. ADMIN modifies values

3. ADMIN clicks "Save"

4. System validates (same rules as creation)

5. System updates room

6. System displays success message

7. System refreshes list and updates total

**Result**: Room information is updated

---

### 4. Delete Room

**Trigger**: ADMIN clicks "Delete" icon next to room

1. System displays confirmation:
   - "Delete this room?"
   - Room type and surface shown
   - "This action cannot be undone"

2. ADMIN clicks "Confirm"

3. System deletes room

4. System displays success message

5. System refreshes list and updates total

**Result**: Room is removed

---

### 5. Quick Add Multiple Rooms

**Trigger**: ADMIN clicks "Quick Add Rooms" button

1. System displays multi-room form:
   - Table with rows for multiple rooms
   - Each row: Type dropdown + Surface input
   - "Add Row" button
   - "Remove Row" button per row
   - "Save All" button

2. ADMIN adds multiple rooms in one form

3. ADMIN clicks "Save All"

4. System validates all rows

5. System saves all valid rooms in batch

6. System shows summary: "X rooms added successfully"

**Result**: Multiple rooms added efficiently

---

## Alternative Flows

### Alternative Flow 2A: Validation Error

**Trigger**: Validation fails during room creation

1. System displays errors:
   - "Room type is required"
   - "Surface is required"
   - "Surface must be positive"
   - "Surface must be less than 1000 m²"

2. ADMIN corrects errors

3. Return to Basic Flow step 3

**Result**: Errors must be corrected

---

### Alternative Flow 2B: Duplicate Room Type Warning

**Trigger**: ADMIN adds second bedroom (or other type)

1. System shows info message (not error):
   - "This unit already has 1 bedroom(s)"
   - "Continue anyway?"

2. ADMIN clicks "Yes" or "No"
   - Yes: Continue saving
   - No: Return to form

**Result**: Warning given but not blocked (multiple bedrooms are valid)

---

### Alternative Flow 5A: Batch Validation Error

**Trigger**: One or more rows invalid in Quick Add

1. System highlights invalid rows

2. System displays errors per row

3. ADMIN can:
   - Fix errors and retry
   - Remove invalid rows
   - Cancel operation

**Result**: Only valid rooms are saved

---

## Exception Flows

### Exception Flow 1: Database Error

**Trigger**: Database unavailable

1. System displays error message

2. System logs error

3. ADMIN can retry

**Result**: Operation fails

---

## Business Rules

### BR-UC003-01: Required Fields
All rooms must have:
- Room type
- Approximate surface (> 0)

**Rationale**: Minimum information for meaningful room data

---

### BR-UC003-02: Surface Range
Surface must be > 0 and < 1000 m² (sanity check).

**Rationale**: Catch obvious data entry errors

---

### BR-UC003-03: Multiple Rooms of Same Type Allowed
A unit can have multiple rooms of the same type (e.g., 3 bedrooms).

**Rationale**: Realistic housing units have multiple similar rooms

---

### BR-UC003-04: Surface is Approximate
Room surface is not legally binding, just for reference.

**Rationale**: Not official measurements, just estimates

---

### BR-UC003-05: Room Deletion
Rooms can be freely deleted (no cascade dependencies).

**Rationale**: Rooms are leaf entities with no children

---

### BR-UC003-06: Total Surface Calculation
Sum of room surfaces can inform housing unit total surface but doesn't override manual entry.

**Rationale**: Manual total surface may include walls, common areas not in room list

---

## Data Elements

### Input Data
- Housing unit ID (selected)
- Room type (enum)
- Approximate surface (decimal, m²)

### Output Data
- Room ID (generated)
- All input data
- Created_at timestamp
- Updated_at timestamp

---

## User Interface Requirements

### Rooms Section (in Housing Unit Details)
- Table with columns: Type, Surface
- "Add Room" button
- Edit/Delete icons per row
- Total surface displayed below table
- "Quick Add Rooms" button (optional)

### Room Form (Create/Edit)
- Room type dropdown with all options
- Surface input with "m²" suffix
- Validation feedback
- "Save" and "Cancel" buttons

### Quick Add Form (optional)
- Multi-row table
- Add/Remove row buttons
- "Save All" button
- Batch validation

---

## Performance Requirements

- Room list loads with unit details (< 500ms)
- Add room completes in < 500ms
- Batch add completes in < 1 second

---

## Security Requirements

- Only ADMIN can manage rooms
- Rooms belong to specific housing unit (data isolation)

---

## Test Scenarios

### TS-UC003-01: Create Room Successfully
**Given**: Housing unit exists  
**When**: ADMIN adds bedroom with 15.5 m²  
**Then**: Room saved, list updated

### TS-UC003-02: Missing Surface Error
**Given**: ADMIN creating room  
**When**: Leaves surface empty  
**Then**: Error "Surface is required"

### TS-UC003-03: Invalid Surface Error
**Given**: ADMIN creating room  
**When**: Enters surface = -5  
**Then**: Error "Surface must be positive"

### TS-UC003-04: Edit Room Type
**Given**: Room exists as "Bedroom"  
**When**: ADMIN changes to "Office"  
**Then**: Room updated successfully

### TS-UC003-05: Delete Room
**Given**: Room exists  
**When**: ADMIN deletes it  
**Then**: Room removed, total recalculated

### TS-UC003-06: Multiple Bedrooms
**Given**: Unit has 1 bedroom  
**When**: ADMIN adds 2nd bedroom  
**Then**: Both bedrooms exist (warning shown but allowed)

### TS-UC003-07: Total Calculation
**Given**: Unit has 3 rooms: 20, 15, 10 m²  
**When**: ADMIN views unit details  
**Then**: Total shows 45.00 m²

### TS-UC003-08: Quick Add 5 Rooms
**Given**: ADMIN uses Quick Add  
**When**: Adds 5 rooms in batch  
**Then**: All 5 saved successfully

---

## Related User Stories

- **US012**: Add room to housing unit
- **US013**: Edit room information
- **US014**: Delete room
- **US015**: Quick add multiple rooms
- **US016**: View room composition

---

## Notes

- Room types are predefined (not free text)
- Surface is approximate, not official cadastral measurement
- No limit on number of rooms per unit
- Consider adding room description field in future
- Future: Room photos, dimensions (length x width)

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: ✅ Ready for Implementation
