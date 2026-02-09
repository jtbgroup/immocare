# User Story US017: Add PEB Score

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US017 |
| **Story Name** | Add PEB Score |
| **Epic** | PEB Score Management |
| **Related UC** | UC004 - Manage PEB Scores |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** add a PEB (Energy Performance Certificate) score to a housing unit  
**So that** I can track the energy efficiency of the property

---

## Acceptance Criteria

### AC1: Display Add PEB Form
**Given** I am viewing a housing unit  
**When** I click "Add PEB Score"  
**Then** the PEB score form is displayed  
**And** required fields are marked

### AC2: PEB Score Dropdown
**Given** I am on PEB form  
**When** I click the score dropdown  
**Then** I see all options with color coding:
- A++ (dark green), A+ (green), A (light green)
- B (yellow-green), C (yellow), D (orange)
- E (light red), F (red), G (dark red)

### AC3: Add First PEB Score
**Given** a unit has no PEB score  
**When** I add score B dated 2024-01-15  
**And** I save  
**Then** score is saved  
**And** unit details show score B as current

### AC4: Add Newer PEB Score
**Given** unit has score C from 2023  
**When** I add score B from 2024  
**Then** B becomes current score  
**And** C becomes historical

### AC5: Add Certificate Details
**Given** I am adding PEB score  
**When** I enter:
- Score: B
- Date: 2024-01-15
- Certificate: PEB-2024-123456
- Valid until: 2034-01-15  

**And** I save  
**Then** all details are stored

### AC6: Validation - Score Date Required
**Given** I am adding PEB score  
**When** I leave date empty  
**Then** I see error "Score date is required"

### AC7: Validation - Future Date
**Given** I am adding PEB score  
**When** I enter date in future  
**Then** I see error "Score date cannot be in the future"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
