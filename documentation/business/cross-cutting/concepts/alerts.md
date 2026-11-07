---
title: Alerts
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: "AlertService, AlertController, PebScoreRepository, EstateConfigDTOs, LeaseService, LeaseRentAdjustmentRepository, BoilerService, EstateService"
---

# Concept — Alerts

Alerting is cross-cutting: an alert may arise from different business domains, including equipment, lease events, or energy-performance (PEB) information. Confirmed equipment policy requires alerts when a validity or maintenance interval approaches or expires ([BR-EQ-032](../../equipment/rules/equipment.md)). The current shared alert list includes boiler service, lease indexation, and PEB certificate expiry alerts. Lease indexation alerts relate to lease records; the list does not generate alerts from the separate housing-unit rent history. The estate dashboard separately counts lease-end and indexation alerts, as well as fire-extinguisher revisions that are missing or more than one year old; this fixed extinguisher threshold needs business clarification under [BR-EQ-031](../../equipment/rules/equipment.md). Meters currently have no validity or maintenance alerts. The confirmed policy does not imply that every alert type is already implemented or define its type-specific trigger, warning period, recipient, or lifecycle.

Boiler service warning periods are configured per estate; boiler-specific service dates, history, and the contextual-alert endpoint are described in [Boiler Management](../../../technical/equipment/boiler-management.md). PEB certificate expiry criteria and their estate-specific warning period are described in [PEB Score Management](../../../technical/buildings-and-housing-units/peb-score-management.md). The estate dashboard's pending-alert counts are documented in [Estate Management](../../../technical/estates-and-access/estate-management.md). Domain-specific alert criteria belong with the relevant domain; shared alert presentation and lifecycle belong in cross-cutting documentation.
