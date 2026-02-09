# User Story US004: View Buildings List

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US004 |
| **Story Name** | View Buildings List |
| **Epic** | Building Management |
| **Related UC** | UC001 - Manage Buildings |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN  
**I want to** view a list of all buildings  
**So that** I can see my property portfolio and access individual building details

---

## Acceptance Criteria

### AC1: Display Buildings List
**Given** I am logged in as ADMIN  
**And** 5 buildings exist in the system  
**When** I navigate to the Buildings page  
**Then** I see a list displaying all 5 buildings  
**And** each building shows:
- Building name
- Full address (street, postal code, city, country)
- Owner name (or "Not specified")
- Number of housing units  

**And** the page title is "Buildings"  
**And** a "Create Building" button is visible

---

### AC2: View Empty Buildings List
**Given** I am logged in as ADMIN  
**And** no buildings exist in the system  
**When** I navigate to the Buildings page  
**Then** I see a message "No buildings yet"  
**And** a "Create Building" button is visible  
**And** helpful text suggests "Create your first building to get started"

---

### AC3: Sort Buildings by Name
**Given** I am viewing the buildings list  
**And** buildings exist with names: "Building A", "Building C", "Building B"  
**When** I click the "Name" column header  
**Then** the list is sorted alphabetically by name (A, B, C)  
**When** I click the "Name" column header again  
**Then** the sort order reverses (C, B, A)

---

### AC4: Sort Buildings by City
**Given** I am viewing the buildings list  
**When** I click the "City" column header  
**Then** the list is sorted alphabetically by city  
**And** I can toggle between ascending and descending order

---

### AC5: Filter Buildings by City
**Given** I am viewing the buildings list  
**And** buildings exist in cities: Brussels (3), Antwerp (2), Ghent (1)  
**When** I select "Brussels" from the city filter dropdown  
**Then** only the 3 buildings in Brussels are displayed  
**And** the total count shows "3 buildings"  
**When** I select "All Cities"  
**Then** all 6 buildings are displayed again

---

### AC6: Search Buildings
**Given** I am viewing the buildings list  
**And** buildings exist: "Résidence Soleil" and "Appartements Luna"  
**When** I type "Soleil" in the search box  
**Then** only "Résidence Soleil" is displayed  
**When** I clear the search box  
**Then** all buildings are displayed again

---

### AC7: Search by Address
**Given** I am viewing the buildings list  
**And** a building has address "123 Rue de la Loi"  
**When** I type "Rue de la Loi" in the search box  
**Then** the building is displayed in the results  
**And** search is case-insensitive

---

### AC8: Navigate to Building Details
**Given** I am viewing the buildings list  
**When** I click on building "Résidence Soleil"  
**Then** I am navigated to the building details page  
**And** the page displays full information for "Résidence Soleil"

---

### AC9: Pagination (More than 20 Buildings)
**Given** 50 buildings exist in the system  
**When** I navigate to the Buildings page  
**Then** I see the first 20 buildings  
**And** pagination controls are displayed at the bottom  
**And** the page indicator shows "Page 1 of 3"  
**When** I click "Next" or "2"  
**Then** I see buildings 21-40  
**And** the page indicator shows "Page 2 of 3"

---

### AC10: Display Unit Count
**Given** I am viewing the buildings list  
**And** a building has 5 housing units  
**When** viewing that building in the list  
**Then** the unit count displays as "5 units"  
**And** if a building has 0 units, it displays "0 units"

---

## Technical Notes

### API Endpoint
```
GET /api/v1/buildings?page=0&size=20&sort=name,asc&city=Brussels&search=Soleil

Response:
{
  "content": [
    {
      "id": 1,
      "name": "Résidence Soleil",
      "streetAddress": "123 Rue de la Loi",
      "postalCode": "1000",
      "city": "Brussels",
      "country": "Belgium",
      "ownerName": "Jean Dupont",
      "unitCount": 5,
      "createdAt": "2024-01-15T10:00:00Z"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

### Query Parameters
- `page`: Page number (0-indexed)
- `size`: Items per page (default 20)
- `sort`: Sort field and direction (e.g., "name,asc")
- `city`: Filter by city (optional)
- `search`: Search in name and address (optional)

---

## UI Mockup Notes

### Layout
- Page header: "Buildings" + "Create Building" button (right)
- Filter/Search bar below header
- Table or card view (configurable)
- Pagination controls at bottom

### Table Columns
1. Name (clickable, sortable)
2. Address (street, postal code, city)
3. Country
4. Owner (or "Not specified")
5. Units (count, sortable)
6. Actions (View icon)

### Search/Filter Bar
- Search input (left): "Search buildings..."
- City filter dropdown (middle): "All Cities", "Brussels", "Antwerp", etc.
- View toggle (right): Table/Cards (optional)

### Responsive Design
- Desktop: Table view
- Tablet: Card view
- Mobile: Compact card view

---

## Dependencies

- US001 (Create Building) for test data
- User must have ADMIN role
- Pagination library (frontend)

---

## Testing Checklist

- [ ] Empty list displays correctly
- [ ] List displays all buildings
- [ ] Each building shows correct information
- [ ] Can sort by name (ascending/descending)
- [ ] Can sort by city
- [ ] Can filter by city
- [ ] Can search by building name
- [ ] Can search by address
- [ ] Search is case-insensitive
- [ ] Can click building to view details
- [ ] Pagination works correctly (if > 20 buildings)
- [ ] Unit count displays correctly
- [ ] "Create Building" button visible and functional

---

## Performance Requirements

- List must load in < 1 second (up to 1000 buildings)
- Search should update results in < 500ms
- Sorting should be instant (< 200ms)
- Pagination should load new page in < 500ms

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] UI matches mockup
- [ ] All acceptance criteria met
- [ ] Performance requirements met
- [ ] Manual testing completed
- [ ] Documentation updated
- [ ] Merged to main branch

---

**Last Updated**: 2024-01-15  
**Status**: Ready for Development
