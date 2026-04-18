/**
 * Demo seed runner — seeds a small demo dataset for local development.
 *
 * Called by seed-dev.ts when --dataset=demo is passed.
 * Do NOT run directly; use: npm run seed:dev
 *
 * Branch: develop
 */

import {
  adjustRent,
  changeLeaseStatus,
  createBankAccount,
  createBuilding,
  createEstate,
  createHousingUnit,
  createLease,
  createPerson,
  createRooms,
  createUnitMeter,
  login,
} from "../e2e/shared/api-client";

import {
  BUILDINGS,
  ESTATES,
  generateRooms,
  generateUnitMeters,
  generateUnits,
  LEASE_TEMPLATES,
  PERSONS,
  RENT_ADJUSTMENT_REASONS,
} from "../e2e/shared/data-factory";

function log(emoji: string, msg: string) {
  console.log(`${emoji}  ${msg}`);
}

export async function runDemo({ dryRun }: { dryRun: boolean }) {
  console.log("\n🏘️  ImmoCare — Demo Seed");
  console.log("=========================================");
  if (dryRun) console.log("⚠️  DRY RUN — no API calls will be made\n");

  if (!dryRun) {
    log("🔐", "Logging in as admin...");
    await login();
    log("✅", "Authenticated\n");
  }

  // ── 0. Create estates ──────────────────────────────────────────────────────
  log("🏘️ ", `Creating ${ESTATES.length} estates...`);
  const estateIds: string[] = [];

  for (const e of ESTATES) {
    if (dryRun) {
      log("  →", `Estate: ${e.name}`);
      estateIds.push(`estate-${estateIds.length + 1}`);
      continue;
    }
    const created = await createEstate(e);
    estateIds.push(created.id);
    log("  ✓", `${e.name} → id ${created.id}`);
  }

  // ── 1. Create persons ──────────────────────────────────────────────────────
  log("👥", `Creating ${Math.min(PERSONS.length, 8)} persons...`);
  const personIds: number[] = [];
  const personsToCreate = PERSONS.slice(0, 8);

  for (const p of personsToCreate) {
    if (dryRun) {
      log("  →", `Person: ${p.firstName} ${p.lastName}`);
      personIds.push(personIds.length + 1);
      continue;
    }
    const created = await createPerson(p);
    personIds.push(created.id);
    log("  ✓", `${p.firstName} ${p.lastName} → id ${created.id}`);
  }

  // ── 2. Create one building + units ────────────────────────────────────────
  const building = BUILDINGS[0];
  log("\n🏢", `Creating building: ${building.name} (${building.city})...`);
  const buildingId = dryRun
    ? 1
    : (await createBuilding({ ...building, ownerId: personIds[0] })).id;

  const units = generateUnits(0);
  const unitIds: number[] = [];

  for (let ui = 0; ui < units.length; ui++) {
    const u = units[ui];
    if (dryRun) {
      log("  →", `Unit ${u.unitNumber} (floor ${u.floor})`);
      unitIds.push(ui + 1);
      continue;
    }

    const createdUnit = await createHousingUnit({
      buildingId,
      ...u,
      ownerId: personIds[0],
    });
    unitIds.push(createdUnit.id);
    log("  ✓", `Unit ${u.unitNumber} → id ${createdUnit.id}`);

    await createRooms(createdUnit.id, generateRooms(u.totalSurface));

    const meters = generateUnitMeters(createdUnit.id, ui);
    for (const m of meters) {
      await createUnitMeter(createdUnit.id, m);
      log("    ✓", `Meter ${m.type} — ${m.meterNumber}`);
    }
  }

  // ── 3. Create one lease (active) ──────────────────────────────────────────
  const tpl = LEASE_TEMPLATES[0];
  const tenantId = personIds[1] ?? personIds[0];
  const signatureDate = new Date();
  const startDate = new Date(signatureDate);
  startDate.setMonth(startDate.getMonth() - 2);
  const endDate = new Date(startDate);
  endDate.setMonth(endDate.getMonth() + tpl.durationMonths);

  log("\n📄", "Creating a demo lease...");

  if (dryRun) {
    log(
      "  →",
      `Lease demo — ${tpl.leaseType} (${tpl.baseRent}€/mo) for ${PERSONS[1].firstName} ${PERSONS[1].lastName}`,
    );
  } else {
    const lease = await createLease(
      {
        housingUnitId: unitIds[0],
        signatureDate: signatureDate.toISOString().slice(0, 10),
        startDate: startDate.toISOString().slice(0, 10),
        endDate: endDate.toISOString().slice(0, 10),
        leaseType: tpl.leaseType,
        durationMonths: tpl.durationMonths,
        noticePeriodMonths: tpl.noticePeriodMonths,
        monthlyRent: tpl.baseRent,
        monthlyCharges: tpl.charges,
        chargesType: tpl.chargesType,
        depositAmount: tpl.baseRent * tpl.depositMultiplier,
        depositType: "BANK_GUARANTEE",
        tenantInsuranceConfirmed: true,
        tenants: [{ personId: tenantId, role: "PRIMARY" }],
      },
      false,
    );
    log("  ✓", `Lease ${lease.id} created`);

    await changeLeaseStatus(lease.id, "ACTIVE");
    log("    ✓", `Status → ACTIVE`);

    // Simple rent adjustment example
    await adjustRent(
      lease.id,
      "RENT",
      tpl.baseRent + 50,
      RENT_ADJUSTMENT_REASONS[0],
      new Date(startDate.getTime() + 30 * 24 * 3600 * 1000)
        .toISOString()
        .slice(0, 10),
    );
    log("    ✓", `Rent adjusted (+50€)`);
  }

  // ── 4. Create a demo bank account ─────────────────────────────────────────
  log("\n🏦", "Creating a demo bank account...");
  if (dryRun) {
    log("  →", "Bank account: Demo current account");
  } else {
    await createBankAccount({
      label: "Demo current account",
      accountNumber: "BE12 3456 7890 1234",
      type: "CURRENT",
      isActive: true,
    });
    log("  ✓", "Bank account created");
  }

  log("\n✅", "Demo seed finished.");
}

export {}; // make this file an ES module so top-level await is allowed
