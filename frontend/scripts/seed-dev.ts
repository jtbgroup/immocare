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
  createBankAccount,
  createBuilding,
  createBuildingBoiler,
  createBuildingMeter,
  createHousingUnit,
  createLease,
  createPerson,
  createRooms,
  createTransaction,
  createUnitBoiler,
  createUnitMeter,
  fetchSubcategories,
  login,
} from "../e2e/shared/api-client";

import {
  BANK_ACCOUNTS_SEED,
  BUILDINGS,
  LEASE_TEMPLATES,
  PERSONS,
  PUNCTUAL_TRANSACTIONS,
  RENT_ADJUSTMENT_REASONS,
  addMonths,
  generateBoiler,
  generateBuildingMeters,
  generateRentTransactions,
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

// ─── Tenant IBAN registry ─────────────────────────────────────────────────────
// Stable fake IBANs derived from tenant index — used for rent counterparty.

function tenantIban(tenantIndex: number): string {
  const n = (tenantIndex + 1).toString().padStart(4, "0");
  return `BE${n}001234${n}${n}`;
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

  // ── 2. Create buildings + units + rooms ───────────────────────────────────
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

    const units = generateUnits(bi);
    const unitIds: number[] = [];

    for (let ui = 0; ui < units.length; ui++) {
      const u = units[ui];
      const unit = await createHousingUnit({ buildingId: building.id, ...u });
      unitIds.push(unit.id);
      log("    ✓", `Unit ${u.unitNumber} floor ${u.floor} — id ${unit.id}`);

      const rooms = generateRooms(u.totalSurface);
      await createRooms(unit.id, rooms);
      log("      ✓", `${rooms.length} rooms created`);

      const meters = generateUnitMeters(unit.id, ui);
      for (const m of meters) {
        await createUnitMeter(unit.id, m);
        log("      ✓", `Meter ${m.type} — ${m.meterNumber}`);
      }

      if (ui % 3 === 0) {
        const boiler = generateBoiler(ui, 2018 + (ui % 4));
        await createUnitBoiler(unit.id, boiler);
        log("      ✓", `Boiler ${boiler.brand} ${boiler.model}`);
      }
    }
    allUnitIds.push(unitIds);

    const bMeters = generateBuildingMeters(building.id);
    for (const m of bMeters) {
      await createBuildingMeter(building.id, m);
      log("    ✓", `Building meter ${m.type} — ${m.meterNumber}`);
    }

    if (bi === 0 || bi === 4) {
      const boiler = generateBoiler(bi + 10, 2016);
      await createBuildingBoiler(building.id, boiler);
      log("    ✓", `Building boiler ${boiler.brand} ${boiler.model}`);
    }
  }

  // ── 3. Create leases ───────────────────────────────────────────────────────
  log("\n📄", `Creating ${LEASE_TEMPLATES.length} leases across all units...`);

  const flatUnitIds = allUnitIds.flat();
  let leaseCount = 0;

  // Track (buildingIndex, unitIndex, tenantName, tenantIban, rent, charges,
  //        startDate, endDate) for active/finished leases → used later for
  // rent transaction generation.
  interface LeaseInfo {
    leaseId: number;
    buildingIndex: number;
    unitIndex: number;
    tenantName: string;
    tenantIban: string;
    monthlyRent: number;
    monthlyCharges: number;
    startDate: Date;
    endDate: Date;
    active: boolean; // false = finished/cancelled/draft
  }
  const leaseInfos: LeaseInfo[] = [];

  for (let li = 0; li < LEASE_TEMPLATES.length; li++) {
    const tpl = LEASE_TEMPLATES[li];
    const unitId = flatUnitIds[li % flatUnitIds.length];
    const tenantIndex = li % tenantIds.length;
    const tenant = PERSONS[5 + tenantIndex];

    // Derive building/unit indices from unitId position in allUnitIds
    let buildingIndex = 0;
    let unitIndex = 0;
    outer: for (let bi = 0; bi < allUnitIds.length; bi++) {
      for (let ui = 0; ui < allUnitIds[bi].length; ui++) {
        if (allUnitIds[bi][ui] === unitId) {
          buildingIndex = bi;
          unitIndex = ui;
          break outer;
        }
      }
    }

    const startDate = yearsAgo(tpl.yearsAgo);
    const endDate = addMonths(startDate, tpl.durationMonths);
    const signatureDate = addMonths(startDate, -1);

    try {
      if (DRY_RUN) {
        log(
          "  →",
          `Lease [${tpl.scenario}] unit ${unitId} — ${tenant.firstName} ${tenant.lastName} — ${tpl.baseRent}€/mo`,
        );
        leaseInfos.push({
          leaseId: li + 1,
          buildingIndex,
          unitIndex,
          tenantName: `${tenant.firstName} ${tenant.lastName}`,
          tenantIban: tenantIban(tenantIndex),
          monthlyRent: tpl.baseRent,
          monthlyCharges: tpl.charges,
          startDate,
          endDate,
          active:
            tpl.scenario === "active" ||
            tpl.scenario === "active-with-adjustments",
        });
        leaseCount++;
        continue;
      }

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
          tenantInsuranceConfirmed: true,
          tenants: [{ personId: tenantIds[tenantIndex], role: "PRIMARY" }],
        },
        false,
      );
      log(
        "  ✓",
        `Lease ${lease.id} [${tpl.scenario}] unit ${unitId} — ${tenant.firstName} ${tenant.lastName}`,
      );

      // Rent adjustments
      if (
        tpl.scenario === "active-with-adjustments" ||
        tpl.scenario === "finished-with-adjustments"
      ) {
        const adj1Date = addMonths(startDate, 12);
        const reason1 =
          RENT_ADJUSTMENT_REASONS[li % RENT_ADJUSTMENT_REASONS.length];
        const newRent1 = Math.round(tpl.baseRent * 1.028);
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
          const newRent2 = Math.round(newRent1 * 1.031);
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

      leaseInfos.push({
        leaseId: lease.id,
        buildingIndex,
        unitIndex,
        tenantName: `${tenant.firstName} ${tenant.lastName}`,
        tenantIban: tenantIban(tenantIndex),
        monthlyRent: tpl.baseRent,
        monthlyCharges: tpl.charges,
        startDate,
        endDate,
        active:
          tpl.scenario === "active" ||
          tpl.scenario === "active-with-adjustments" ||
          tpl.scenario === "finished" ||
          tpl.scenario === "finished-with-adjustments",
      });
      leaseCount++;
    } catch (err) {
      log("  ❌", `Lease ${li + 1} failed: ${(err as Error).message}`);
    }
  }

  // ── 4. Create bank accounts ────────────────────────────────────────────────
  log("\n🏦", `Creating ${BANK_ACCOUNTS_SEED.length} bank accounts...`);
  const bankAccountIds: number[] = [];

  for (const ba of BANK_ACCOUNTS_SEED) {
    if (DRY_RUN) {
      log("  →", `Bank account: ${ba.label} (${ba.accountNumber})`);
      bankAccountIds.push(bankAccountIds.length + 1);
      continue;
    }
    try {
      const created = await createBankAccount(ba);
      bankAccountIds.push(created.id);
      log("  ✓", `${ba.label} → id ${created.id}`);
    } catch (err) {
      // Ignore duplicate IBAN errors on re-runs
      log("  ⚠", `${ba.label} skipped: ${(err as Error).message}`);
      bankAccountIds.push(0);
    }
  }

  // Primary current account used for most transactions (first CURRENT account)
  const primaryBankAccountId = bankAccountIds[0] || undefined;

  // ── 5. Build subcategory name → id map ────────────────────────────────────
  log("\n🏷️ ", "Loading subcategory catalogue...");
  const subcategoryMap = new Map<string, number>();

  if (!DRY_RUN) {
    const subcats = await fetchSubcategories();
    for (const sc of subcats) {
      subcategoryMap.set(sc.name, sc.id);
    }
    log("  ✓", `${subcategoryMap.size} subcategories loaded`);
  }

  // ── 6. Create transactions ─────────────────────────────────────────────────

  // ── 6a. Rent transactions (one per occupied month per active/finished lease)
  log("\n💶", "Generating rent transactions...");
  let rentTxCount = 0;

  for (const info of leaseInfos) {
    if (!info.active) continue;

    const rentTxs = generateRentTransactions({
      tenantName: info.tenantName,
      tenantIban: info.tenantIban,
      monthlyRent: info.monthlyRent,
      monthlyCharges: info.monthlyCharges,
      leaseStartDate: info.startDate,
      leaseEndDate: info.endDate,
      buildingIndex: info.buildingIndex,
      unitIndex: info.unitIndex,
    });

    for (const tx of rentTxs) {
      if (DRY_RUN) {
        log(
          "  →",
          `Rent ${tx.transactionDate} — ${tx.counterpartyName} — ${tx.amount}€`,
        );
        rentTxCount++;
        continue;
      }
      try {
        const buildingId = buildingIds[tx.buildingIndex] ?? undefined;
        const unitId =
          allUnitIds[tx.buildingIndex]?.[tx.unitIndex] ?? undefined;
        const subcategoryId = subcategoryMap.get(tx.subcategoryName);

        await createTransaction({
          direction: tx.direction,
          transactionDate: tx.transactionDate,
          accountingMonth: tx.accountingMonth,
          amount: tx.amount,
          description: tx.description,
          counterpartyName: tx.counterpartyName,
          counterpartyAccount: tx.counterpartyAccount,
          bankAccountId: primaryBankAccountId,
          subcategoryId,
          leaseId: info.leaseId,
          housingUnitId: unitId,
          buildingId,
        });
        rentTxCount++;
      } catch (err) {
        log(
          "  ❌",
          `Rent tx ${tx.transactionDate} failed: ${(err as Error).message}`,
        );
      }
    }
  }
  log("  ✓", `${rentTxCount} rent transactions created`);

  // ── 6b. Punctual transactions (water, gas, maintenance, rente, etc.) ───────
  log(
    "\n💳",
    `Creating ${PUNCTUAL_TRANSACTIONS.length} punctual transactions...`,
  );
  let punctualTxCount = 0;

  for (const tx of PUNCTUAL_TRANSACTIONS) {
    if (DRY_RUN) {
      log(
        "  →",
        `[${tx.direction}] ${tx.transactionDate} — ${tx.counterpartyName} — ${tx.amount}€ (${tx.subcategoryName})`,
      );
      punctualTxCount++;
      continue;
    }
    try {
      const buildingId =
        tx.buildingIndex !== undefined
          ? (buildingIds[tx.buildingIndex] ?? undefined)
          : undefined;
      const unitId =
        tx.buildingIndex !== undefined && tx.unitIndex !== undefined
          ? (allUnitIds[tx.buildingIndex]?.[tx.unitIndex] ?? undefined)
          : undefined;
      const subcategoryId = subcategoryMap.get(tx.subcategoryName);

      await createTransaction({
        direction: tx.direction,
        transactionDate: tx.transactionDate,
        accountingMonth: tx.accountingMonth,
        amount: tx.amount,
        description: tx.description,
        counterpartyName: tx.counterpartyName,
        counterpartyAccount: tx.counterpartyAccount,
        bankAccountId: primaryBankAccountId,
        subcategoryId,
        buildingId,
        housingUnitId: unitId,
      });
      punctualTxCount++;
    } catch (err) {
      log(
        "  ❌",
        `Tx ${tx.transactionDate} ${tx.counterpartyName} failed: ${(err as Error).message}`,
      );
    }
  }
  log("  ✓", `${punctualTxCount} punctual transactions created`);

  // ── Summary ────────────────────────────────────────────────────────────────
  console.log("\n================================");
  log("🎉", `Seed complete!`);
  log("📊", `Persons      : ${PERSONS.length}`);
  log("🏢", `Buildings    : ${BUILDINGS.length}`);
  log("🚪", `Units        : ${allUnitIds.flat().length}`);
  log("📄", `Leases       : ${leaseCount}`);
  log("🏦", `Bank accounts: ${BANK_ACCOUNTS_SEED.length}`);
  log("💶", `Rent txs     : ${rentTxCount}`);
  log("💳", `Other txs    : ${punctualTxCount}`);
  log("📈", `Total txs    : ${rentTxCount + punctualTxCount}`);
  if (DRY_RUN) log("⚠️", "DRY RUN — nothing was actually created");
  console.log("================================\n");
}

main().catch((err) => {
  console.error("\n❌ Seed failed:", err);
  process.exit(1);
});
