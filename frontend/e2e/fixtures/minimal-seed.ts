/**
 * Minimal seed for E2E tests.
 * File location in project: frontend/e2e/fixtures/minimal-seed.ts
 *
 * Creates just enough data for a test to run, then returns the IDs
 * so tests can reference them directly.
 *
 * Usage in a Playwright test:
 *   import { seedMinimal, SeedResult } from '../fixtures/minimal-seed';
 *   let seed: SeedResult;
 *   test.beforeEach(async () => { seed = await seedMinimal(); });
 */

import {
  login,
  createPerson,
  createBuilding,
  createHousingUnit,
  createRooms,
  createLease,
} from '../shared/api-client';

export interface SeedResult {
  ownerPersonId: number;
  tenantPersonId: number;
  buildingId: number;
  unitId: number;
  activeLeaseId: number;
  draftLeaseId: number;
}

export async function seedMinimal(): Promise<SeedResult> {
  await login();

  // One owner
  const owner = await createPerson({
    firstName: 'Test',
    lastName: 'Owner',
    email: `owner.${Date.now()}@e2e.test`,
    city: 'Bruxelles',
    postalCode: '1000',
    country: 'Belgium',
  });

  // One tenant
  const tenant = await createPerson({
    firstName: 'Test',
    lastName: 'Tenant',
    email: `tenant.${Date.now()}@e2e.test`,
    city: 'Bruxelles',
    postalCode: '1000',
    country: 'Belgium',
  });

  // One building
  const building = await createBuilding({
    name: `E2E Building ${Date.now()}`,
    streetAddress: 'Rue du Test 1',
    postalCode: '1000',
    city: 'Bruxelles',
    ownerId: owner.id,
  });

  // One unit with rooms
  const unit = await createHousingUnit({
    buildingId: building.id,
    unitNumber: 'A01',
    floor: 1,
    totalSurface: 75,
  });

  await createRooms(unit.id, [
    { roomType: 'LIVING_ROOM', approximateSurface: 25 },
    { roomType: 'BEDROOM', approximateSurface: 15 },
    { roomType: 'KITCHEN', approximateSurface: 12 },
    { roomType: 'BATHROOM', approximateSurface: 8 },
  ]);

  const iso = (d: Date) => d.toISOString().split('T')[0];
  const today = new Date();

  // Active lease (created with activate=true)
  const startDate = new Date(today);
  startDate.setMonth(startDate.getMonth() - 6);
  const endDate = new Date(startDate);
  endDate.setFullYear(endDate.getFullYear() + 9);
  const signatureDate = new Date(startDate);
  signatureDate.setMonth(signatureDate.getMonth() - 1);

  const activeLease = await createLease(
    {
      housingUnitId: unit.id,
      signatureDate: iso(signatureDate),
      startDate: iso(startDate),
      endDate: iso(endDate),
      leaseType: 'MAIN_RESIDENCE_9Y',
      durationMonths: 108,
      noticePeriodMonths: 3,
      monthlyRent: 850,
      monthlyCharges: 80,
      chargesType: 'FORFAIT',
      depositAmount: 1700,
      depositType: 'BANK_GUARANTEE',
      tenantInsuranceConfirmed: true,
      tenants: [{ personId: tenant.id, role: 'PRIMARY' }],
    },
    true,
  );

  // Second unit for a draft lease
  const unit2 = await createHousingUnit({
    buildingId: building.id,
    unitNumber: 'A02',
    floor: 1,
    totalSurface: 65,
  });

  const futureStart = new Date(today);
  futureStart.setMonth(futureStart.getMonth() + 1);
  const futureEnd = new Date(futureStart);
  futureEnd.setFullYear(futureEnd.getFullYear() + 3);
  const futureSign = new Date(futureStart);
  futureSign.setMonth(futureSign.getMonth() - 1);

  const draftLease = await createLease({
    housingUnitId: unit2.id,
    signatureDate: iso(futureSign),
    startDate: iso(futureStart),
    endDate: iso(futureEnd),
    leaseType: 'SHORT_TERM_3Y',
    durationMonths: 36,
    noticePeriodMonths: 1,
    monthlyRent: 750,
    monthlyCharges: 60,
    chargesType: 'FORFAIT',
    depositAmount: 1500,
    depositType: 'BANK_GUARANTEE',
    tenantInsuranceConfirmed: false,
    tenants: [{ personId: tenant.id, role: 'PRIMARY' }],
  });

  return {
    ownerPersonId: owner.id,
    tenantPersonId: tenant.id,
    buildingId: building.id,
    unitId: unit.id,
    activeLeaseId: activeLease.id,
    draftLeaseId: draftLease.id,
  };
}
