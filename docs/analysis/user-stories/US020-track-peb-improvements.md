# User Story US020: Track PEB Score Improvements

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US020 |
| **Story Name** | Track PEB Score Improvements |
| **Epic** | PEB Score Management |
| **Related UC** | UC004 - Manage PEB Scores |
| **Priority** | COULD HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN  
**I want to** see if PEB scores have improved over time  
**So that** I can evaluate the impact of energy efficiency renovations

---

## Acceptance Criteria

### AC1: Show Improvement Indicator
**Given** a unit's PEB history is: D (2020), C (2022), B (2024)  
**When** I view PEB section or history  
**Then** I see improvement indicators:
- D → C: "Improved" (green arrow ↑)
- C → B: "Improved" (green arrow ↑)

### AC2: Show Degradation Indicator
**Given** a unit's score changed from B to C  
**When** I view PEB history  
**Then** I see "Degraded" (red arrow ↓)

### AC3: Show No Change
**Given** consecutive scores are both B  
**When** I view history  
**Then** I see "No change" (gray dash −)

### AC4: Calculate Total Improvement
**Given** first score was F (2018), current is B (2024)  
**When** I view PEB section  
**Then** I see "Improved by 4 grades over 6 years"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
