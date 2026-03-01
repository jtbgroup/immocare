// models/boiler.model.ts — UC011

export type FuelType = 'GAS' | 'OIL' | 'ELECTRIC' | 'HEAT_PUMP';
export type BoilerOwnerType = 'HOUSING_UNIT' | 'BUILDING';

export const FUEL_TYPE_LABELS: Record<FuelType, string> = {
  GAS: 'Gas',
  OIL: 'Oil',
  ELECTRIC: 'Electric',
  HEAT_PUMP: 'Heat Pump',
};

export const FUEL_TYPE_ICONS: Record<FuelType, string> = {
  GAS: '🔥',
  OIL: '🛢️',
  ELECTRIC: '⚡',
  HEAT_PUMP: '♻️',
};

export interface BoilerDTO {
  id: number;
  ownerType: BoilerOwnerType;
  ownerId: number;
  brand: string | null;
  model: string | null;
  serialNumber: string | null;
  fuelType: FuelType;
  installationDate: string;      // ISO date
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
