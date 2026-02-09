# User Story US025: Add Notes to Rent Changes

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US025 |
| **Story Name** | Add Notes to Rent Changes |
| **Epic** | Rent Management |
| **Related UC** | UC005 - Manage Rents |
| **Priority** | SHOULD HAVE |
| **Story Points** | 1 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** add notes when changing rent  
**So that** I can document the reason for the change

---

## Acceptance Criteria

### AC1: Add Notes When Setting Rent
**Given** I am setting initial rent  
**When** I enter notes "Initial market rate"  
**And** I save  
**Then** notes are stored with rent record

### AC2: Add Notes When Updating Rent
**Given** I am updating rent  
**When** I enter notes "Annual indexation +5%"  
**And** I save  
**Then** notes are stored with new rent record

### AC3: View Notes in History
**Given** rent records have notes  
**When** I view rent history  
**Then** notes column shows each rent's notes  
**And** empty if no notes

### AC4: Notes Field Optional
**Given** I am setting/updating rent  
**When** I leave notes empty  
**And** I save  
**Then** rent is saved successfully  
**And** notes = NULL

### AC5: Common Notes Templates (Optional)
**Given** I am adding notes  
**Then** I can select from common templates:
- "Annual indexation"
- "Market adjustment"
- "After renovation"
- "Tenant negotiation"
- Custom (free text)

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
