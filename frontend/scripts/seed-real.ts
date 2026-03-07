/**
 * Real seed runner — seeds the Saint-Gilles real dataset.
 *
 * Called by seed-dev.ts when --dataset=real is passed.
 * Do NOT run directly; use: npm run seed:real
 *
 * Branch: develop
 */

import {
  adjustRent,
  changeLeaseStatus,
  createBankAccount,
  createBuilding,
  createBuildingBoiler,
  createBuildingMeter,
  createHousingUnit,
  createLease,
  createPerson,
  createRooms,
  createUnitBoiler,
  createUnitMeter,
  login,
} from "../e2e/shared/api-client";

import {
  BANK_ACCOUNTS_SEED,
  BUILDINGS,
  PERSONS,
  PUNCTUAL_TRANSACTIONS,
  REAL_BOILERS,
  REAL_EXTINGUISHERS,
  REAL_LEASES,
  REAL_METERS,
  REAL_UNITS,
} from "../e2e/shared/data-factory.real";

// ─── Fire extinguisher API helper (not yet in api-client) ─────────────────────

const BASE_URL = process.env["API_URL"] ?? "http://localhost:8080/api/v1";
let _sessionCookie: string | null = null;

function apiHeaders(): Record<string, string> {
  if (!_sessionCookie) throw new Error("Not logged in");
  return { "Content-Type": "application/json", Cookie: _sessionCookie };
}

async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: "POST",
    headers: apiHeaders(),
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`POST ${path} [${res.status}]: ${text}`);
  }
  return res.json();
}

async function createFireExtinguisher(
  buildingId: number,
  data: {
    identificationNumber: string;
    unitId: number | null;
    notes: string | null;
  },
): Promise<{ id: number }> {
  return apiPost(`/buildings/${buildingId}/fire-extinguishers`, data);
}

async function addExtinguisherRevision(
  extId: number,
  data: { revisionDate: string; notes: string | null },
): Promise<void> {
  await apiPost(`/fire-extinguishers/${extId}/revisions`, data);
}

async function addBoilerServiceRecord(
  boilerId: number,
  data: { serviceDate: string; validUntil: string; notes: string | null },
): Promise<void> {
  await apiPost(`/boilers/${boilerId}/services`, data);
}

// ─── Logger ───────────────────────────────────────────────────────────────────

function log(emoji: string, msg: string) {
  console.log(`${emoji}  ${msg}`);
}

// ─── Main ─────────────────────────────────────────────────────────────────────

export async function runReal({ dryRun }: { dryRun: boolean }) {
  console.log("\n🏘️  ImmoCare — Real Seed (Saint-Gilles)");
  console.log("=========================================");
  if (dryRun) console.log("⚠️  DRY RUN — no API calls will be made\n");

  if (!dryRun) {
    log("🔐", "Logging in as admin...");
    const cookie = await login();
    _sessionCookie = cookie;
    log("✅", "Authenticated\n");
  }

  // ── 1. Create persons ──────────────────────────────────────────────────────
  log("👥", `Creating ${PERSONS.length} persons...`);
  const personIds: number[] = [];

  for (const p of PERSONS) {
    if (dryRun) {
      log("  →", `Person: ${p.firstName} ${p.lastName}`);
      personIds.push(personIds.length + 1);
      continue;
    }
    const created = await createPerson(p);
    personIds.push(created.id);
    log("  ✓", `${p.firstName} ${p.lastName} → id ${created.id}`);
  }

  // personIds[0] = Gautier Vanderslyen (owner)
  // personIds[1..4] = tenants

  // ── 2. Create buildings + units + meters ───────────────────────────────────
  log("\n🏢", `Creating ${BUILDINGS.length} building(s)...`);

  const buildingIds: number[] = [];
  const allUnitIds: number[][] = []; // allUnitIds[buildingIndex][unitIndex]

  for (let bi = 0; bi < BUILDINGS.length; bi++) {
    const b = BUILDINGS[bi];
    const ownerId = personIds[0]; // only one owner

    if (dryRun) {
      log("  →", `Building: ${b.name} — ${b.city}`);
      buildingIds.push(bi + 1);
      allUnitIds.push(REAL_UNITS[bi].map((_, ui) => bi * 10 + ui + 1));
      continue;
    }

    const building = await createBuilding({
      name: b.name,
      streetAddress: b.streetAddress,
      postalCode: b.postalCode,
      city: b.city,
      ownerId,
    });
    buildingIds.push(building.id);
    log("  ✓", `${b.name} → id ${building.id}`);

    const unitIds: number[] = [];
    const units = REAL_UNITS[bi];

    for (let ui = 0; ui < units.length; ui++) {
      const u = units[ui];
      const unit = await createHousingUnit({ buildingId: building.id, ...u });
      unitIds.push(unit.id);
      log("    ✓", `Unit ${u.unitNumber} → id ${unit.id}`);

      // Rooms (generic for now — surface not provided, use 70m² template)
      await createRooms(unit.id, [
        { roomType: "LIVING_ROOM", approximateSurface: 20 },
        { roomType: "BEDROOM", approximateSurface: 15 },
        { roomType: "KITCHEN", approximateSurface: 10 },
        { roomType: "BATHROOM", approximateSurface: 8 },
      ]);

      // Unit meters
      const unitMeters = REAL_METERS[bi]?.units[ui] ?? [];
      for (const m of unitMeters) {
        await createUnitMeter(unit.id, m);
        log("      ✓", `Meter ${m.type} — ${m.meterNumber}`);
      }
    }
    allUnitIds.push(unitIds);

    // Building meters
    const buildingMeters = REAL_METERS[bi]?.building ?? [];
    for (const m of buildingMeters) {
      await createBuildingMeter(building.id, m);
      log("    ✓", `Building meter ${m.type} — ${m.meterNumber}`);
    }
  }

  // ── 3. Create leases ───────────────────────────────────────────────────────
  log("\n📄", `Creating ${REAL_LEASES.length} leases...`);
  let leaseCount = 0;

  interface LeaseInfo {
    leaseId: number;
    buildingIndex: number;
    unitIndex: number;
    tenantName: string;
    monthlyRent: number;
    monthlyCharges: number;
    startDate: Date;
    endDate: Date;
    active: boolean;
  }
  const leaseInfos: LeaseInfo[] = [];

  for (const tpl of REAL_LEASES) {
    const unitId = dryRun
      ? tpl.buildingIndex * 10 + tpl.unitIndex + 1
      : allUnitIds[tpl.buildingIndex][tpl.unitIndex];
    const tenantPersonId = dryRun
      ? tpl.tenantPersonIndex + 1
      : personIds[tpl.tenantPersonIndex];
    const tenant = PERSONS[tpl.tenantPersonIndex];

    if (dryRun) {
      log(
        "  →",
        `Lease [${tpl.scenario}] ${tenant.firstName} ${tenant.lastName} — unit ${unitId} — ${tpl.baseRent}€/mo`,
      );
      leaseInfos.push({
        leaseId: leaseCount + 1,
        buildingIndex: tpl.buildingIndex,
        unitIndex: tpl.unitIndex,
        tenantName: `${tenant.firstName} ${tenant.lastName}`,
        monthlyRent: tpl.baseRent,
        monthlyCharges: tpl.charges,
        startDate: new Date(tpl.startDate),
        endDate: new Date(tpl.endDate),
        active:
          tpl.scenario === "active" ||
          tpl.scenario === "active-with-adjustments",
      });
      leaseCount++;
      continue;
    }

    try {
      const lease = await createLease(
        {
          housingUnitId: unitId,
          signatureDate: tpl.signatureDate,
          startDate: tpl.startDate,
          endDate: tpl.endDate,
          leaseType: tpl.leaseType,
          durationMonths: tpl.durationMonths,
          noticePeriodMonths: tpl.noticePeriodMonths,
          monthlyRent: tpl.baseRent,
          monthlyCharges: tpl.charges,
          chargesType: tpl.chargesType,
          depositAmount: tpl.baseRent * tpl.depositMultiplier,
          depositType: "BANK_GUARANTEE",
          tenantInsuranceConfirmed: true,
          tenants: [{ personId: tenantPersonId, role: "PRIMARY" }],
        },
        false,
      );
      log("  ✓", `Lease ${lease.id} — ${tenant.firstName} ${tenant.lastName}`);

      // Rent adjustments
      if (tpl.rentAdjustments && tpl.rentAdjustments.length > 0) {
        // Must activate first to allow adjustments
        await changeLeaseStatus(lease.id, "ACTIVE");
        for (const adj of tpl.rentAdjustments) {
          await adjustRent(
            lease.id,
            "RENT",
            adj.newRent,
            adj.reason,
            adj.effectiveDate,
          );
          log("    ✓", `Rent → ${adj.newRent}€ on ${adj.effectiveDate}`);
        }
        if (tpl.scenario === "finished-with-adjustments") {
          await changeLeaseStatus(lease.id, "FINISHED");
          log("    ✓", `Status → FINISHED`);
        }
      } else {
        // Status transitions for leases without adjustments
        if (tpl.scenario === "finished") {
          await changeLeaseStatus(lease.id, "ACTIVE");
          await changeLeaseStatus(lease.id, "FINISHED");
          log("    ✓", `Status → ACTIVE → FINISHED`);
        } else if (
          tpl.scenario === "active" ||
          tpl.scenario === "active-with-adjustments"
        ) {
          await changeLeaseStatus(lease.id, "ACTIVE");
          log("    ✓", `Status → ACTIVE`);
        }
        // "draft" stays as-is
      }

      leaseInfos.push({
        leaseId: lease.id,
        buildingIndex: tpl.buildingIndex,
        unitIndex: tpl.unitIndex,
        tenantName: `${tenant.firstName} ${tenant.lastName}`,
        monthlyRent: tpl.baseRent,
        monthlyCharges: tpl.charges,
        startDate: new Date(tpl.startDate),
        endDate: new Date(tpl.endDate),
        active:
          tpl.scenario === "active" ||
          tpl.scenario === "active-with-adjustments",
      });
      leaseCount++;
    } catch (err) {
      log(
        "  ❌",
        `Lease ${tenant.firstName} ${tenant.lastName} failed: ${(err as Error).message}`,
      );
    }
  }

  // ── 4. Create bank accounts ────────────────────────────────────────────────
  log("\n🏦", `Creating ${BANK_ACCOUNTS_SEED.length} bank account(s)...`);
  const bankAccountIds: number[] = [];

  for (const ba of BANK_ACCOUNTS_SEED) {
    if (dryRun) {
      log("  →", `Bank account: ${ba.label}`);
      bankAccountIds.push(bankAccountIds.length + 1);
      continue;
    }
    try {
      const created = await createBankAccount(ba);
      bankAccountIds.push(created.id);
      log("  ✓", `${ba.label} → id ${created.id}`);
    } catch (err) {
      log("  ⚠", `${ba.label} skipped: ${(err as Error).message}`);
      bankAccountIds.push(0);
    }
  }

  // ── 5. Create boilers ──────────────────────────────────────────────────────
  log("\n🔥", `Creating ${REAL_BOILERS.length} boiler(s)...`);

  for (const boiler of REAL_BOILERS) {
    if (dryRun) {
      const loc =
        boiler.unitIndex !== null
          ? `unit ${REAL_UNITS[boiler.buildingIndex][boiler.unitIndex].unitNumber}`
          : `building ${BUILDINGS[boiler.buildingIndex].name} (common)`;
      log(
        "  →",
        `Boiler ${boiler.brand} — ${loc} — ${boiler.services.length} service(s)`,
      );
      continue;
    }

    try {
      const buildingId = buildingIds[boiler.buildingIndex];
      const boilerPayload = {
        fuelType: boiler.fuelType,
        installationDate: boiler.installationDate,
        brand: boiler.brand !== "Unknown" ? boiler.brand : undefined,
        model: boiler.model ?? undefined,
        serialNumber: boiler.serialNumber ?? undefined,
      };

      let createdBoilerId: number;

      if (boiler.unitIndex !== null) {
        const unitId = allUnitIds[boiler.buildingIndex][boiler.unitIndex];
        const created = await createUnitBoiler(unitId, boilerPayload);
        createdBoilerId = created.id;
        log(
          "  ✓",
          `Unit boiler → id ${created.id} (unit ${REAL_UNITS[boiler.buildingIndex][boiler.unitIndex].unitNumber})`,
        );
      } else {
        const created = await createBuildingBoiler(buildingId, boilerPayload);
        createdBoilerId = created.id;
        log(
          "  ✓",
          `Building boiler → id ${created.id} (${boiler.notes ?? "common area"})`,
        );
      }

      // Add service records
      for (const svc of boiler.services) {
        await addBoilerServiceRecord(createdBoilerId, {
          serviceDate: svc.serviceDate,
          validUntil: svc.validUntil,
          notes: svc.notes,
        });
        log(
          "    ✓",
          `Service ${svc.serviceDate} → valid until ${svc.validUntil}`,
        );
      }
    } catch (err) {
      log("  ❌", `Boiler failed: ${(err as Error).message}`);
    }
  }

  // ── 6. Create fire extinguishers ───────────────────────────────────────────
  log("\n🧯", `Creating ${REAL_EXTINGUISHERS.length} fire extinguisher(s)...`);

  for (const ext of REAL_EXTINGUISHERS) {
    if (dryRun) {
      log(
        "  →",
        `Extinguisher ${ext.identificationNumber} — ${ext.notes ?? "common area"} — ${ext.revisions.length} revision(s)`,
      );
      continue;
    }

    try {
      const buildingId = buildingIds[ext.buildingIndex];
      const unitId =
        ext.unitIndex !== null
          ? allUnitIds[ext.buildingIndex][ext.unitIndex]
          : null;

      const created = await createFireExtinguisher(buildingId, {
        identificationNumber: ext.identificationNumber,
        unitId,
        notes: ext.notes,
      });
      log("  ✓", `${ext.identificationNumber} → id ${created.id}`);

      for (const rev of ext.revisions) {
        await addExtinguisherRevision(created.id, {
          revisionDate: rev.revisionDate,
          notes: rev.notes,
        });
        log("    ✓", `Revision ${rev.revisionDate}`);
      }
    } catch (err) {
      log(
        "  ❌",
        `Extinguisher ${ext.identificationNumber} failed: ${(err as Error).message}`,
      );
    }
  }

  // ── 7. Punctual transactions ───────────────────────────────────────────────
  if (PUNCTUAL_TRANSACTIONS.length > 0) {
    log("\n💳", `Creating ${PUNCTUAL_TRANSACTIONS.length} transaction(s)...`);
    // TODO: implement if/when real transactions are added to data-factory.real.ts
  }

  // ── Summary ────────────────────────────────────────────────────────────────
  console.log("\n=========================================");
  log("🎉", `Real seed complete!`);
  log("📦", `Dataset      : real (Saint-Gilles)`);
  log("👥", `Persons      : ${PERSONS.length}`);
  log("🏢", `Buildings    : ${BUILDINGS.length}`);
  log(
    "🚪",
    `Units        : ${allUnitIds.flat().length || REAL_UNITS.flat().length}`,
  );
  log("📄", `Leases       : ${leaseCount}`);
  log("🏦", `Bank accounts: ${BANK_ACCOUNTS_SEED.length}`);
  log("🔥", `Boilers      : ${REAL_BOILERS.length}`);
  log("🧯", `Extinguishers: ${REAL_EXTINGUISHERS.length}`);
  if (dryRun) log("⚠️", "DRY RUN — nothing was actually created");
  console.log("=========================================\n");
}
