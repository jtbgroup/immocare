// models/boiler.model.ts — UC012

export type FuelType = "GAS" | "OIL" | "ELECTRIC" | "HEAT_PUMP";
export type BoilerOwnerType = "HOUSING_UNIT" | "BUILDING";
export type ServiceStatus =
  | "VALID"
  | "EXPIRING_SOON"
  | "EXPIRED"
  | "NO_SERVICE";

export const FUEL_TYPE_LABELS: Record<FuelType, string> = {
  GAS: "Gas",
  OIL: "Oil",
  ELECTRIC: "Electric",
  HEAT_PUMP: "Heat Pump",
};

export const FUEL_TYPE_ICONS: Record<FuelType, string> = {
  GAS: "🔥",
  OIL: "🛢️",
  ELECTRIC: "⚡",
  HEAT_PUMP: "♻️",
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

export const SERVICE_STATUS_LABELS: Record<ServiceStatus, string> = {
  VALID: "Valid",
  EXPIRING_SOON: "Expiring soon",
  EXPIRED: "Expired",
  NO_SERVICE: "No service recorded",
};

export const SERVICE_STATUS_CSS: Record<ServiceStatus, string> = {
  VALID: "badge-green",
  EXPIRING_SOON: "badge-warning",
  EXPIRED: "badge-danger",
  NO_SERVICE: "badge-danger",
};
