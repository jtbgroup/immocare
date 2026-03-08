/**
 * Real data factory — Saint-Gilles dataset.
 *
 * Usage:
 *   npm run seed:real
 *   npm run seed:dev -- --dataset=real
 *   npm run seed:dev -- --dataset=real --dry-run
 *
 * Branch: develop
 */

import type { PunctualTransaction } from "./data-factory";

// ─── Owner ────────────────────────────────────────────────────────────────────

export const PERSONS = [
  // index 0 — owner (must come first; matched by index to BUILDINGS)
  {
    firstName: "Gautier",
    lastName: "Vanderslyen",
    email: "gautier.vanderslyen@gmail.com",
    birthDate: "1978-09-09",
    city: "Bruxelles",
    postalCode: "1060",
    country: "Belgium",
  },
  // index 1 — past tenant App 1
  {
    firstName: "Veerle",
    lastName: "Bloemen",
    email: "veerle_bloemen@hotmail.com",
    city: "Bruxelles",
    postalCode: "1060",
    country: "Belgium",
  },
  // index 2 — current tenant App 1
  {
    firstName: "Arne",
    lastName: "Sonck",
    email: "sonckarne@outlook.be",
    city: "Bruxelles",
    postalCode: "1060",
    country: "Belgium",
  },
  // index 3 — current tenant App 2
  {
    firstName: "Louis",
    lastName: "Verheyen",
    email: "louis.verheyen@gmail.com",
    city: "Bruxelles",
    postalCode: "1060",
    country: "Belgium",
  },
  // index 4 — current tenant App 3
  {
    firstName: "Johan",
    lastName: "Janssen",
    email: "johanjjanssens@gmail.com",
    city: "Bruxelles",
    postalCode: "1060",
    country: "Belgium",
  },
  // index 5 — tenant App 1
  {
    firstName: "Tina",
    lastName: "Rubbrecht",
    email: "rubbrechtt@outlook.com",
    city: "Bruxelles",
    postalCode: "1060",
    country: "Belgium",
  },
];

// ─── Buildings ────────────────────────────────────────────────────────────────

export const BUILDINGS = [
  {
    name: "Saint-Gilles",
    streetAddress: "Rue de l'Hôtel des Monnaies 64",
    postalCode: "1060",
    city: "Bruxelles",
    unitCount: 3,
  },
];

// ─── Units ────────────────────────────────────────────────────────────────────
// Used by the real seed script instead of generateUnits().

export const REAL_UNITS = [
  // buildingIndex 0 — Saint-Gilles
  [
    {
      unitNumber: "App 1",
      floor: 0,
      totalSurface: 80,
      hasTerrace: true,
      hasGarden: false,
    },
    {
      unitNumber: "App 2",
      floor: 1,
      totalSurface: 80,
      hasTerrace: true,
      hasGarden: false,
    },
    {
      unitNumber: "App 3",
      floor: 2,
      totalSurface: 110,
      hasTerrace: true,
      hasGarden: false,
    },
  ],
];

// ─── Meters ───────────────────────────────────────────────────────────────────
// Indexed as REAL_METERS[buildingIndex] = { building: [...], units: [[...], [...], ...] }

export const REAL_METERS = [
  {
    // Saint-Gilles (buildingIndex 0)
    building: [
      {
        type: "WATER",
        meterNumber: "KEO201106744",
        label: "Water — common areas",
        installationNumber: "4000210535",
        customerNumber: "1000411990",
        startDate: "2015-01-01",
      },
    ],
    units: [
      // App 1 (unitIndex 0)
      [
        {
          type: "WATER",
          meterNumber: "KE0201106741",
          label: "Water — App 1",
          installationNumber: "4000222216",
          startDate: "2015-01-01",
        },
        {
          type: "GAS",
          meterNumber: "24632652",
          label: "Gas — App 1",
          eanCode: "541448965000300335",
          startDate: "2015-01-01",
        },
        {
          type: "ELECTRICITY",
          meterNumber: "5030-804-2010",
          label: "Electricity — App 1",
          eanCode: "541448920708672023",
          startDate: "2015-01-01",
        },
      ],
      // App 2 (unitIndex 1)
      [
        {
          type: "WATER",
          meterNumber: "KE0201106742",
          label: "Water — App 2",
          installationNumber: "4000222199",
          startDate: "2015-01-01",
        },
      ],
      // App 3 (unitIndex 2)
      [
        {
          type: "WATER",
          meterNumber: "KE0201106743",
          label: "Water — App 3",
          installationNumber: "4000222177",
          startDate: "2015-01-01",
        },
        {
          type: "GAS",
          meterNumber: "24632655",
          label: "Gas — App 3",
          eanCode: "541448965000300373",
          startDate: "2015-01-01",
        },
        {
          type: "ELECTRICITY",
          meterNumber: "503048022010",
          label: "Electricity — App 3",
          eanCode: "541448965000300366",
          startDate: "2015-01-01",
        },
      ],
    ],
  },
];

// ─── Lease templates ──────────────────────────────────────────────────────────
// Explicit leases; processed in order by the real seed script.
// Each entry maps to (buildingIndex, unitIndex, tenantPersonIndex).

export interface RealLease {
  buildingIndex: number;
  unitIndex: number;
  tenantPersonIndex: number; // index into PERSONS array
  scenario:
    | "finished"
    | "finished-with-adjustments"
    | "active"
    | "active-with-adjustments";
  leaseType: string;
  durationMonths: number;
  noticePeriodMonths: number;
  startDate: string; // ISO date
  endDate: string; // ISO date
  signatureDate: string; // ISO date
  baseRent: number;
  charges: number;
  chargesType: "FORFAIT" | "PROVISION";
  depositMultiplier: number;
  /** Rent adjustments to apply after activation, in chronological order */
  rentAdjustments?: Array<{
    effectiveDate: string;
    newRent: number;
    reason: string;
  }>;
}

export const REAL_LEASES: RealLease[] = [
  // ── App 1 — Veerle Bloemen (Jun 2018 → Apr 2025, finished) ─────────────────
  {
    buildingIndex: 0,
    unitIndex: 0,
    tenantPersonIndex: 1, // Veerle Bloemen
    scenario: "finished-with-adjustments",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    startDate: "2018-05-01",
    endDate: "2022-04-30",
    signatureDate: "2018-02-26",
    baseRent: 900,
    charges: 0,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    rentAdjustments: [
      {
        effectiveDate: "2024-10-01",
        newRent: 1050,
        reason: "Indexation annuelle loyer (art. 1728bis C.civ.)",
      },
    ],
  },

  // ── App 1 — Arne Sonck (May 2025 → ongoing, active) ────────────────────────
  {
    buildingIndex: 0,
    unitIndex: 0,
    tenantPersonIndex: 2, // Arne Sonck
    scenario: "active",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    startDate: "2025-05-01",
    endDate: "2034-04-30",
    signatureDate: "2025-04-01",
    baseRent: 1200,
    charges: 0,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
  },

  // ── App 2 — Louis Verheyen (Sep 2025 → ongoing, active) ────────────────────
  {
    buildingIndex: 0,
    unitIndex: 1,
    tenantPersonIndex: 3, // Louis Verheyen
    scenario: "active",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    startDate: "2025-09-01",
    endDate: "2034-08-31",
    signatureDate: "2025-08-01",
    baseRent: 1200,
    charges: 8.5,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
  },

  // ── App 3 — Johan Janssen (Aug 2025 → ongoing, active) ─────────────────────
  {
    buildingIndex: 0,
    unitIndex: 2,
    tenantPersonIndex: 4, // Johan Janssen
    scenario: "active",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    startDate: "2025-08-01",
    endDate: "2034-07-31",
    signatureDate: "2025-07-01",
    baseRent: 1500,
    charges: 8.5,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
  },
  // ── App 2 — Tina Rubbrecht (06Sep2024 → 06Oct2025) ─────────────────────
  {
    buildingIndex: 0,
    unitIndex: 1,
    tenantPersonIndex: 5, // Tina Rubbrecht
    scenario: "finished-with-adjustments",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    startDate: "2024-09-06",
    endDate: "2025-10-06",
    signatureDate: "2024-08-26",
    baseRent: 1200,
    charges: 8.5,
    chargesType: "FORFAIT",
    depositMultiplier: 1,
  },
];

// ─── Bank accounts ────────────────────────────────────────────────────────────
// TODO: add your real bank account(s) here.

export const BANK_ACCOUNTS_SEED = [
  {
    label: "Keytrade Immo Compte courant",
    accountNumber: "BE13 6512 1242 3639",
    type: "CURRENT" as const,
    isActive: true,
  },
];

// ─── Fire extinguishers ───────────────────────────────────────────────────────
// One entry per physical extinguisher; each has a list of revision records.

export interface RealExtinguisher {
  buildingIndex: number;
  unitIndex: number | null; // null = common area
  identificationNumber: string;
  notes: string | null;
  revisions: Array<{
    revisionDate: string;
    validUntil: string;
    notes: string | null;
  }>;
}

export const REAL_EXTINGUISHERS: RealExtinguisher[] = [
  {
    buildingIndex: 0,
    unitIndex: 0,
    identificationNumber: "EXT-APP1",
    notes: "Appartement 1er",
    revisions: [
      { revisionDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { revisionDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { revisionDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: 1,
    identificationNumber: "EXT-APP2",
    notes: "Appartement 2ème",
    revisions: [
      { revisionDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { revisionDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { revisionDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: 2,
    identificationNumber: "EXT-APP3",
    notes: "Appartement 3ème (4ème étage)",
    revisions: [
      { revisionDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { revisionDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { revisionDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: null,
    identificationNumber: "EXT-CAVE-AV",
    notes: "Cave avant",
    revisions: [
      { revisionDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { revisionDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { revisionDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: null,
    identificationNumber: "EXT-CAVE-AR",
    notes: "Cave arrière",
    revisions: [
      { revisionDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { revisionDate: "2023-01-14", validUntil: "2024-01-14", notes: null },
      { revisionDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
];

// ─── Boilers ──────────────────────────────────────────────────────────────────
// Service records use the same dates as extinguishers (confirmed identical).

export interface RealBoiler {
  buildingIndex: number;
  unitIndex: number | null;
  brand: string;
  model: string | null;
  fuelType: string;
  installationDate: string;
  serialNumber: string | null;
  notes?: string | null;
  services: Array<{
    serviceDate: string;
    validUntil: string;
    notes: string | null;
  }>;
}

export const REAL_BOILERS: RealBoiler[] = [
  {
    buildingIndex: 0,
    unitIndex: 0,
    brand: "Unknown",
    model: null,
    fuelType: "GAS",
    installationDate: "2015-01-01",
    serialNumber: null,
    services: [
      { serviceDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { serviceDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { serviceDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: 1,
    brand: "Unknown",
    model: null,
    fuelType: "GAS",
    installationDate: "2015-01-01",
    serialNumber: null,
    services: [
      { serviceDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { serviceDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { serviceDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: 2,
    brand: "Unknown",
    model: null,
    fuelType: "GAS",
    installationDate: "2015-01-01",
    serialNumber: null,
    services: [
      { serviceDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { serviceDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { serviceDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: null, // cave avant — building-level boiler
    brand: "Unknown",
    model: null,
    fuelType: "GAS",
    installationDate: "2015-01-01",
    serialNumber: null,
    services: [
      { serviceDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { serviceDate: "2023-01-10", validUntil: "2024-01-10", notes: null },
      { serviceDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
  {
    buildingIndex: 0,
    unitIndex: null, // cave arrière — building-level boiler
    brand: "Unknown",
    model: null,
    fuelType: "GAS",
    installationDate: "2015-01-01",
    serialNumber: null,
    services: [
      { serviceDate: "2022-01-08", validUntil: "2023-01-08", notes: null },
      { serviceDate: "2023-01-14", validUntil: "2024-01-14", notes: null },
      { serviceDate: "2025-01-15", validUntil: "2026-01-15", notes: null },
    ],
  },
];

// ─── Punctual transactions ────────────────────────────────────────────────────
// TODO: add real expense/income transactions as needed.

export const PUNCTUAL_TRANSACTIONS: PunctualTransaction[] = [];

// ─── Rent adjustment reasons (unused in real dataset, kept for type compat) ───

export const RENT_ADJUSTMENT_REASONS: string[] = [
  "Indexation annuelle loyer (art. 1728bis C.civ.)",
];
