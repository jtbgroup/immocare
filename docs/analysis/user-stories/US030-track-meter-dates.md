# User Story US030: Track Meter Installation Dates

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US030 |
| **Story Name** | Track Meter Installation Dates |
| **Epic** | Water Meter Management |
| **Related UC** | UC006 - Manage Water Meters |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** see when water meters were installed and how long they've been active  
**So that** I can plan for maintenance or replacements

---

## Acceptance Criteria

### AC1: Display Installation Date
**Given** a meter installed on 2024-01-15  
**When** I view unit details  
**Then** Water Meter section shows:
- "Installed: 2024-01-15"
- Days active (calculated)

### AC2: Calculate Active Duration
**Given** meter installed 2024-01-01  
**And** today is 2024-07-01  
**When** viewing meter info  
**Then** I see "Active for: 6 months"

### AC3: Show Historical Duration
**Given** meter from 2024-01-01 to 2024-06-01  
**When** viewing history  
**Then** duration shows "5 months"

### AC4: Meter Age Indicator (Optional)
**Given** meter installed more than 10 years ago  
**When** viewing meter info  
**Then** I see warning "Old meter - consider replacement"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
