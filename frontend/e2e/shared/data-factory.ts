/**
 * Data factory — generates realistic Belgian property management data.
 * Used by both the E2E minimal seed and the dev seed script.
 */

// ─── Buildings ────────────────────────────────────────────────────────────────

export const BUILDINGS = [
  {
    name: "Résidence Les Tilleuls",
    streetAddress: "Rue de la Loi 42",
    postalCode: "1000",
    city: "Bruxelles",
    unitCount: 5,
  },
  {
    name: "Immeuble du Parc",
    streetAddress: "Avenue du Parc 18",
    postalCode: "4000",
    city: "Liège",
    unitCount: 4,
  },
  {
    name: "Résidence Gaston",
    streetAddress: "Veldstraat 77",
    postalCode: "9000",
    city: "Gent",
    unitCount: 3,
  },
  {
    name: "Le Beffroi",
    streetAddress: "Grand-Place 5",
    postalCode: "7000",
    city: "Mons",
    unitCount: 2,
  },
  {
    name: "Résidence Meuse",
    streetAddress: "Quai de Namur 12",
    postalCode: "5000",
    city: "Namur",
    unitCount: 4,
  },
];

// ─── Unit templates per building ──────────────────────────────────────────────

export function generateUnits(buildingIndex: number): Array<{
  unitNumber: string;
  floor: number;
  totalSurface: number;
  hasTerrace: boolean;
  terraceSurface?: number;
  terraceOrientation?: string;
  hasGarden: boolean;
  gardenSurface?: number;
  gardenOrientation?: string;
}> {
  const templates = [
    [
      {
        unitNumber: "A01",
        floor: 0,
        totalSurface: 75,
        hasTerrace: false,
        hasGarden: true,
        gardenSurface: 30,
        gardenOrientation: "S",
      },
      {
        unitNumber: "A02",
        floor: 0,
        totalSurface: 68,
        hasTerrace: false,
        hasGarden: false,
      },
      {
        unitNumber: "B01",
        floor: 1,
        totalSurface: 82,
        hasTerrace: true,
        terraceSurface: 12,
        terraceOrientation: "SW",
        hasGarden: false,
      },
      {
        unitNumber: "B02",
        floor: 1,
        totalSurface: 79,
        hasTerrace: true,
        terraceSurface: 10,
        terraceOrientation: "NE",
        hasGarden: false,
      },
      {
        unitNumber: "C01",
        floor: 2,
        totalSurface: 91,
        hasTerrace: true,
        terraceSurface: 15,
        terraceOrientation: "S",
        hasGarden: false,
      },
    ],
    [
      {
        unitNumber: "01",
        floor: 0,
        totalSurface: 65,
        hasTerrace: false,
        hasGarden: true,
        gardenSurface: 25,
        gardenOrientation: "W",
      },
      {
        unitNumber: "02",
        floor: 0,
        totalSurface: 70,
        hasTerrace: false,
        hasGarden: false,
      },
      {
        unitNumber: "11",
        floor: 1,
        totalSurface: 88,
        hasTerrace: true,
        terraceSurface: 14,
        terraceOrientation: "S",
        hasGarden: false,
      },
      {
        unitNumber: "12",
        floor: 1,
        totalSurface: 76,
        hasTerrace: false,
        hasGarden: false,
      },
    ],
    [
      {
        unitNumber: "GV",
        floor: 0,
        totalSurface: 60,
        hasTerrace: false,
        hasGarden: true,
        gardenSurface: 20,
        gardenOrientation: "E",
      },
      {
        unitNumber: "1V",
        floor: 1,
        totalSurface: 72,
        hasTerrace: true,
        terraceSurface: 9,
        terraceOrientation: "W",
        hasGarden: false,
      },
      {
        unitNumber: "2V",
        floor: 2,
        totalSurface: 85,
        hasTerrace: true,
        terraceSurface: 11,
        terraceOrientation: "S",
        hasGarden: false,
      },
    ],
    [
      {
        unitNumber: "REZ",
        floor: 0,
        totalSurface: 95,
        hasTerrace: false,
        hasGarden: true,
        gardenSurface: 45,
        gardenOrientation: "S",
      },
      {
        unitNumber: "ETG",
        floor: 1,
        totalSurface: 88,
        hasTerrace: true,
        terraceSurface: 16,
        terraceOrientation: "SE",
        hasGarden: false,
      },
    ],
    [
      {
        unitNumber: "A",
        floor: 0,
        totalSurface: 71,
        hasTerrace: false,
        hasGarden: true,
        gardenSurface: 28,
        gardenOrientation: "SW",
      },
      {
        unitNumber: "B",
        floor: 0,
        totalSurface: 68,
        hasTerrace: false,
        hasGarden: false,
      },
      {
        unitNumber: "C",
        floor: 1,
        totalSurface: 80,
        hasTerrace: true,
        terraceSurface: 10,
        terraceOrientation: "S",
        hasGarden: false,
      },
      {
        unitNumber: "D",
        floor: 2,
        totalSurface: 93,
        hasTerrace: true,
        terraceSurface: 18,
        terraceOrientation: "SW",
        hasGarden: false,
      },
    ],
  ];
  return templates[buildingIndex] ?? templates[0];
}

// ─── Rooms per unit ───────────────────────────────────────────────────────────

export function generateRooms(
  surface: number,
): Array<{ roomType: string; approximateSurface: number }> {
  if (surface < 65) {
    return [
      { roomType: "LIVING_ROOM", approximateSurface: 22 },
      { roomType: "BEDROOM", approximateSurface: 14 },
      { roomType: "KITCHEN", approximateSurface: 10 },
      { roomType: "BATHROOM", approximateSurface: 7 },
      { roomType: "HALLWAY", approximateSurface: 5 },
    ];
  }
  if (surface < 80) {
    return [
      { roomType: "LIVING_ROOM", approximateSurface: 25 },
      { roomType: "BEDROOM", approximateSurface: 16 },
      { roomType: "BEDROOM", approximateSurface: 12 },
      { roomType: "KITCHEN", approximateSurface: 11 },
      { roomType: "BATHROOM", approximateSurface: 8 },
      { roomType: "TOILET", approximateSurface: 2 },
      { roomType: "HALLWAY", approximateSurface: 6 },
    ];
  }
  return [
    { roomType: "LIVING_ROOM", approximateSurface: 30 },
    { roomType: "DINING_ROOM", approximateSurface: 14 },
    { roomType: "BEDROOM", approximateSurface: 18 },
    { roomType: "BEDROOM", approximateSurface: 14 },
    { roomType: "KITCHEN", approximateSurface: 13 },
    { roomType: "BATHROOM", approximateSurface: 9 },
    { roomType: "TOILET", approximateSurface: 2 },
    { roomType: "HALLWAY", approximateSurface: 7 },
  ];
}

// ─── Persons ──────────────────────────────────────────────────────────────────

export const PERSONS = [
  // Owners (first 5 — will be assigned as building owners)
  {
    firstName: "Henri",
    lastName: "Dupont",
    email: "h.dupont@immocare-test.be",
    gsm: "+32 475 11 22 33",
    city: "Bruxelles",
    postalCode: "1000",
    streetAddress: "Rue Royale 10",
    nationalId: "BE-001",
  },
  {
    firstName: "Martine",
    lastName: "Lecomte",
    email: "m.lecomte@immocare-test.be",
    gsm: "+32 477 44 55 66",
    city: "Liège",
    postalCode: "4000",
    streetAddress: "Quai de Rome 7",
    nationalId: "BE-002",
  },
  {
    firstName: "Pieter",
    lastName: "Vanderberg",
    email: "p.vanderberg@immocare-test.be",
    gsm: "+32 478 77 88 99",
    city: "Gent",
    postalCode: "9000",
    streetAddress: "Korenmarkt 3",
    nationalId: "BE-003",
  },
  {
    firstName: "Sophie",
    lastName: "Maes",
    email: "s.maes@immocare-test.be",
    gsm: "+32 479 00 11 22",
    city: "Mons",
    postalCode: "7000",
    streetAddress: "Rue de la Coupe 14",
    nationalId: "BE-004",
  },
  {
    firstName: "Jean-Paul",
    lastName: "Renard",
    email: "jp.renard@immocare-test.be",
    gsm: "+32 471 33 44 55",
    city: "Namur",
    postalCode: "5000",
    streetAddress: "Rue de Fer 22",
    nationalId: "BE-005",
  },
  // Tenants
  {
    firstName: "Alice",
    lastName: "Moreau",
    email: "alice.moreau@gmail.com",
    gsm: "+32 472 10 20 30",
    city: "Bruxelles",
    postalCode: "1050",
    streetAddress: "Avenue Louise 88",
    nationalId: "BE-006",
  },
  {
    firstName: "Thomas",
    lastName: "Bernard",
    email: "t.bernard@outlook.com",
    gsm: "+32 473 20 30 40",
    city: "Bruxelles",
    postalCode: "1000",
    streetAddress: "Rue Neuve 15",
    nationalId: "BE-007",
  },
  {
    firstName: "Emma",
    lastName: "Willems",
    email: "emma.willems@hotmail.be",
    gsm: "+32 474 30 40 50",
    city: "Liège",
    postalCode: "4020",
    streetAddress: "Rue Saint-Gilles 6",
    nationalId: "BE-008",
  },
  {
    firstName: "Lucas",
    lastName: "Dubois",
    email: "l.dubois@gmail.com",
    gsm: "+32 475 40 50 60",
    city: "Gent",
    postalCode: "9000",
    streetAddress: "Langemunt 44",
    nationalId: "BE-009",
  },
  {
    firstName: "Claire",
    lastName: "Peeters",
    email: "c.peeters@skynet.be",
    gsm: "+32 476 50 60 70",
    city: "Namur",
    postalCode: "5000",
    streetAddress: "Avenue de la Plante 33",
    nationalId: "BE-010",
  },
  {
    firstName: "Antoine",
    lastName: "Simon",
    email: "a.simon@gmail.com",
    gsm: "+32 477 60 70 80",
    city: "Mons",
    postalCode: "7000",
    streetAddress: "Rue des Arbalestriers 9",
    nationalId: "BE-011",
  },
  {
    firstName: "Julie",
    lastName: "Claes",
    email: "j.claes@live.be",
    gsm: "+32 478 70 80 90",
    city: "Bruxelles",
    postalCode: "1030",
    streetAddress: "Chaussée de Haecht 101",
    nationalId: "BE-012",
  },
  {
    firstName: "Maxime",
    lastName: "Leclercq",
    email: "m.leclercq@proximus.be",
    gsm: "+32 479 80 90 00",
    city: "Liège",
    postalCode: "4000",
    streetAddress: "Rue Léopold 55",
    nationalId: "BE-013",
  },
  {
    firstName: "Laura",
    lastName: "Hermans",
    email: "laura.hermans@gmail.com",
    gsm: "+32 470 90 00 11",
    city: "Gent",
    postalCode: "9000",
    streetAddress: "Brabantdam 28",
    nationalId: "BE-014",
  },
  {
    firstName: "Nicolas",
    lastName: "Jacobs",
    email: "n.jacobs@telenet.be",
    gsm: "+32 471 00 11 22",
    city: "Bruxelles",
    postalCode: "1180",
    streetAddress: "Avenue Molière 67",
    nationalId: "BE-015",
  },
  {
    firstName: "Isabelle",
    lastName: "Fontaine",
    email: "i.fontaine@gmail.com",
    gsm: "+32 472 11 22 33",
    city: "Namur",
    postalCode: "5100",
    streetAddress: "Rue du Collège 12",
    nationalId: "BE-016",
  },
  {
    firstName: "Kevin",
    lastName: "Mertens",
    email: "k.mertens@outlook.be",
    gsm: "+32 473 22 33 44",
    city: "Mons",
    postalCode: "7000",
    streetAddress: "Avenue de Valenciennes 4",
    nationalId: "BE-017",
  },
  {
    firstName: "Sarah",
    lastName: "Baert",
    email: "s.baert@hotmail.com",
    gsm: "+32 474 33 44 55",
    city: "Gent",
    postalCode: "9000",
    streetAddress: "Coupure Links 82",
    nationalId: "BE-018",
  },
  {
    firstName: "Romain",
    lastName: "Henry",
    email: "r.henry@gmail.com",
    gsm: "+32 475 44 55 66",
    city: "Bruxelles",
    postalCode: "1000",
    streetAddress: "Boulevard du Midi 33",
    nationalId: "BE-019",
  },
  {
    firstName: "Nathalie",
    lastName: "Dumont",
    email: "n.dumont@skynet.be",
    gsm: "+32 476 55 66 77",
    city: "Liège",
    postalCode: "4000",
    streetAddress: "Rue Hors-Château 19",
    nationalId: "BE-020",
  },
];

// ─── Lease scenarios ──────────────────────────────────────────────────────────

export type LeaseScenario =
  | "active"
  | "active-with-adjustments"
  | "finished"
  | "finished-with-adjustments"
  | "draft"
  | "cancelled";

export interface LeaseTemplate {
  scenario: LeaseScenario;
  leaseType: string;
  durationMonths: number;
  noticePeriodMonths: number;
  baseRent: number;
  charges: number;
  chargesType: string;
  depositMultiplier: number; // months of rent
  yearsAgo: number; // how far back the lease started
}

export const LEASE_TEMPLATES: LeaseTemplate[] = [
  // Active leases — currently running
  {
    scenario: "active",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 850,
    charges: 80,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 2,
  },
  {
    scenario: "active",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 920,
    charges: 100,
    chargesType: "PROVISION",
    depositMultiplier: 2,
    yearsAgo: 1,
  },
  {
    scenario: "active",
    leaseType: "MAIN_RESIDENCE_3Y",
    durationMonths: 36,
    noticePeriodMonths: 3,
    baseRent: 750,
    charges: 60,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 1,
  },
  {
    scenario: "active",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 1100,
    charges: 150,
    chargesType: "PROVISION",
    depositMultiplier: 2,
    yearsAgo: 3,
  },
  {
    scenario: "active",
    leaseType: "STUDENT",
    durationMonths: 12,
    noticePeriodMonths: 1,
    baseRent: 550,
    charges: 50,
    chargesType: "FORFAIT",
    depositMultiplier: 1,
    yearsAgo: 0,
  },
  // Active with rent adjustments
  {
    scenario: "active-with-adjustments",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 780,
    charges: 75,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 4,
  },
  {
    scenario: "active-with-adjustments",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 870,
    charges: 90,
    chargesType: "PROVISION",
    depositMultiplier: 2,
    yearsAgo: 5,
  },
  {
    scenario: "active-with-adjustments",
    leaseType: "MAIN_RESIDENCE_3Y",
    durationMonths: 36,
    noticePeriodMonths: 3,
    baseRent: 690,
    charges: 65,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 2,
  },
  // Finished leases — historical
  {
    scenario: "finished",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 700,
    charges: 70,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 10,
  },
  {
    scenario: "finished",
    leaseType: "MAIN_RESIDENCE_3Y",
    durationMonths: 36,
    noticePeriodMonths: 3,
    baseRent: 620,
    charges: 55,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 7,
  },
  {
    scenario: "finished",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 730,
    charges: 65,
    chargesType: "PROVISION",
    depositMultiplier: 2,
    yearsAgo: 12,
  },
  {
    scenario: "finished",
    leaseType: "STUDENT",
    durationMonths: 12,
    noticePeriodMonths: 1,
    baseRent: 480,
    charges: 45,
    chargesType: "FORFAIT",
    depositMultiplier: 1,
    yearsAgo: 6,
  },
  {
    scenario: "finished",
    leaseType: "MAIN_RESIDENCE_3Y",
    durationMonths: 36,
    noticePeriodMonths: 3,
    baseRent: 660,
    charges: 60,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 5,
  },
  // Finished with historical rent adjustments
  {
    scenario: "finished-with-adjustments",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 650,
    charges: 60,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 8,
  },
  {
    scenario: "finished-with-adjustments",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 800,
    charges: 85,
    chargesType: "PROVISION",
    depositMultiplier: 2,
    yearsAgo: 11,
  },
  {
    scenario: "finished-with-adjustments",
    leaseType: "MAIN_RESIDENCE_3Y",
    durationMonths: 36,
    noticePeriodMonths: 3,
    baseRent: 580,
    charges: 50,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 9,
  },
  // Draft leases — not yet activated
  {
    scenario: "draft",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 950,
    charges: 100,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 0,
  },
  {
    scenario: "draft",
    leaseType: "MAIN_RESIDENCE_3Y",
    durationMonths: 36,
    noticePeriodMonths: 3,
    baseRent: 800,
    charges: 75,
    chargesType: "PROVISION",
    depositMultiplier: 2,
    yearsAgo: 0,
  },
  // Cancelled leases
  {
    scenario: "cancelled",
    leaseType: "MAIN_RESIDENCE_9Y",
    durationMonths: 108,
    noticePeriodMonths: 3,
    baseRent: 870,
    charges: 80,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 3,
  },
  {
    scenario: "cancelled",
    leaseType: "MAIN_RESIDENCE_3Y",
    durationMonths: 36,
    noticePeriodMonths: 3,
    baseRent: 720,
    charges: 65,
    chargesType: "FORFAIT",
    depositMultiplier: 2,
    yearsAgo: 2,
  },
];

// ─── Rent adjustment reasons ──────────────────────────────────────────────────

export const RENT_ADJUSTMENT_REASONS = [
  "Annual indexation (health index)",
  "Market rent adjustment",
  "Renovation works completed",
  "Mutual agreement — annual review",
  "Index adjustment per lease clause",
];

// ─── Meter templates ──────────────────────────────────────────────────────────

export function generateUnitMeters(unitId: number, unitIndex: number) {
  const base = (unitId * 1000 + unitIndex).toString().padStart(8, "0");
  const meters = [
    {
      type: "ELECTRICITY",
      meterNumber: `EL${base}`,
      label: "Main electricity",
      eanCode: `541445900${base.slice(0, 9)}`,
      startDate: "2018-01-01",
    },
  ];
  if (unitIndex % 2 === 0) {
    meters.push({
      type: "GAS",
      meterNumber: `GZ${base}`,
      label: "Natural gas",
      eanCode: `541234500${base.slice(0, 9)}`,
      startDate: "2018-01-01",
    });
  }
  return meters;
}

export function generateBuildingMeters(buildingId: number) {
  const base = (buildingId * 100).toString().padStart(6, "0");
  return [
    {
      type: "WATER",
      meterNumber: `WA${base}`,
      label: "Common areas water",
      installationNumber: `INS${base}`,
      customerNumber: `CUS${base}`,
      startDate: "2015-06-01",
    },
  ];
}

// ─── Boiler templates ─────────────────────────────────────────────────────────

export const BOILER_BRANDS = [
  { brand: "Vaillant", model: "ecoTEC Plus", fuelType: "GAS" },
  { brand: "Buderus", model: "Logamax U072", fuelType: "GAS" },
  { brand: "Viessmann", model: "Vitodens 100-W", fuelType: "GAS" },
  { brand: "Daikin", model: "Altherma 3", fuelType: "HEAT_PUMP" },
  { brand: "Saunier Duval", model: "ThemaFast", fuelType: "GAS" },
];

export function generateBoiler(index: number, installYear: number) {
  const brand = BOILER_BRANDS[index % BOILER_BRANDS.length];
  const installDate = `${installYear}-03-15`;
  const lastService = `${installYear + 2}-03-15`;
  const nextService = `${installYear + 3}-03-15`;
  return {
    ...brand,
    installationDate: installDate,
    lastServiceDate: lastService,
    nextServiceDate: nextService,
    serialNumber: `SN-${(index + 1) * 1234567}`,
  };
}

// ─── Date helpers ─────────────────────────────────────────────────────────────

export function isoDate(date: Date): string {
  return date.toISOString().split("T")[0];
}

export function addMonths(date: Date, months: number): Date {
  const d = new Date(date);
  d.setMonth(d.getMonth() + months);
  return d;
}

export function yearsAgo(years: number): Date {
  const d = new Date();
  d.setFullYear(d.getFullYear() - years);
  d.setDate(1);
  return d;
}
