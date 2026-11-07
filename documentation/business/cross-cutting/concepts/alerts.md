---
title: Alerts
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: "AlertService, PebScoreRepository, EstateConfigDTOs, LeaseService, BoilerService"
---

# Concept — Alerts

Alerting is cross-cutting: an alert may arise from different business domains, including equipment, lease events, or energy-performance (PEB) information. The current alert list includes boiler service, lease, and PEB certificate expiry alerts. This shared classification does not imply that every possible alert type is implemented or define a common trigger, recipient, or lifecycle.

PEB certificate expiry criteria and their estate-specific warning period are described in [PEB Score Management](../../../technical/buildings-and-housing-units/peb-score-management.md). The estate dashboard's pending-alert counts are documented in [Estate Management](../../../technical/estates-and-access/estate-management.md). Domain-specific alert criteria belong with the relevant domain; shared alert presentation and lifecycle belong in cross-cutting documentation.
