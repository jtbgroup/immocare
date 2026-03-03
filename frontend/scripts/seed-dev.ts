/**
 * Dev seed script — fills the development database with realistic data.
 *
 * Usage:
 *   npm run seed:dev              → adds data to the dev DB
 *   npm run seed:dev -- --dry-run → prints what would be created, no API calls
 *
 * Target: http://localhost:8080/api/v1 (override with API_URL env var)
 *
 * The script is idempotent-friendly: it logs every created resource.
 * Run against a fresh DB for best results.
 */

import {
  adjustRent,
  changeLeaseStatus,
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
  BUILDINGS,
  LEASE_TEMPLATES,
  PERSONS,
  RENT_ADJUSTMENT_REASONS,
  addMonths,
  generateBoiler,
  generateBuildingMeters,
  generateRooms,
  generateUnitMeters,
  generateUnits,
  isoDate,
  yearsAgo,
} from "../e2e/shared/data-factory";

const DRY_RUN = process.argv.includes("--dry-run");

// ─── Logger ───────────────────────────────────────────────────────────────────

function log(emoji: string, msg: string) {
  console.log(`${emoji}  ${msg}`);
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log("\n🏘️  ImmoCare — Dev Seed Script");
  console.log("================================");
  if (DRY_RUN) console.log("⚠️  DRY RUN — no API calls will be made\n");

  if (!DRY_RUN) {
    log("🔐", "Logging in as admin...");
    await login();
    log("✅", "Authenticated\n");
  }

  // ── 1. Create persons ──────────────────────────────────────────────────────
  log("👥", `Creating ${PERSONS.length} persons...`);
  const personIds: number[] = [];

  for (const p of PERSONS) {
    if (DRY_RUN) {
      log("  →", `Person: ${p.firstName} ${p.lastName} (${p.email})`);
      personIds.push(personIds.length + 1);
      continue;
    }
    const created = await createPerson(p);
    personIds.push(created.id);
    log("  ✓", `${p.firstName} ${p.lastName} → id ${created.id}`);
  }

  const ownerIds = personIds.slice(0, 5); // first 5 are building owners
  const tenantIds = personIds.slice(5); // rest are tenants

  // ── 2. Create buildings + units + rooms ────────────────────────────────────
  log("\n🏢", `Creating ${BUILDINGS.length} buildings...`);

  const buildingIds: number[] = [];
  const allUnitIds: number[][] = []; // allUnitIds[buildingIndex][unitIndex]

  for (let bi = 0; bi < BUILDINGS.length; bi++) {
    const b = BUILDINGS[bi];

    if (DRY_RUN) {
      log("  →", `Building: ${b.name} — ${b.city}`);
      buildingIds.push(bi + 1);
      const units = generateUnits(bi);
      allUnitIds.push(units.map((_, ui) => bi * 10 + ui + 1));
      continue;
    }

    const building = await createBuilding({
      name: b.name,
      streetAddress: b.streetAddress,
      postalCode: b.postalCode,
      city: b.city,
      ownerId: ownerIds[bi],
    });
    buildingIds.push(building.id);
    log("  ✓", `${b.name} (${b.city}) → id ${building.id}`);

    // Units
    const units = generateUnits(bi);
    const unitIds: number[] = [];

    for (let ui = 0; ui < units.length; ui++) {
      const u = units[ui];
      const unit = await createHousingUnit({ buildingId: building.id, ...u });
      unitIds.push(unit.id);
      log("    ✓", `Unit ${u.unitNumber} floor ${u.floor} — id ${unit.id}`);

      // Rooms
      const rooms = generateRooms(u.totalSurface);
      await createRooms(unit.id, rooms);
      log("      ✓", `${rooms.length} rooms created`);

      // Meters
      const meters = generateUnitMeters(unit.id, ui);
      for (const m of meters) {
        await createUnitMeter(unit.id, m);
        log("      ✓", `Meter ${m.type} — ${m.meterNumber}`);
      }

      // Boiler on some units (every 3rd unit)
      if (ui % 3 === 0) {
        const boiler = generateBoiler(ui, 2018 + (ui % 4));
        await createUnitBoiler(unit.id, boiler);
        log("      ✓", `Boiler ${boiler.brand} ${boiler.model}`);
      }
    }
    allUnitIds.push(unitIds);

    // Building-level water meter
    const bMeters = generateBuildingMeters(building.id);
    for (const m of bMeters) {
      await createBuildingMeter(building.id, m);
      log("    ✓", `Building meter ${m.type} — ${m.meterNumber}`);
    }

    // Building-level boiler (shared heating on buildings 0 and 4)
    if (bi === 0 || bi === 4) {
      const boiler = generateBoiler(bi + 10, 2016);
      await createBuildingBoiler(building.id, boiler);
      log("    ✓", `Building boiler ${boiler.brand} ${boiler.model}`);
    }
  }

  // ── 3. Create leases ───────────────────────────────────────────────────────
  log("\n📄", `Creating ${LEASE_TEMPLATES.length} leases across all units...`);

  // Flatten all unit IDs and cycle through them for lease assignment.
  // Units with ACTIVE/DRAFT leases can only have one at a time — the
  // script assigns at most one active/draft lease per unit.
  const flatUnits = allUnitIds.flat();
  const activeAssigned = new Set<number>(); // unitIds already having active/draft lease
  let tenantCursor = 0;
  let leaseCount = 0;

  for (let li = 0; li < LEASE_TEMPLATES.length; li++) {
    const tpl = LEASE_TEMPLATES[li];

    // Pick a unit — for active/draft, skip already-assigned units
    let unitId: number | undefined;
    for (let attempt = 0; attempt < flatUnits.length; attempt++) {
      const candidate = flatUnits[(li + attempt) % flatUnits.length];
      const needsAvailable =
        tpl.scenario === "active" ||
        tpl.scenario === "active-with-adjustments" ||
        tpl.scenario === "draft" ||
        tpl.scenario === "cancelled";
      if (!needsAvailable || !activeAssigned.has(candidate)) {
        unitId = candidate;
        if (needsAvailable) activeAssigned.add(candidate);
        break;
      }
    }
    if (!unitId) {
      log(
        "  ⚠️",
        `No available unit for lease ${li + 1} (${tpl.scenario}) — skipping`,
      );
      continue;
    }

    // Dates
    const startDate = yearsAgo(tpl.yearsAgo);
    startDate.setDate(1);
    const signatureDate = new Date(startDate);
    signatureDate.setMonth(signatureDate.getMonth() - 1);
    const endDate = addMonths(startDate, tpl.durationMonths);

    // Pick 1 or 2 tenants (cycle through tenant pool)
    const primaryId = tenantIds[tenantCursor % tenantIds.length];
    tenantCursor++;
    const tenants: Array<{
      personId: number;
      role: "PRIMARY" | "CO_TENANT" | "GUARANTOR";
    }> = [{ personId: primaryId, role: "PRIMARY" }];
    // Add a co-tenant on every other lease
    if (li % 2 === 0) {
      const coId = tenantIds[tenantCursor % tenantIds.length];
      tenantCursor++;
      if (coId !== primaryId) {
        tenants.push({ personId: coId, role: "CO_TENANT" });
      }
    }

    if (DRY_RUN) {
      log(
        "  →",
        `Lease [${tpl.scenario}] unit ${unitId} — ${isoDate(startDate)} to ${isoDate(endDate)} — €${tpl.baseRent}/mo`,
      );
      leaseCount++;
      continue;
    }

    try {
      const shouldActivate =
        tpl.scenario === "active" || tpl.scenario === "active-with-adjustments";

      const lease = await createLease(
        {
          housingUnitId: unitId,
          signatureDate: isoDate(signatureDate),
          startDate: isoDate(startDate),
          endDate: isoDate(endDate),
          leaseType: tpl.leaseType,
          durationMonths: tpl.durationMonths,
          noticePeriodMonths: tpl.noticePeriodMonths,
          monthlyRent: tpl.baseRent,
          monthlyCharges: tpl.charges,
          chargesType: tpl.chargesType,
          depositAmount: tpl.baseRent * tpl.depositMultiplier,
          depositType: "BANK_GUARANTEE",
          tenantInsuranceConfirmed: tpl.scenario !== "draft",
          tenants,
        },
        shouldActivate,
      );

      log(
        "  ✓",
        `Lease #${lease.id} [${tpl.scenario}] unit ${unitId} → ${isoDate(startDate)}`,
      );

      // Rent adjustments
      if (
        tpl.scenario === "active-with-adjustments" ||
        tpl.scenario === "finished-with-adjustments"
      ) {
        const adj1Date = addMonths(startDate, 12);
        const reason1 =
          RENT_ADJUSTMENT_REASONS[li % RENT_ADJUSTMENT_REASONS.length];
        const newRent1 = Math.round(tpl.baseRent * 1.027); // ~2.7% index
        await adjustRent(
          lease.id,
          "RENT",
          newRent1,
          reason1,
          isoDate(adj1Date),
        );
        log(
          "    ✓",
          `Rent adjustment +${newRent1 - tpl.baseRent}€ on ${isoDate(adj1Date)}`,
        );

        if (tpl.yearsAgo >= 3) {
          const adj2Date = addMonths(startDate, 24);
          const reason2 =
            RENT_ADJUSTMENT_REASONS[(li + 2) % RENT_ADJUSTMENT_REASONS.length];
          const newRent2 = Math.round(newRent1 * 1.031); // ~3.1% index
          await adjustRent(
            lease.id,
            "RENT",
            newRent2,
            reason2,
            isoDate(adj2Date),
          );
          log(
            "    ✓",
            `Rent adjustment +${newRent2 - newRent1}€ on ${isoDate(adj2Date)}`,
          );
        }
      }

      // Status transitions
      if (
        tpl.scenario === "finished" ||
        tpl.scenario === "finished-with-adjustments"
      ) {
        await changeLeaseStatus(lease.id, "ACTIVE");
        await changeLeaseStatus(lease.id, "FINISHED");
        log("    ✓", `Status → ACTIVE → FINISHED`);
      }

      if (tpl.scenario === "cancelled") {
        await changeLeaseStatus(lease.id, "CANCELLED");
        log("    ✓", `Status → CANCELLED`);
      }

      leaseCount++;
    } catch (err) {
      log("  ❌", `Lease ${li + 1} failed: ${(err as Error).message}`);
    }
  }

  // ── Summary ────────────────────────────────────────────────────────────────
  console.log("\n================================");
  log("🎉", `Seed complete!`);
  log("📊", `Persons  : ${PERSONS.length}`);
  log("🏢", `Buildings: ${BUILDINGS.length}`);
  log("🚪", `Units    : ${allUnitIds.flat().length}`);
  log("📄", `Leases   : ${leaseCount}`);
  if (DRY_RUN) log("⚠️", "DRY RUN — nothing was actually created");
  console.log("================================\n");
}

main().catch((err) => {
  console.error("\n❌ Seed failed:", err);
  process.exit(1);
});
