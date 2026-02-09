# User Story US019: Check PEB Certificate Validity

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US019 |
| **Story Name** | Check PEB Certificate Validity |
| **Epic** | PEB Score Management |
| **Related UC** | UC004 - Manage PEB Scores |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 3 |

---

## User Story

**As an** ADMIN  
**I want to** see if PEB certificates are expired or expiring soon  
**So that** I can plan for certificate renewals

---

## Acceptance Criteria

### AC1: Show Expired Certificate
**Given** a PEB score has valid until 2023-12-31  
**And** today is 2024-01-15  
**When** I view unit details  
**Then** PEB section shows "Expired" warning in red  
**And** suggests "Certificate needs renewal"

### AC2: Show Expiring Soon Warning
**Given** a PEB score valid until 3 months from now  
**When** I view unit details  
**Then** I see "Expires soon" warning in orange  
**And** expiry date is highlighted

### AC3: Show Valid Certificate
**Given** a PEB score valid until 5 years from now  
**When** I view unit details  
**Then** no warning is shown  
**And** valid until date displayed normally

### AC4: No Expiry Date Specified
**Given** a PEB score has no valid until date  
**When** I view unit details  
**Then** no expiry warning shown  
**And** displays "Validity period not specified"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
