# Use Case UC010: Manage Leases

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC010 |
| **Use Case Name** | Manage Leases |
| **Version** | 1.0 |
| **Status** | 📋 Ready for Implementation |
| **Priority** | HIGH |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; At least one housing unit exists; At least one person exists |
| **Postconditions** | Lease data is created, updated, or archived in the system |
| **Related Use Cases** | UC009 (Manage Persons), UC002 (Manage Housing Units), UC004 (Manage PEB Scores), UC005 (Manage Rents) |

---

## Description

This use case describes how an administrator manages lease contracts (baux) linked to housing units. A lease formalizes the rental relationship between the owner and one or more tenants, with all legal and financial details. The system enforces non-overlap of active leases per unit, tracks indexation history, and generates UI alerts for upcoming deadlines (indexation anniversary, lease end notice).

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator
- **Goal**: Create, view, edit, manage status, and record indexation for leases
- **Characteristics**:
  - Has access to signed lease documents
  - Manages multiple units and tenants

---

## Preconditions

1. User authenticated as ADMIN
2. At least one housing unit exists
3. At least one person exists (for tenants)
4. System operational

---

## Basic Flow

### 1. View Leases for a Housing Unit

**Trigger**: ADMIN navigates to housing unit details

1. System displays a "Leases" section showing:
   - Active lease (if any): tenant names, start date, end date, monthly rent, status badge
   - "View all leases" link to see full history
2. If no active lease: message "No active lease" with "Create Lease" button
3. If active lease exists: "View" and "Edit" buttons

**Result**: ADMIN sees current lease status at a glance

---

### 2. View Lease Details

**Trigger**: ADMIN clicks on a lease

1. System displays full lease details in sections:

   **Identification**
   - Lease ID, status badge (DRAFT / ACTIVE / FINISHED / CANCELLED)
   - Housing unit (link), building (link)

   **Dates & Duration**
   - Signature date
   - Start date (prise d'effet)
   - Lease type
   - Duration (months)
   - End date (calculated)
   - Notice period (months)
   - ⚠ Alert if notice deadline is approaching

   **Financial**
   - Monthly rent (contractual)
   - Monthly charges + type (FORFAIT / PROVISION)
   - Charges description
   - Link to rent_history reference rent

   **Indexation**
   - Base index value + base index month
   - Anniversary month
   - ⚠ Alert if indexation deadline is approaching
   - Indexation history table (collapsible)

   **Registration**
   - SPF Finances registration number (optional)
   - Region registration number (optional)

   **Guarantee**
   - Deposit amount
   - Deposit type
   - Deposit reference

   **Tenant Insurance**
   - Confirmed: yes/no
   - Policy reference (optional)
   - Expiry date (optional)

   **Tenants**
   - List of tenants: full name, role (PRIMARY / CO_TENANT / GUARANTOR), contact info

2. ADMIN can: Edit lease, Change status, Record indexation, Add/remove tenant

**Result**: ADMIN has complete view of the lease

---

### 3. Create New Lease (DRAFT)

**Trigger**: ADMIN clicks "Create Lease" on housing unit details

1. System checks: no currently ACTIVE or DRAFT lease on this unit
   - If conflict: error "An active or draft lease already exists for this unit"
2. System displays multi-section lease creation form
3. ADMIN fills all sections (see Data Elements)
4. ADMIN adds at least one tenant (primary tenant required)
5. ADMIN clicks "Save as Draft" or "Save and Activate"
6. System validates all fields (see Business Rules)
7. System creates lease with status DRAFT or ACTIVE
8. System redirects to lease details
9. System shows success message

**Result**: New lease is created

---

### 4. Edit Lease

**Trigger**: ADMIN clicks "Edit" on lease details

1. System displays pre-filled form (same layout as creation)
2. ADMIN modifies fields
3. ADMIN clicks "Save"
4. System validates and updates lease
5. System shows success message

**Notes**:
- An ACTIVE lease can be edited (corrections, adding registration numbers after the fact)
- A FINISHED or CANCELLED lease cannot be edited

---

### 5. Change Lease Status

**Trigger**: ADMIN clicks status action button

Allowed transitions:
- DRAFT → ACTIVE (activate lease)
- DRAFT → CANCELLED (cancel before activation)
- ACTIVE → FINISHED (lease ended normally at end date)
- ACTIVE → CANCELLED (early termination)

1. System shows confirmation dialog with transition details
2. For FINISHED/CANCELLED: system asks for effective date and optional notes
3. System updates status
4. System records timestamp of status change

**Result**: Lease status is updated

---

### 6. Record Indexation

**Trigger**: ADMIN clicks "Record Indexation" on active lease

1. System displays indexation form:
   - Application date (date of effect of new rent) — required
   - Old rent (pre-filled, read-only)
   - New index value (manual entry) — required
   - New index month (month/year of the new index) — required
   - Applied rent (amount actually applied after admin calculation) — required
   - Notification sent date (optional)
   - Notes (optional)

2. ADMIN fills form and saves
3. System creates a record in `lease_indexation_history`
4. System updates `lease.monthly_rent` to the new applied rent
5. System shows success message

**Result**: Indexation is recorded; lease rent is updated

---

### 7. Manage Tenants on Lease

**Trigger**: ADMIN clicks "Manage Tenants" on lease details

1. System displays list of current tenants with their roles
2. ADMIN can:
   - Add a tenant: search by name from Person picker, assign role
   - Change tenant role
   - Remove a tenant (with confirmation; at least one PRIMARY tenant must remain)
3. System validates and saves changes

**Result**: Tenant list on lease is updated

---

## Exception Flows

### Exception Flow 1: Overlapping Lease
**Trigger**: ADMIN tries to create/activate a lease on a unit that already has an ACTIVE or DRAFT lease

1. System detects the conflict
2. System displays: "Unit [X] already has an active lease (from [date] to [date]). Finish or cancel it before creating a new one."
3. Lease is not saved

---

### Exception Flow 2: No Primary Tenant
**Trigger**: ADMIN tries to save lease without a PRIMARY tenant

1. System displays: "At least one primary tenant is required"
2. Lease is not saved

---

### Exception Flow 3: End Date Before Start Date
**Trigger**: Calculated end date is invalid

1. System displays: "End date cannot be before start date. Check duration and start date."

---

## Business Rules

### BR-UC010-01: No Overlapping Active Leases
Only one ACTIVE or DRAFT lease may exist per housing unit at any time.
**Rationale**: A unit cannot be rented to two tenants simultaneously.

### BR-UC010-02: End Date is Calculated
`end_date = start_date + duration_months`. It is never entered manually.
**Rationale**: Ensures mathematical consistency.

### BR-UC010-03: At Least One Primary Tenant
Every lease must have exactly one or more tenants, with at least one having role PRIMARY.
**Rationale**: Legal requirement — a lease must have a named primary tenant.

### BR-UC010-04: Rent Link to rent_history
The `monthly_rent` on the lease is the contractual amount. It is initialized by the admin (often from the current `rent_history` reference rent) but is stored independently. Indexations update `lease.monthly_rent` only; they do not modify `rent_history`.
**Rationale**: Indicative rent (rent_history) and contractual rent (lease) serve different purposes.

### BR-UC010-05: Lease Type Determines Default Notice Period
| Type | Default notice (months) |
|------|------------------------|
| SHORT_TERM | 1 |
| MAIN_RESIDENCE_3Y | 3 |
| MAIN_RESIDENCE_6Y | 3 |
| MAIN_RESIDENCE_9Y | 3 |
| STUDENT | 1 |
| GLIDING | 1 |
| COMMERCIAL | 6 |

Admin can override.

### BR-UC010-06: No Edit on Finished/Cancelled Lease
Completed leases are read-only.
**Rationale**: Audit integrity.

### BR-UC010-07: Indexation Only on Active Lease
Indexation can only be recorded on a lease with status ACTIVE.

### BR-UC010-08: Indexation Anniversary Alert
System computes the alert date = current year's anniversary date − `indexation_notice_days`. If today >= alert date and no indexation has been recorded this anniversary year, an alert is displayed on the lease and unit details.

### BR-UC010-09: Lease End Notice Alert
System computes the alert date = `end_date` − `notice_period_months` × 30 days. If today >= alert date and lease is still ACTIVE, an alert is displayed.

---

## Data Elements

### Lease

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| housing_unit_id | FK | YES | |
| status | Enum | YES | DRAFT / ACTIVE / FINISHED / CANCELLED |
| signature_date | Date | YES | |
| start_date | Date | YES | Prise d'effet |
| lease_type | Enum | YES | See types below |
| duration_months | Integer | YES | > 0 |
| end_date | Date | Calculated | start_date + duration_months |
| notice_period_months | Integer | YES | Default by type, overridable |
| indexation_notice_days | Integer | YES | Default: 30 |
| monthly_rent | Decimal(10,2) | YES | Contractual rent |
| monthly_charges | Decimal(10,2) | YES | |
| charges_type | Enum | YES | FORFAIT / PROVISION |
| charges_description | Text | NO | |
| base_index_value | Decimal(8,4) | NO | Statbel health index at signature |
| base_index_month | Date | NO | Reference month of base index |
| indexation_anniversary_month | Integer | NO | 1–12 |
| registration_spf | String(50) | NO | SPF Finances number |
| registration_region | String(50) | NO | Region registration number |
| deposit_amount | Decimal(10,2) | NO | |
| deposit_type | Enum | NO | BLOCKED_ACCOUNT / BANK_GUARANTEE / CPAS / INSURANCE |
| deposit_reference | String(100) | NO | |
| tenant_insurance_confirmed | Boolean | NO | Default: false |
| tenant_insurance_reference | String(100) | NO | |
| tenant_insurance_expiry | Date | NO | |

### Lease Types
- `SHORT_TERM` — Bail de courte durée (< 3 ans)
- `MAIN_RESIDENCE_3Y` — Bail résidence principale 3 ans
- `MAIN_RESIDENCE_6Y` — Bail résidence principale 6 ans
- `MAIN_RESIDENCE_9Y` — Bail résidence principale 9 ans
- `STUDENT` — Bail étudiant
- `GLIDING` — Bail glissant
- `COMMERCIAL` — Bail commercial

### Lease Tenant (junction)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| lease_id | FK | YES | |
| person_id | FK | YES | |
| tenant_role | Enum | YES | PRIMARY / CO_TENANT / GUARANTOR |

### Lease Indexation History

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| lease_id | FK | YES | |
| calculation_date | Date | YES | When the calculation was done |
| application_date | Date | YES | Effective date of new rent |
| old_rent | Decimal(10,2) | YES | Rent before indexation |
| new_index_value | Decimal(8,4) | YES | Index value used |
| new_index_month | Date | YES | Month of the index |
| applied_rent | Decimal(10,2) | YES | Rent actually applied |
| notification_sent_date | Date | NO | |
| notes | Text | NO | |
| created_at | Timestamp | auto | |

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/housing-units/{unitId}/leases` | All leases for a unit |
| GET | `/api/v1/housing-units/{unitId}/leases/active` | Active lease (204 if none) |
| GET | `/api/v1/leases/{id}` | Lease details |
| POST | `/api/v1/housing-units/{unitId}/leases` | Create lease |
| PUT | `/api/v1/leases/{id}` | Update lease |
| PATCH | `/api/v1/leases/{id}/status` | Change status |
| GET | `/api/v1/leases/{id}/indexations` | Indexation history |
| POST | `/api/v1/leases/{id}/indexations` | Record indexation |
| GET | `/api/v1/leases/{id}/tenants` | List tenants |
| POST | `/api/v1/leases/{id}/tenants` | Add tenant |
| DELETE | `/api/v1/leases/{id}/tenants/{personId}` | Remove tenant |
| GET | `/api/v1/leases/alerts` | All pending alerts (indexation + end notice) |

---

## UI Requirements

### Lease Section in Housing Unit Details
- Active lease card: status badge, tenant name(s), rent, dates
- Alert banners if deadlines approaching
- "Create Lease" button (if no active/draft)
- "View History" link

### Lease Details Page
- Tabbed or sectioned layout: General / Financial / Indexation / Tenants / Guarantee
- Alert banners at top for approaching deadlines
- Status workflow buttons (Activate, Finish, Cancel)
- "Record Indexation" button (active leases only)

### Lease Form
- Multi-section form with progressive disclosure
- End date auto-computed from start date + duration
- Person picker for tenants with role selector
- Lease type selector updates default notice period automatically

### Indexation History Table
- Columns: Application Date | Old Rent | New Index | Applied Rent | Notification | Notes
- Sorted by application date DESC
- Total indexation since lease start (calculated)

### Alerts Panel (global or per unit)
- "Indexation due for Unit X — anniversary on [date]"
- "Lease ending soon for Unit Y — notice deadline [date]"

---

## Test Scenarios

| ID | Scenario | Expected |
|----|----------|----------|
| TS-UC010-01 | Create lease on unit with no existing lease | Lease created as DRAFT |
| TS-UC010-02 | Try to create lease on unit with active lease | Error: overlap detected |
| TS-UC010-03 | Activate DRAFT lease | Status changes to ACTIVE |
| TS-UC010-04 | End date is correctly calculated | end_date = start_date + 36 months |
| TS-UC010-05 | Save lease without primary tenant | Error: primary tenant required |
| TS-UC010-06 | Record indexation on active lease | New rent saved, history updated |
| TS-UC010-07 | Try to record indexation on DRAFT lease | Error: not allowed |
| TS-UC010-08 | Finish active lease | Status changes to FINISHED |
| TS-UC010-09 | Try to edit FINISHED lease | Form is read-only |
| TS-UC010-10 | Indexation alert appears near anniversary | Alert shown on unit and lease |
| TS-UC010-11 | End notice alert appears near notice deadline | Alert shown on unit and lease |
| TS-UC010-12 | Add co-tenant to active lease | Co-tenant added to lease_tenant |
| TS-UC010-13 | Remove primary tenant when only one exists | Error: at least one primary required |
| TS-UC010-14 | Lease with FORFAIT charges | Charges type stored correctly |
| TS-UC010-15 | Registration numbers optional | Lease saved without SPF/region numbers |

---

## Related User Stories

- **US049**: View lease for housing unit
- **US050**: Create lease (draft)
- **US051**: Activate lease
- **US052**: Edit lease
- **US053**: Finish lease
- **US054**: Cancel lease
- **US055**: Record indexation
- **US056**: View indexation history
- **US057**: Add tenant to lease
- **US058**: Remove tenant from lease
- **US059**: View lease alerts

---

## Notes

- Lease PDF generation is out of scope for Phase 1 (backlog)
- État des lieux is out of scope for Phase 1 (backlog)
- Payment tracking is out of scope for Phase 1 (backlog)
- The `rent_history` reference rent is displayed alongside the contractual lease rent for comparison, but they are managed independently

---

**Last Updated**: 2026-02-24
**Version**: 1.0
**Status**: 📋 Ready for Implementation
