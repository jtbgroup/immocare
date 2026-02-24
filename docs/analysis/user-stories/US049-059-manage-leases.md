# User Stories UC010 — Manage Leases

---

## US049: View Lease for Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US049 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** see the current lease status directly on the housing unit details page
**So that** I can quickly check occupancy and key lease information

### Acceptance Criteria

**AC1: No active lease**
Given a unit has no active or draft lease
Then the Leases section shows "No active lease"
And a "Create Lease" button is visible

**AC2: Active lease displayed**
Given a unit has an active lease
Then the Leases section shows:
- Status badge (ACTIVE, green)
- Tenant name(s)
- Monthly rent and charges
- Start date and end date
- "View" and "Edit" buttons

**AC3: Draft lease displayed**
Given a unit has a draft lease
Then the Leases section shows the lease with status badge "DRAFT" (grey)

**AC4: Alert — end notice deadline**
Given the lease end notice deadline (end_date − notice_period_months) is within 30 days
Then an orange alert banner is shown: "⚠ Lease ending soon — notice deadline: [date]"

**AC5: Alert — indexation due**
Given the indexation anniversary is within `indexation_notice_days` days
And no indexation has been recorded for this anniversary year
Then an orange alert banner is shown: "⚠ Indexation due — anniversary: [date]"

**AC6: View lease history**
Given a unit has had multiple leases
When I click "View all leases"
Then a list of all leases (all statuses) is shown, sorted by start date DESC

---

## US050: Create Lease (Draft)

| Attribute | Value |
|-----------|-------|
| **Story ID** | US050 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 8 |

**As an** ADMIN
**I want to** create a new lease for a housing unit
**So that** I can formalize the rental agreement

### Acceptance Criteria

**AC1: Creation blocked when active/draft lease exists**
Given unit A101 already has an ACTIVE lease
When I try to create a new lease on A101
Then I see error: "An active or draft lease already exists for this unit. Finish or cancel it first."

**AC2: Form displays all sections**
Given I click "Create Lease" on a unit
Then the form displays sections: Dates & Duration / Financial / Indexation / Registration / Guarantee & Insurance / Tenants
And the housing unit is pre-selected and read-only

**AC3: End date auto-calculated**
Given I enter start date 2026-03-01 and duration 36 months
Then the end date field automatically shows 2029-03-01

**AC4: Default notice period by lease type**
Given I select lease type "MAIN_RESIDENCE_9Y"
Then the notice period field is pre-filled with 3 months (but editable)

**AC5: Add primary tenant**
Given I am in the Tenants section
When I search and select "Jean Dupont" and assign role "PRIMARY"
Then Jean Dupont appears in the tenant list

**AC6: Save as Draft**
Given all required fields are filled and at least one PRIMARY tenant is added
When I click "Save as Draft"
Then the lease is created with status DRAFT
And I see success message "Lease saved as draft"

**AC7: Validation — primary tenant required**
Given I have not added any tenant
When I click Save
Then I see error: "At least one primary tenant is required"

**AC8: Validation — duration must be positive**
Given I enter duration = 0
When I click Save
Then I see error: "Duration must be at least 1 month"

**AC9: Validation — signature date required**
Given I leave signature date empty
When I click Save
Then I see error: "Signature date is required"

**AC10: Charges type drives description field**
Given I select charges type "PROVISION"
Then the Charges Description field becomes visible with hint "Describe what charges are included"
Given I select "FORFAIT"
Then the Charges Description field is still available (optional)

**AC11: Optional fields are truly optional**
Given I leave SPF registration, region registration, base index, deposit, and insurance fields empty
When I save the lease
Then the lease is saved without errors

---

## US051: Activate Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US051 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** activate a draft lease
**So that** it becomes the official active lease for the unit

### Acceptance Criteria

**AC1: Activate button visible on DRAFT lease**
Given I am viewing a DRAFT lease
Then an "Activate" button is shown

**AC2: Confirmation dialog**
Given I click "Activate"
Then a confirmation dialog shows: "Activate this lease? It will become the official active lease for unit [X]."

**AC3: Lease becomes ACTIVE**
Given I confirm activation
Then the lease status changes to ACTIVE
And the status badge on the unit details shows "ACTIVE"

**AC4: Cannot activate if another ACTIVE lease exists**
Given unit A101 already has an ACTIVE lease (created after this DRAFT)
When I try to activate the DRAFT
Then I see error: "Activation blocked: another lease is already active for this unit"

---

## US052: Edit Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US052 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN
**I want to** edit an existing lease
**So that** I can correct data or add information obtained after signing (e.g., registration numbers)

### Acceptance Criteria

**AC1: Edit form pre-filled**
Given I click "Edit" on a DRAFT or ACTIVE lease
Then the form is pre-filled with all current values

**AC2: Save changes**
Given I update the SPF registration number
When I click Save
Then the lease is updated and the new value is shown on details

**AC3: End date recalculates when start date or duration changes**
Given I change duration from 36 to 48 months
Then the end date field updates automatically

**AC4: FINISHED and CANCELLED leases are read-only**
Given I navigate to a FINISHED lease
Then no "Edit" button is shown
And all fields are displayed in read-only mode

**AC5: Validation same as creation**
Given I clear the monthly rent field
When I click Save
Then I see error: "Monthly rent is required"

---

## US053: Finish Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US053 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** mark a lease as finished
**So that** I can record that the tenant has vacated and the unit is available

### Acceptance Criteria

**AC1: Finish button on ACTIVE lease**
Given I am viewing an ACTIVE lease
Then a "Finish Lease" button is shown

**AC2: Confirmation with effective date**
Given I click "Finish Lease"
Then a dialog asks for: Effective end date (required) and Notes (optional)
And the effective date defaults to today

**AC3: Lease becomes FINISHED**
Given I confirm with effective date
Then the lease status changes to FINISHED
And the unit Leases section shows "No active lease" with a "Create Lease" button

**AC4: Historical lease preserved**
Given the lease is FINISHED
Then it still appears in the lease history for the unit

---

## US054: Cancel Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US054 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** cancel a draft or active lease
**So that** I can record an early termination or a draft that was never used

### Acceptance Criteria

**AC1: Cancel button on DRAFT and ACTIVE leases**
Given I am viewing a DRAFT or ACTIVE lease
Then a "Cancel Lease" button is shown (styled in red or warning)

**AC2: Confirmation dialog**
Given I click "Cancel Lease"
Then a dialog shows: "Are you sure you want to cancel this lease? This action cannot be undone."
And asks for: Cancellation date and Reason/notes (optional)

**AC3: Lease becomes CANCELLED**
Given I confirm
Then the lease status changes to CANCELLED
And the unit is available for a new lease

**AC4: Cancelled lease preserved in history**
Then the cancelled lease still appears in the unit's lease history with status badge "CANCELLED" (red)

---

## US055: Record Indexation

| Attribute | Value |
|-----------|-------|
| **Story ID** | US055 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN
**I want to** record a rent indexation on an active lease
**So that** the new rent amount is tracked and the indexation history is complete

### Acceptance Criteria

**AC1: "Record Indexation" button on ACTIVE lease**
Given I am viewing an ACTIVE lease
Then a "Record Indexation" button is shown in the Indexation section

**AC2: Form pre-fills old rent**
Given I click "Record Indexation"
Then a form shows the current rent as "Old Rent (read-only)"
And fields: Application Date, New Index Value, New Index Month (month/year), Applied Rent, Notification Sent Date (optional), Notes (optional)

**AC3: Record indexation successfully**
Given I fill: Application Date=2026-04-01, New Index=121.45, Index Month=2026-02, Applied Rent=920.00
When I click Save
Then a record is added to the indexation history
And the lease's monthly rent is updated to 920.00

**AC4: Cannot record indexation on non-ACTIVE lease**
Given I navigate to a DRAFT lease
Then no "Record Indexation" button is visible

**AC5: Validation — all required fields**
Given I leave Application Date empty
When I click Save
Then I see error: "Application date is required"

**AC6: Applied rent must be positive**
Given I enter Applied Rent = 0
When I click Save
Then I see error: "Applied rent must be greater than 0"

---

## US056: View Indexation History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US056 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** view the complete indexation history of a lease
**So that** I can track all rent changes over time

### Acceptance Criteria

**AC1: Indexation section on lease details**
Given a lease has 2 indexation records
Then the Indexation section shows a collapsible table with both records
And columns: Application Date | Old Rent | Index Value | Index Month | Applied Rent | Notification Date | Notes

**AC2: Sorted newest first**
Given multiple indexation records exist
Then they are shown with the most recent application date first

**AC3: No indexations**
Given no indexation has been recorded
Then the section shows "No indexation recorded yet"

**AC4: Total indexation since start**
Given the original rent was 800€ and current is 920€
Then a summary shows: "Total indexation: +120.00€ (+15.0%) since lease start"

---

## US057: Add Tenant to Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US057 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN
**I want to** add a tenant (or co-tenant or guarantor) to a lease
**So that** all occupants are formally registered on the contract

### Acceptance Criteria

**AC1: Tenant section on lease details**
Given I am viewing a lease
Then the Tenants section shows all current tenants with their role and contact info

**AC2: Add tenant via person picker**
Given I click "Add Tenant"
Then a person picker opens with a role selector (PRIMARY / CO_TENANT / GUARANTOR)
When I select a person and a role and click "Add"
Then the tenant is added to the lease

**AC3: Multiple tenants allowed**
Given a lease already has one PRIMARY tenant
When I add a CO_TENANT
Then the lease shows both tenants

**AC4: Duplicate person prevention**
Given "Jean Dupont" is already a tenant on this lease
When I try to add Jean Dupont again
Then I see error: "This person is already a tenant on this lease"

**AC5: "Create new person" shortcut**
Given the person I want to add doesn't exist yet
When I click "Create new person" in the picker
Then a quick-create person form opens
After saving, the new person is added as tenant

---

## US058: Remove Tenant from Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US058 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN
**I want to** remove a tenant from a lease
**So that** I can correct errors or record departure of a co-tenant

### Acceptance Criteria

**AC1: Remove button per tenant**
Given I am viewing the tenants list on a lease
Then each tenant row has a "Remove" button

**AC2: Confirmation dialog**
Given I click "Remove" on a tenant
Then a dialog: "Remove [Name] from this lease?"

**AC3: Remove co-tenant successfully**
Given the lease has 1 PRIMARY and 1 CO_TENANT
When I remove the CO_TENANT
Then only the PRIMARY tenant remains

**AC4: Cannot remove last PRIMARY tenant**
Given the lease has only 1 PRIMARY tenant
When I try to remove them
Then I see error: "Cannot remove the only primary tenant. Add another primary tenant first."

**AC5: Remove guarantor**
Given the lease has a GUARANTOR
When I remove the guarantor
Then the guarantor is removed without affecting other tenants

---

## US059: View Lease Alerts

| Attribute | Value |
|-----------|-------|
| **Story ID** | US059 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 3 |

**As an** ADMIN
**I want to** see all upcoming lease deadlines in one place
**So that** I don't miss any indexation or end-of-lease notification obligation

### Acceptance Criteria

**AC1: Alerts visible on lease and unit details**
Given an active lease has an indexation anniversary within `indexation_notice_days` days
Then an orange alert banner appears on both the lease details page and the unit details page

**AC2: End notice alert**
Given an active lease's end notice deadline (end_date − notice_period_months) is within 30 days
Then an orange alert banner appears: "⚠ Lease ending soon — send notice before [date]"

**AC3: Alerts summary page or dashboard section**
Given I navigate to a global "Alerts" page (or dashboard section)
Then I see a list of all pending alerts across all units:
- Unit, Lease ID, Alert type (Indexation / End Notice), Deadline date
Sorted by deadline date ASC

**AC4: Alert disappears after action**
Given an indexation was recorded for this anniversary year
Then the indexation alert for that lease is no longer shown

**AC5: No alert when no active leases**
Given no active leases exist
Then the alerts section shows "No pending alerts"

---

**Last Updated**: 2026-02-24
**Status**: 📋 Ready for Implementation
