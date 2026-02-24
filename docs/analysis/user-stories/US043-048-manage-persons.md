# User Stories UC009 — Manage Persons

---

## US043: View Persons List

| Attribute | Value |
|-----------|-------|
| **Story ID** | US043 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** view a list of all persons in the system
**So that** I can find and manage owners and tenants

### Acceptance Criteria

**AC1: Display persons list**
Given I navigate to the Persons page
Then I see a paginated list of all persons
And each row shows: Last Name, First Name, Email, GSM, City, Role badges
And the list is sorted by last name ASC by default

**AC2: Role badges**
Given a person is owner of a building
Then a blue "Owner" badge is shown on their row
Given a person is tenant on an active lease
Then a green "Tenant" badge is shown on their row

**AC3: Empty state**
Given no persons exist
Then I see "No persons yet" message and "Add Person" button

**AC4: Pagination**
Given more than 20 persons exist
Then pagination controls are shown and 20 persons displayed per page

**AC5: Search**
Given I type "dupont" in the search bar
Then only persons with "dupont" in their name, email, or national ID are shown (case-insensitive)

---

## US044: Create Person

| Attribute | Value |
|-----------|-------|
| **Story ID** | US044 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN
**I want to** create a new person record
**So that** I can assign them as owner or tenant

### Acceptance Criteria

**AC1: Display creation form**
Given I click "Add Person"
Then a form is displayed with sections: Identity, Contact, Address
And Last Name and First Name are marked as required (*)

**AC2: Create with minimum fields**
Given I enter Last Name "Dupont" and First Name "Jean"
When I click Save
Then the person is created and I see the details page

**AC3: Create with all fields**
Given I fill all fields including national ID "85.01.15-123.45"
When I click Save
Then all data is stored correctly

**AC4: Validation — duplicate national ID**
Given a person with national ID "85.01.15-123.45" already exists
When I save a new person with the same national ID
Then I see error "A person with this national ID already exists"
And I see a link to the existing person

**AC5: Validation — required fields**
Given I leave Last Name empty
When I click Save
Then I see error "Last name is required"

**AC6: Validation — email format**
Given I enter "not-an-email" in the email field
When I click Save
Then I see error "Please enter a valid email address"

**AC7: Validation — birth date not in future**
Given I enter a birth date in the future
When I click Save
Then I see error "Date of birth cannot be in the future"

**AC8: Country defaults to Belgium**
Given I open the creation form
Then the Country field is pre-filled with "Belgium"

---

## US045: Edit Person

| Attribute | Value |
|-----------|-------|
| **Story ID** | US045 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** edit a person's information
**So that** I can keep data accurate and up to date

### Acceptance Criteria

**AC1: Pre-filled form**
Given I click "Edit" on a person's details
Then all current values are pre-filled in the form

**AC2: Edit and save successfully**
Given I change the email to "new@email.com"
When I click Save
Then the person record is updated
And I see success message "Person updated successfully"

**AC3: Updated data reflected everywhere**
Given I update a person's name
Then the new name appears on all leases and buildings referencing this person

**AC4: Validation applies same rules as creation**
Given I clear the Last Name field
When I click Save
Then I see error "Last name is required"

**AC5: National ID uniqueness on edit**
Given another person has national ID "85.01.15-123.45"
When I edit a different person and set the same national ID
Then I see error "A person with this national ID already exists"

---

## US046: Delete Person

| Attribute | Value |
|-----------|-------|
| **Story ID** | US046 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** delete a person record
**So that** I can remove obsolete or incorrectly created entries

### Acceptance Criteria

**AC1: Delete unreferenced person**
Given a person has no linked buildings, units, or leases
When I click Delete and confirm
Then the person is permanently deleted
And I am redirected to the persons list

**AC2: Confirmation dialog**
Given I click Delete
Then a dialog appears: "Are you sure you want to delete [Name]? This action cannot be undone."
And Cancel and Confirm Delete buttons are shown

**AC3: Cannot delete person who is owner**
Given a person is owner of Building "Résidence Soleil"
When I click Delete
Then I see error "This person cannot be deleted. They are owner of: Résidence Soleil"
And the person is not deleted

**AC4: Cannot delete person who is tenant**
Given a person is tenant on an active lease (Unit A101)
When I click Delete
Then I see error "This person cannot be deleted. They are tenant on: Lease #12 (Unit A101, ACTIVE)"
And the person is not deleted

---

## US047: Assign Person as Owner to Building or Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US047 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN
**I want to** assign a person as the owner of a building or housing unit
**So that** ownership is properly tracked and linked to real person records

### Acceptance Criteria

**AC1: Person picker on building form**
Given I am editing a building
Then the "Owner" field shows a search-as-you-type person picker
And I can search by name or national ID

**AC2: Assign owner to building**
Given I search "Dupont" and select "Dupont, Jean"
When I save the building
Then Jean Dupont is shown as owner on the building details page

**AC3: Clear owner from building**
Given a building has an owner
When I clear the owner field and save
Then the building shows "No owner specified"

**AC4: Unit inherits building owner**
Given building owner is Jean Dupont
And housing unit has no specific owner
Then the unit details show "Owner: Jean Dupont (inherited from building)"

**AC5: Unit with specific owner overrides building**
Given I assign Marie Martin as owner of housing unit A101
And the building owner is Jean Dupont
Then unit A101 shows "Owner: Marie Martin" (not Jean Dupont)

**AC6: "Create new person" shortcut**
Given I open the owner picker and no matching person exists
Then I see a "Create new person" link
When I click it, a quick-create person modal opens
After saving the new person, they are auto-selected in the picker

---

## US048: Search Person (Picker)

| Attribute | Value |
|-----------|-------|
| **Story ID** | US048 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** quickly search and select a person from any form that requires one
**So that** I don't need to navigate away to find a person

### Acceptance Criteria

**AC1: Search by name**
Given I type "dup" in a person picker
Then within 300ms, a dropdown shows matching persons (case-insensitive)
And each result shows: Full name, City, National ID (if available)

**AC2: Minimum 2 characters to search**
Given I type only 1 character
Then no search is triggered and a hint "Type at least 2 characters" is shown

**AC3: No results**
Given my search returns no matching persons
Then the dropdown shows "No person found" and a "Create new person" link

**AC4: Select a person**
Given results are shown
When I click on a person
Then the picker closes and the selected person is shown in the field

**AC5: Clear selection**
Given a person is selected
When I click the X button next to the name
Then the selection is cleared and the picker is ready for a new search

---

**Last Updated**: 2026-02-24
**Status**: 📋 Ready for Implementation
