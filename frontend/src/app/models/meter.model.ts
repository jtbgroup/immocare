export type MeterType = 'WATER' | 'GAS' | 'ELECTRICITY';
export type MeterOwnerType = 'HOUSING_UNIT' | 'BUILDING';
export type ReplacementReason = 'BROKEN' | 'END_OF_LIFE' | 'UPGRADE' | 'CALIBRATION_ISSUE' | 'OTHER';

export const ALL_METER_TYPES: MeterType[] = ['WATER', 'GAS', 'ELECTRICITY'];
export const ALL_REPLACEMENT_REASONS: ReplacementReason[] = [
  'BROKEN', 'END_OF_LIFE', 'UPGRADE', 'CALIBRATION_ISSUE', 'OTHER',
];

export interface MeterDTO {
  id: number;
  type: MeterType;
  meterNumber: string;
  label?: string | null;          // ← NEW optional label
  eanCode?: string | null;
  installationNumber?: string | null;
  customerNumber?: string | null;
  ownerType: MeterOwnerType;
  ownerId: number;
  startDate: string;
  endDate?: string | null;
  status: 'ACTIVE' | 'CLOSED';
  createdAt: string;
}

export interface AddMeterRequest {
  type: MeterType;
  meterNumber: string;
  label?: string | null;          // ← NEW
  eanCode?: string | null;
  installationNumber?: string | null;
  customerNumber?: string | null;
  startDate: string;
}

export interface ReplaceMeterRequest {
  newMeterNumber: string;
  newLabel?: string | null;       // ← NEW
  newEanCode?: string | null;
  newInstallationNumber?: string | null;
  newCustomerNumber?: string | null;
  newStartDate: string;
  reason?: ReplacementReason;
}

export interface RemoveMeterRequest {
  endDate: string;
}

export const METER_TYPE_LABELS: Record<MeterType, string> = {
  WATER: 'Water',
  GAS: 'Gas',
  ELECTRICITY: 'Electricity',
};

export const METER_TYPE_ICONS: Record<MeterType, string> = {
  WATER: 'water_drop',
  GAS: 'local_fire_department',
  ELECTRICITY: 'bolt',
};

export const REPLACEMENT_REASON_LABELS: Record<ReplacementReason, string> = {
  BROKEN: 'Broken',
  END_OF_LIFE: 'End of life',
  UPGRADE: 'Upgrade',
  CALIBRATION_ISSUE: 'Calibration issue',
  OTHER: 'Other',
};

export function meterDurationMonths(startDate: string, endDate?: string | null): number {
  const start = new Date(startDate);
  const end = endDate ? new Date(endDate) : new Date();
  return Math.max(
    0,
    (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth()),
  );
}
