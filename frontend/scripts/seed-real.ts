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
  createPersonBankAccount,
  createRooms,
  createTagCategory,
  createTagSubcategory,
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

// ─── Tag categories & subcategories ──────────────────────────────────────────
// Source of truth for all transaction category labels.
// Add / remove entries here — they will be created on next seed run.

const TAG_CATEGORIES: Array<{
  name: string;
  subcategories: Array<{ name: string; direction: "INCOME" | "EXPENSE" | "BOTH" }>;
}> = [
  {
    name: "Administration",
    subcategories: [
      { name: "Assurance habitation", direction: "EXPENSE" },
      { name: "PEB",                  direction: "EXPENSE" },
      { name: "Petits frais",         direction: "BOTH"    },
      { name: "Syndic",               direction: "EXPENSE" },
    ],
  },
  {
    name: "Consommables",
    subcategories: [
      { name: "Adoucisseur", direction: "EXPENSE" },
      { name: "Eau",         direction: "EXPENSE" },
    ],
  },
  {
    name: "Dépôt",
    subcategories: [
      { name: "Dépôt de garantie", direction: "INCOME" },
    ],
  },
  {
    name: "Location",
    subcategories: [
      { name: "Loyer",   direction: "INCOME" },
      { name: "Charges", direction: "INCOME" },
    ],
  },
  {
    name: "Maintenance",
    subcategories: [
      { name: "Chaudière",  direction: "EXPENSE" },
      { name: "Extincteur", direction: "EXPENSE" },
      { name: "Divers",     direction: "EXPENSE" },
    ],
  },
  {
    name: "Prime",
    subcategories: [
      { name: "Prime rénovation", direction: "INCOME" },
    ],
  },
  {
    name: "Rente",
    subcategories: [
      { name: "Rente foncière", direction: "EXPENSE" },
    ],
  },
  {
    name: "Taxes",
    subcategories: [
      { name: "Précompte immobilier", direction: "EXPENSE" },
      { name: "Taxe communale",       direction: "EXPENSE" },
    ],
  },
  {
    name: "Travaux",
    subcategories: [
      { name: "Rénovation", direction: "EXPENSE" },
      { name: "Entretien",  direction: "EXPENSE" },
    ],
  },
];

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
  data: { identificationNumber: string; unitId: number | null; notes: string | null },
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
  const totalSubcategories = TAG_CATEGORIES.reduce((n, c) => n + c.subcategories.length, 0);

  console.log("\n🏘️  ImmoCare — Real Seed (Saint-Gilles)");
  console.log("=========================================");
  if (dryRun) console.log("⚠️  DRY RUN — no API calls will be made\n");

  if (!dryRun) {
    log("🔐", "Logging in as admin...");
    const cookie = await login();
    _sessionCookie = cookie;
    log("✅", "Authenticated\n");
  }

  // ── 1. Tag categories & subcategories ──────────────────────────────────────
  log("🏷️", `Creating ${TAG_CATEGORIES.length} categories / ${totalSubcategories} subcategories...`);

  for (const cat of TAG_CATEGORIES) {
    if (dryRun) {
      log("  →", `Category: ${cat.name} (${cat.subcategories.length} subcategories)`);
      continue;
    }
    try {
      const created = await createTagCategory(cat.name);
      log("  ✓", `${cat.name} → id ${created.id}`);
      for (const sub of cat.subcategories) {
        await createTagSubcategory(created.id, sub.name, sub.direction);
        log("    ✓", `${sub.name} (${sub.direction})`);
      }
    } catch (err) {
      log("  ⚠", `${cat.name} skipped: ${(err as Error).message}`);
    }
  }

  // ── 2. Create persons ──────────────────────────────────────────────────────
  log("\n👥", `Creating ${PERSONS.length} persons...`);
  const personIds: number[] = [];

  for (const p of PERSONS) {
    if (dryRun) {
      log("  →", `Person: ${p.firstName} ${p.lastName}`);
      personIds.push(personIds.length + 1);
      continue;
    }

    const { bankAccounts, phone, ...personData } = p as any;
    const created = await createPerson({ ...personData, gsm: phone });
    personIds.push(created.id);
    log("  ✓", `${p.firstName} ${p.lastName} → id ${created.id}`);

    if (bankAccounts?.length) {
      for (const ba of bankAccounts) {
        await createPersonBankAccount(created.id, { iban: ba.iban, label: ba.label });
        log("    ✓", `IBAN ${ba.iban}`);
      }
    }
  }

  // ── 3. Create buildings + units + meters ───────────────────────────────────
  log("\n🏢", `Creating ${BUILDINGS.length} building(s)...`);

  const buildingIds: number[] = [];
  const allUnitIds: number[][] = [];

  for (let bi = 0; bi < BUILDINGS.length; bi++) {
    const b = BUILDINGS[bi];
    const ownerId = personIds[0];

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
    for (let ui = 0; ui < REAL_UNITS[bi].length; ui++) {
      const u = REAL_UNITS[bi][ui];
      const unit = await createHousingUnit({ buildingId: building.id, ...u });
      unitIds.push(unit.id);
      log("    ✓", `Unit ${u.unitNumber} → id ${unit.id}`);

      await createRooms(unit.id, [
        { roomType: "LIVING_ROOM", approximateSurface: 20 },
        { roomType: "BEDROOM",     approximateSurface: 15 },
        { roomType: "KITCHEN",     approximateSurface: 10 },
        { roomType: "BATHROOM",    approximateSurface: 8  },
      ]);

      for (const m of REAL_METERS[bi]?.units[ui] ?? []) {
        await createUnitMeter(unit.id, m);
        log("      ✓", `Meter ${m.type} — ${m.meterNumber}`);
      }
    }
    allUnitIds.push(unitIds);

    for (const m of REAL_METERS[bi]?.building ?? []) {
      await createBuildingMeter(building.id, m);
      log("    ✓", `Building meter ${m.type} — ${m.meterNumber}`);
    }
  }

  // ── 4. Create leases ───────────────────────────────────────────────────────
  log("\n📄", `Creating ${REAL_LEASES.length} leases...`);
  let leaseCount = 0;

  for (const tpl of REAL_LEASES) {
    const unitId = dryRun
      ? tpl.buildingIndex * 10 + tpl.unitIndex + 1
      : allUnitIds[tpl.buildingIndex][tpl.unitIndex];
    const tenantPersonId = dryRun
      ? tpl.tenantPersonIndex + 1
      : personIds[tpl.tenantPersonIndex];
    const tenant = PERSONS[tpl.tenantPersonIndex];

    const tenants: { personId: number; role: "PRIMARY" | "CO_TENANT" }[] = [
      { personId: tenantPersonId, role: "PRIMARY" },
    ];
    if (tpl.coTenantPersonIndices?.length) {
      for (const coIdx of tpl.coTenantPersonIndices) {
        tenants.push({
          personId: dryRun ? coIdx + 1 : personIds[coIdx],
          role: "CO_TENANT",
        });
      }
    }

    if (dryRun) {
      const coNames = (tpl.coTenantPersonIndices ?? [])
        .map((i) => `${PERSONS[i].firstName} ${PERSONS[i].lastName}`)
        .join(", ");
      log("  →", `Lease [${tpl.scenario}] ${tenant.firstName} ${tenant.lastName}${coNames ? ` + ${coNames}` : ""} — unit ${unitId} — ${tpl.baseRent}€/mo`);
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
          tenants,
        },
        false,
      );

      const coNames = (tpl.coTenantPersonIndices ?? [])
        .map((i) => `${PERSONS[i].firstName} ${PERSONS[i].lastName}`)
        .join(", ");
      log("  ✓", `Lease ${lease.id} — ${tenant.firstName} ${tenant.lastName}${coNames ? ` + ${coNames}` : ""}`);

      if (tpl.rentAdjustments?.length) {
        await changeLeaseStatus(lease.id, "ACTIVE");
        for (const adj of tpl.rentAdjustments) {
          await adjustRent(lease.id, "RENT", adj.newRent, adj.reason, adj.effectiveDate);
          log("    ✓", `Rent → ${adj.newRent}€ on ${adj.effectiveDate}`);
        }
        if (tpl.scenario === "finished-with-adjustments") {
          await changeLeaseStatus(lease.id, "FINISHED");
          log("    ✓", `Status → FINISHED`);
        }
      } else {
        if (tpl.scenario === "finished") {
          await changeLeaseStatus(lease.id, "ACTIVE");
          await changeLeaseStatus(lease.id, "FINISHED");
          log("    ✓", `Status → ACTIVE → FINISHED`);
        } else if (tpl.scenario === "active" || tpl.scenario === "active-with-adjustments") {
          await changeLeaseStatus(lease.id, "ACTIVE");
          log("    ✓", `Status → ACTIVE`);
        }
      }

      leaseCount++;
    } catch (err) {
      log("  ❌", `Lease ${tenant.firstName} ${tenant.lastName} failed: ${(err as Error).message}`);
    }
  }

  // ── 5. Create bank accounts ────────────────────────────────────────────────
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

  // ── 6. Create boilers ──────────────────────────────────────────────────────
  log("\n🔥", `Creating ${REAL_BOILERS.length} boiler(s)...`);

  for (const boiler of REAL_BOILERS) {
    if (dryRun) {
      const loc = boiler.unitIndex !== null
        ? `unit ${REAL_UNITS[boiler.buildingIndex][boiler.unitIndex].unitNumber}`
        : `building ${BUILDINGS[boiler.buildingIndex].name} (${boiler.notes ?? "common area"})`;
      log("  →", `Boiler ${boiler.brand} — ${loc} — ${boiler.services.length} service(s)`);
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
        notes: boiler.notes ?? undefined,
      };

      let createdBoilerId: number;
      if (boiler.unitIndex !== null) {
        const unitId = allUnitIds[boiler.buildingIndex][boiler.unitIndex];
        const created = await createUnitBoiler(unitId, boilerPayload);
        createdBoilerId = created.id;
        log("  ✓", `Unit boiler → id ${created.id} (unit ${REAL_UNITS[boiler.buildingIndex][boiler.unitIndex].unitNumber})`);
      } else {
        const created = await createBuildingBoiler(buildingId, boilerPayload);
        createdBoilerId = created.id;
        log("  ✓", `Building boiler → id ${created.id} (${boiler.notes ?? "common area"})`);
      }

      for (const svc of boiler.services) {
        await addBoilerServiceRecord(createdBoilerId, {
          serviceDate: svc.serviceDate,
          validUntil: svc.validUntil,
          notes: svc.notes,
        });
        log("    ✓", `Service ${svc.serviceDate} → valid until ${svc.validUntil}`);
      }
    } catch (err) {
      log("  ❌", `Boiler failed: ${(err as Error).message}`);
    }
  }

  // ── 7. Create fire extinguishers ───────────────────────────────────────────
  log("\n🧯", `Creating ${REAL_EXTINGUISHERS.length} fire extinguisher(s)...`);

  for (const ext of REAL_EXTINGUISHERS) {
    if (dryRun) {
      log("  →", `Extinguisher ${ext.identificationNumber} — ${ext.notes ?? "common area"} — ${ext.revisions.length} revision(s)`);
      continue;
    }

    try {
      const buildingId = buildingIds[ext.buildingIndex];
      const unitId = ext.unitIndex !== null
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
      log("  ❌", `Extinguisher ${ext.identificationNumber} failed: ${(err as Error).message}`);
    }
  }

  // ── 8. Punctual transactions ───────────────────────────────────────────────
  if (PUNCTUAL_TRANSACTIONS.length > 0) {
    log("\n💳", `Creating ${PUNCTUAL_TRANSACTIONS.length} transaction(s)...`);
    // TODO: implement when real transactions are added to data-factory.real.ts
  }

  // ── Summary ────────────────────────────────────────────────────────────────
  console.log("\n=========================================");
  log("🎉", `Real seed complete!`);
  log("📦", `Dataset      : real (Saint-Gilles)`);
  log("🏷️", `Categories   : ${TAG_CATEGORIES.length} / ${totalSubcategories} subcategories`);
  log("👥", `Persons      : ${PERSONS.length}`);
  log("🏢", `Buildings    : ${BUILDINGS.length}`);
  log("🚪", `Units        : ${allUnitIds.flat().length || REAL_UNITS.flat().length}`);
  log("📄", `Leases       : ${leaseCount}`);
  log("🏦", `Bank accounts: ${BANK_ACCOUNTS_SEED.length}`);
  log("🔥", `Boilers      : ${REAL_BOILERS.length}`);
  log("🧯", `Extinguishers: ${REAL_EXTINGUISHERS.length}`);
  if (dryRun) log("⚠️", "DRY RUN — nothing was actually created");
  console.log("=========================================\n");
}
