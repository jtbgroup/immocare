---
title: Equipment Rules
type: business rules
status: draft
last_verified: 2026-11-07
verified_against: "Boiler, BoilerServiceRecord, BoilerServiceValidityRule, FireExtinguisher, FireExtinguisherRevision, Meter, BoilerService, BoilerServiceHistoryService, BoilerServiceValidityRuleService, FireExtinguisherService, MeterService, AlertService, BoilerController, BoilerServiceController, FireExtinguisherController, MeterController, EstateService, AlertController, BoilerRepository, FireExtinguisherRepository, EstateConfigDTOs, SaveBoilerRequest, SaveFireExtinguisherRequest, AddRevisionRequest, AddMeterRequest, ReplaceMeterRequest, RemoveMeterRequest, V009__uc009_manage_meters.sql, V011__uc011_manage_boilers.sql, V012__uc012_manage_fire_extinguishers.sql, V017__generalize_fire_extinguisher_ownership.sql"
---

# Equipment Rules

## Rules

| ID | Rule | Status |
|---|---|---|
| BR-EQ-001 | Any supported equipment type can be associated with either a building or a housing unit. Equipment type must not restrict the kind of property to which it can be associated. | Confirmed |
| BR-EQ-002 | A meter's type is WATER, GAS, or ELECTRICITY. | Needs business validation |
| BR-EQ-003 | A meter's start date is not in the future. | Needs business validation |
| BR-EQ-004 | A GAS or ELECTRICITY meter has an EAN code. | Needs business validation |
| BR-EQ-005 | A WATER meter has an installation number. | Needs business validation |
| BR-EQ-006 | A WATER meter associated with a building has a customer number. | Needs business validation |
| BR-EQ-007 | A meter's end date is not before its start date or after today. | Needs business validation |
| BR-EQ-008 | Replacing a meter sets the existing meter's end date to the replacement's start date and creates the replacement in the same operation. | Needs business validation |
| BR-EQ-009 | A replacement meter's start date is not before the replaced meter's start date. | Needs business validation |
| BR-EQ-010 | A meter is active when it has no end date and closed when it has an end date. | Needs business validation |
| BR-EQ-011 | One building or housing unit may have multiple active meters of the same type. | Needs business validation |
| BR-EQ-012 | Other than closing, meter details are not edited after creation; closing a meter sets its end date. | Needs business validation |
| BR-EQ-013 | A meter has a nonblank meter number. | Needs business validation |
| BR-EQ-014 | A boiler's fuel type is GAS, OIL, ELECTRIC, or HEAT_PUMP. | Needs business validation |
| BR-EQ-015 | A boiler has an installation date. | Needs business validation |
| BR-EQ-016 | A boiler's last service date, when present, is not in the future or before its installation date. | Needs business validation |
| BR-EQ-017 | A boiler's next service date, when present, is not before its installation date. | Needs business validation |
| BR-EQ-018 | When both dates are present, a boiler's last service date is not before its out-of-service date. | Needs clarification |
| BR-EQ-019 | A boiler service date is not in the future. | Needs business validation |
| BR-EQ-020 | When no validity end date is supplied for a boiler service record, the application calculates it using the latest estate validity rule effective on the service date, or a 24-month duration if no rule applies. | Needs business validation |
| BR-EQ-021 | A supplied validity end date overrides the calculated boiler service validity end date. | Needs business validation |
| BR-EQ-022 | Boiler service warning status uses the estate's configured warning period, defaulting to three months. | Needs business validation |
| BR-EQ-023 | A fire extinguisher's identification number is nonblank and no longer than 50 characters. | Needs business validation |
| BR-EQ-024 | A fire-extinguisher identification number is unique, ignoring case, among extinguishers with the same direct building or housing-unit owner. | Needs clarification |
| BR-EQ-025 | A fire-extinguisher revision date is not in the future. | Needs business validation |
| BR-EQ-026 | A fire extinguisher is associated with exactly one management context: a building or a housing unit. | Needs clarification |
| BR-EQ-027 | Deleting a fire extinguisher also deletes its revision records. | Needs business validation |
| BR-EQ-028 | An individual fire-extinguisher revision record can be deleted. | Needs clarification |
| BR-EQ-029 | A boiler may be recorded with an installation date later than today. | Needs clarification |
| BR-EQ-030 | A boiler may be recorded without a brand. | Needs clarification |
| BR-EQ-031 | The estate dashboard counts a fire-extinguisher alert when no revision exists or the latest revision is more than one year old. | Needs clarification |
| BR-EQ-032 | Each equipment item must retain its relevant lifecycle history, and items subject to a validity or maintenance interval must generate alerts as that interval approaches or expires. | Confirmed |

BR-EQ-001 defines the required cross-type association capability. It does not prescribe how the application stores the association or how equipment-specific behavior is implemented.

The user explicitly confirmed BR-EQ-001 during the domain-structure discussion; the exact confirmation date was not retained.

The user confirmed BR-EQ-032 on 2026-11-07.

## Type-specific behavior

Equipment types may have different attributes, lifecycle rules, maintenance or inspection histories, and alerts. The shared ability to associate equipment with a building or housing unit does not imply that every type must support identical behavior.

See [Property Equipment and Assets](../concepts/property-equipment.md) for the types currently represented in the application.

Rules BR-EQ-001 and BR-EQ-032 are stakeholder-confirmed. Rules BR-EQ-002 through BR-EQ-017, BR-EQ-019 through BR-EQ-023, BR-EQ-025, and BR-EQ-027 are inferred from the implementation and remain subject to business validation. BR-EQ-018, BR-EQ-024, BR-EQ-026, and BR-EQ-028 through BR-EQ-031 reflect behavior that conflicts with or is not settled by the analysis and need clarification. The meter replacement-reason field is accepted by the current request but is not persisted; the intended retention of that reason remains an open question.

## Observed sources

`Boiler`, `BoilerServiceRecord`, `BoilerServiceValidityRule`, `BoilerService`, `BoilerServiceHistoryService`, `BoilerServiceValidityRuleService`, `BoilerController`, `BoilerServiceController`, `SaveBoilerRequest`, `FireExtinguisher`, `FireExtinguisherRevision`, `FireExtinguisherService`, `FireExtinguisherController`, `FireExtinguisherRepository`, `SaveFireExtinguisherRequest`, `AddRevisionRequest`, `Meter`, `MeterService`, `MeterController`, `MeterRepository`, `AlertService`, `AlertController`, `EstateService`, meter and estate-configuration request DTOs, and migrations `V009__uc009_manage_meters.sql`, `V011__uc011_manage_boilers.sql`, `V012__uc012_manage_fire_extinguishers.sql`, and `V017__generalize_fire_extinguisher_ownership.sql`.
