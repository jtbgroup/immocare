# User Story US005: Search Buildings

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US005 |
| **Story Name** | Search Buildings |
| **Epic** | Building Management |
| **Related UC** | UC001 - Manage Buildings |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN  
**I want to** search for buildings by name or address  
**So that** I can quickly find a specific property in my portfolio

---

## Acceptance Criteria

### AC1: Search by Building Name
**Given** buildings exist with names: "Résidence Soleil", "Appartements Luna", "Villa Aurora"  
**When** I type "Luna" in the search box  
**Then** only "Appartements Luna" is displayed  
**And** other buildings are hidden

### AC2: Search by Address
**Given** a building has address "123 Rue de la Loi, Brussels"  
**When** I type "Rue de la Loi" in the search box  
**Then** the building is displayed in results

### AC3: Case-Insensitive Search
**Given** a building named "Résidence Soleil" exists  
**When** I search for "soleil" (lowercase)  
**Then** "Résidence Soleil" is found

### AC4: Partial Match Search
**Given** a building named "Résidence Soleil" exists  
**When** I search for "Rési"  
**Then** "Résidence Soleil" is found

### AC5: Clear Search
**Given** I have searched for "Luna"  
**And** only filtered results are displayed  
**When** I clear the search box  
**Then** all buildings are displayed again

### AC6: No Results Found
**Given** no buildings match my search term  
**When** I search for "XYZ123"  
**Then** I see message "No buildings found"  
**And** helpful text "Try a different search term"

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
