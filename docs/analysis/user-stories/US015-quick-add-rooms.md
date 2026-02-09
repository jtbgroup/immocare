# User Story US015: Quick Add Multiple Rooms

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US015 |
| **Story Name** | Quick Add Multiple Rooms |
| **Epic** | Room Management |
| **Related UC** | UC003 - Manage Rooms |
| **Priority** | COULD HAVE |
| **Story Points** | 5 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** add multiple rooms at once  
**So that** I can quickly define all rooms in a unit without repetitive forms

---

## Acceptance Criteria

### AC1: Display Quick Add Form
**Given** I am viewing a housing unit  
**When** I click "Quick Add Rooms"  
**Then** a multi-row form is displayed  
**And** I see 3 empty rows by default  
**And** each row has: type dropdown + surface input

### AC2: Add More Rows
**Given** I am on quick add form  
**When** I click "Add Row"  
**Then** a new empty row is added  
**And** I can add up to 20 rows

### AC3: Remove Row
**Given** I have 5 rows in quick add form  
**When** I click "Remove" on a row  
**Then** that row is removed  
**And** remaining rows stay intact

### AC4: Save All Rooms
**Given** I have filled 4 rows:
- Living Room: 25 m²
- Bedroom: 15 m²
- Kitchen: 10 m²
- Bathroom: 5 m²  

**When** I click "Save All"  
**Then** all 4 rooms are created  
**And** I see "4 rooms added successfully"  
**And** room list shows all 4 rooms

### AC5: Validation - Skip Empty Rows
**Given** I have 5 rows but only 3 are filled  
**When** I click "Save All"  
**Then** only the 3 filled rows are saved  
**And** empty rows are ignored

### AC6: Validation - Highlight Invalid Rows
**Given** I have filled rows but one has no surface  
**When** I click "Save All"  
**Then** invalid row is highlighted in red  
**And** error message shown  
**And** valid rows can still be saved

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
