# User Story US024: Track Rent Increases Over Time

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US024 |
| **Story Name** | Track Rent Increases Over Time |
| **Epic** | Rent Management |
| **Related UC** | UC005 - Manage Rents |
| **Priority** | COULD HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN  
**I want to** track how rent has increased over time  
**So that** I can analyze rental income trends

---

## Acceptance Criteria

### AC1: Show Rent Trend
**Given** rent history is:
- €750 (2022)
- €800 (2023)
- €850 (2024)  

**When** viewing rent history  
**Then** I see trend indicators:
- 2022→2023: +€50 (+6.67%) ↑
- 2023→2024: +€50 (+6.25%) ↑

### AC2: Calculate Total Increase
**Given** first rent was €750, current is €850  
**When** viewing rent section  
**Then** I see "Total increase: +€100 (+13.33%) over 2 years"

### AC3: Show Decrease
**Given** rent decreased from €900 to €850  
**When** viewing history  
**Then** change shows "-€50 (-5.56%)" with ↓ indicator

### AC4: Visual Chart (Optional)
**Given** viewing rent history  
**Then** a line chart shows rent evolution over time (optional)

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
