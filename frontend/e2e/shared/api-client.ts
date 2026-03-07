/**
 * Shared API client — used by both E2E fixtures and the dev seed script.
 * All calls go through the REST API, never direct DB access.
 */

const BASE_URL = process.env["API_URL"] ?? "http://localhost:8080/api/v1";

// ─── Auth ─────────────────────────────────────────────────────────────────────

let _sessionCookie: string | null = null;

export async function login(
  username = "admin",
  password = "admin123",
): Promise<string> {
  const body = new URLSearchParams();
  body.set("username", username);
  body.set("password", password);

  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: body.toString(),
  });
  if (!res.ok) throw new Error(`Login failed: ${res.status}`);

  const cookie = res.headers.get("set-cookie");
  if (!cookie)
    throw new Error("No session cookie returned — is the backend running?");
  _sessionCookie = cookie.split(";")[0];
  return _sessionCookie;
}

function headers(): Record<string, string> {
  if (!_sessionCookie) throw new Error("Not logged in — call login() first");
  return {
    "Content-Type": "application/json",
    Cookie: _sessionCookie,
  };
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: "POST",
    headers: headers(),
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`POST ${path} failed [${res.status}]: ${text}`);
  }
  return res.json();
}

async function patch<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: "PATCH",
    headers: headers(),
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`PATCH ${path} failed [${res.status}]: ${text}`);
  }
  return res.json();
}

// ─── Persons ──────────────────────────────────────────────────────────────────

export interface PersonPayload {
  firstName: string;
  lastName: string;
  email?: string;
  gsm?: string;
  birthDate?: string;
  birthPlace?: string;
  nationalId?: string;
  streetAddress?: string;
  postalCode?: string;
  city?: string;
  country?: string;
}

export async function createPerson(
  data: PersonPayload,
): Promise<{ id: number }> {
  return post("/persons", { country: "Belgium", ...data });
}

// ─── Buildings ────────────────────────────────────────────────────────────────

export interface BuildingPayload {
  name: string;
  streetAddress: string;
  postalCode: string;
  city: string;
  country?: string;
  ownerId?: number;
}

export async function createBuilding(
  data: BuildingPayload,
): Promise<{ id: number }> {
  return post("/buildings", { country: "Belgium", ...data });
}

// ─── Housing Units ────────────────────────────────────────────────────────────

export interface HousingUnitPayload {
  buildingId: number;
  unitNumber: string;
  floor: number;
  totalSurface?: number;
  hasTerrace?: boolean;
  terraceSurface?: number;
  terraceOrientation?: string;
  hasGarden?: boolean;
  gardenSurface?: number;
  gardenOrientation?: string;
  ownerId?: number;
}

export async function createHousingUnit(
  data: HousingUnitPayload,
): Promise<{ id: number }> {
  return post("/units", data);
}

// ─── Rooms ────────────────────────────────────────────────────────────────────

export interface RoomEntry {
  roomType: string;
  approximateSurface: number;
}

export async function createRooms(
  unitId: number,
  rooms: RoomEntry[],
): Promise<void> {
  await post(`/housing-units/${unitId}/rooms/batch`, { rooms });
}

// ─── Leases ───────────────────────────────────────────────────────────────────

export interface TenantEntry {
  personId: number;
  role: "PRIMARY" | "CO_TENANT" | "GUARANTOR";
}

export interface LeasePayload {
  housingUnitId: number;
  signatureDate: string;
  startDate: string;
  endDate: string;
  leaseType: string;
  durationMonths: number;
  noticePeriodMonths: number;
  monthlyRent: number;
  monthlyCharges: number;
  chargesType: string;
  chargesDescription?: string;
  depositAmount?: number;
  depositType?: string;
  tenantInsuranceConfirmed: boolean;
  tenants: TenantEntry[];
}

export async function createLease(
  data: LeasePayload,
  activate = false,
): Promise<{ id: number }> {
  return post(`/leases?activate=${activate}`, data);
}

export async function changeLeaseStatus(
  leaseId: number,
  targetStatus: "ACTIVE" | "FINISHED" | "CANCELLED",
): Promise<void> {
  await patch(`/leases/${leaseId}/status`, { targetStatus });
}

export async function adjustRent(
  leaseId: number,
  field: "RENT" | "CHARGES",
  newValue: number,
  reason: string,
  effectiveDate: string,
): Promise<void> {
  await post(`/leases/${leaseId}/rent-adjustments`, {
    field,
    newValue,
    reason,
    effectiveDate,
  });
}

// ─── Meters ───────────────────────────────────────────────────────────────────

export interface MeterPayload {
  type: string;
  meterNumber: string;
  label?: string;
  eanCode?: string;
  installationNumber?: string;
  customerNumber?: string;
  startDate: string;
}

export async function createUnitMeter(
  unitId: number,
  data: MeterPayload,
): Promise<{ id: number }> {
  return post(`/housing-units/${unitId}/meters`, data);
}

export async function createBuildingMeter(
  buildingId: number,
  data: MeterPayload,
): Promise<{ id: number }> {
  return post(`/buildings/${buildingId}/meters`, data);
}

// ─── Boilers ──────────────────────────────────────────────────────────────────

export interface BoilerPayload {
  fuelType: string;
  installationDate: string;
  brand?: string;
  model?: string;
  serialNumber?: string;
  lastServiceDate?: string;
  nextServiceDate?: string;
  notes?: string;
}

export async function createUnitBoiler(
  unitId: number,
  data: BoilerPayload,
): Promise<{ id: number }> {
  return post(`/housing-units/${unitId}/boilers`, data);
}

export async function createBuildingBoiler(
  buildingId: number,
  data: BoilerPayload,
): Promise<{ id: number }> {
  return post(`/buildings/${buildingId}/boilers`, data);
}

// ─── Bank accounts ────────────────────────────────────────────────────────────

export interface BankAccountPayload {
  label: string;
  accountNumber: string;
  type: "CURRENT" | "SAVINGS";
  isActive: boolean;
}

export async function createBankAccount(
  data: BankAccountPayload,
): Promise<{ id: number }> {
  return post("/bank-accounts", data);
}

// ─── Tag subcategories (read) ─────────────────────────────────────────────────

export interface SubcategoryDTO {
  id: number;
  name: string;
  categoryName: string;
  direction: string;
}

export async function fetchSubcategories(): Promise<SubcategoryDTO[]> {
  const res = await fetch(`${BASE_URL}/tag-subcategories`, {
    headers: headers(),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`GET /tag-subcategories failed [${res.status}]: ${text}`);
  }
  return res.json();
}

// ─── Transactions ─────────────────────────────────────────────────────────────

export interface TransactionPayload {
  direction: "INCOME" | "EXPENSE";
  transactionDate: string;
  valueDate?: string;
  accountingMonth: string;
  amount: number;
  description?: string;
  counterpartyName?: string;
  counterpartyAccount?: string;
  bankAccountId?: number;
  subcategoryId?: number;
  leaseId?: number;
  housingUnitId?: number;
  buildingId?: number;
}

export async function createTransaction(
  data: TransactionPayload,
): Promise<{ id: number; reference: string }> {
  return post("/transactions", data);
}
