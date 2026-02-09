# User Story US018: View PEB Score History

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US018 |
| **Story Name** | View PEB Score History |
| **Epic** | PEB Score Management |
| **Related UC** | UC004 - Manage PEB Scores |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** view the complete PEB score history of a housing unit  
**So that** I can track energy efficiency improvements over time

---

## Acceptance Criteria

### AC1: Display Current Score in Unit Details
**Given** a unit has PEB score B from 2024  
**When** I view unit details  
**Then** PEB section shows:
- Current score badge (B with yellow-green color)
- Score date: 2024-01-15
- "View History" link

### AC2: View Complete History
**Given** a unit has 3 PEB scores: D (2020), C (2022), B (2024)  
**When** I click "View History"  
**Then** a history table/modal is displayed  
**And** all 3 scores are shown  
**And** sorted by date (newest first)

### AC3: History Table Columns
**Given** I am viewing PEB history  
**Then** the table shows:
- Score (with color badge)
- Score Date
- Certificate Number
- Valid Until
- Status (Current/Historical/Expired)

### AC4: Visual Timeline (Optional)
**Given** I am viewing PEB history  
**Then** scores can be displayed as timeline (optional)  
**And** shows progression from D → C → B

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
