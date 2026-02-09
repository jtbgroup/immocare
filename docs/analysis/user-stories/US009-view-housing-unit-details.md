# User Story US009: View Housing Unit Details

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US009 |
| **Story Name** | View Housing Unit Details |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 - Manage Housing Units |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 2 |

---

## User Story

**As an** ADMIN  
**I want to** view complete housing unit details  
**So that** I can see all information about a specific apartment

---

## Acceptance Criteria

### AC1: Display Unit Information
**Given** a housing unit exists  
**When** I view the unit details page  
**Then** I see all unit information:
- Unit number, floor, landing
- Total surface
- Terrace info (if applicable)
- Garden info (if applicable)
- Owner name (inherited or specific)
- Building link

### AC2: Display Rooms Section
**Given** a unit has 3 rooms  
**When** I view unit details  
**Then** the Rooms section shows:
- List of all 3 rooms with types and surfaces
- Total calculated surface
- "Add Room" button

### AC3: Display Current PEB Score
**Given** a unit has PEB score B from 2024-01-15  
**When** I view unit details  
**Then** the PEB section shows:
- Score badge (B)
- Score date
- "View History" link
- "Add PEB Score" button

### AC4: Display Current Rent
**Given** a unit has rent €850/month from 2024-01-01  
**When** I view unit details  
**Then** the Rent section shows:
- Current rent amount
- Effective from date
- "Update Rent" button
- "View History" link

### AC5: Display Current Water Meter
**Given** a unit has active meter "WM-2024-001"  
**When** I view unit details  
**Then** the Water Meter section shows:
- Meter number
- Installation date
- Location (if specified)
- "Replace Meter" button

### AC6: Navigate to Building
**Given** I am viewing a unit  
**When** I click the building name link  
**Then** I am navigated to the building details page

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
