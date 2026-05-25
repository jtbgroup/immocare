// models/boiler.model.ts — UC012

export type BoilerOwnerType = "HOUSING_UNIT" | "BUILDING";

// *****************
// FUEL TYPES
// *****************

export type FuelType = "GAS" | "OIL" | "ELECTRIC" | "HEAT_PUMP";

type EnforceAllFuelTypes<T extends readonly FuelType[]> = [FuelType] extends [
  T[number],
]
  ? T
  : never;

const createFuelTypes = <T extends readonly FuelType[]>(
  arr: EnforceAllFuelTypes<T>,
) => arr;

export const FUEL_TYPES = createFuelTypes([
  "GAS",
  "OIL",
  "ELECTRIC",
  "HEAT_PUMP",
] as const);

export const FUEL_TYPE_ICONS: Record<FuelType, string> = {
  GAS: "🔥",
  OIL: "🛢️",
  ELECTRIC: "⚡",
  HEAT_PUMP: "♻️",
};

// *****************
// SERVICE STATUSES
// *****************

export type ServiceStatus =
  | "VALID"
  | "EXPIRING_SOON"
  | "EXPIRED"
  | "NO_SERVICE";

type EnforceAllServiceStatuses<T extends readonly ServiceStatus[]> = [
  ServiceStatus,
] extends [T[number]]
  ? T
  : never;

const createServiceStatuses = <T extends readonly ServiceStatus[]>(
  arr: EnforceAllServiceStatuses<T>,
) => arr;

export const SERVICE_STATUSES = createServiceStatuses([
  "VALID",
  "EXPIRING_SOON",
  "EXPIRED",
  "NO_SERVICE",
] as const);

// export const SERVICE_STATUS_LABELS: Record<ServiceStatus, string> = {
//   VALID: "Valid",
//   EXPIRING_SOON: "Expiring soon",
//   EXPIRED: "Expired",
//   NO_SERVICE: "No service recorded",
// };

export const SERVICE_STATUS_CSS: Record<ServiceStatus, string> = {
  VALID: "badge-green",
  EXPIRING_SOON: "badge-warning",
  EXPIRED: "badge-danger",
  NO_SERVICE: "badge-danger",
};

export interface BoilerDTO {
  id: number;
  ownerType: BoilerOwnerType;
  ownerId: number;
  brand: string | null;
  model: string | null;
  serialNumber: string | null;
  fuelType: FuelType;
  installationDate: string; // ISO date
  outOfServiceDate: string | null; // ISO date or null
  lastServiceDate: string | null;
  nextServiceDate: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  // computed by backend
  daysUntilNextService: number | null;
  serviceAlert: boolean;
}

export interface SaveBoilerRequest {
  fuelType: FuelType;
  installationDate: string;
  outOfServiceDate?: string | null;
  brand?: string | null;
  model?: string | null;
  serialNumber?: string | null;
  lastServiceDate?: string | null;
  nextServiceDate?: string | null;
  notes?: string | null;
}

export interface BoilerServiceRecordDTO {
  id: number;
  boilerId: number;
  serviceDate: string;
  validUntil: string;
  notes: string | null;
  status: ServiceStatus;
  createdAt: string;
}

export interface AddBoilerServiceRecordRequest {
  serviceDate: string;
  validUntil?: string | null;
  notes?: string | null;
}
