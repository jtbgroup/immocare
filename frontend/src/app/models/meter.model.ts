// UC008 - Manage Meters

export type MeterType = 'WATER' | 'GAS' | 'ELECTRICITY';
export type MeterOwnerType = 'HOUSING_UNIT' | 'BUILDING';
export type MeterStatus = 'ACTIVE' | 'CLOSED';
export type ReplacementReason = 'BROKEN' | 'END_OF_LIFE' | 'UPGRADE' | 'CALIBRATION_ISSUE' | 'OTHER';

export interface MeterDTO {
  id: number;
  type: MeterType;
  meterNumber: string;
  eanCode?: string | null;
  installationNumber?: string | null;
  customerNumber?: string | null;
  ownerType: MeterOwnerType;
  ownerId: number;
  startDate: string;       // ISO date: YYYY-MM-DD
  endDate?: string | null; // null = active
  status: MeterStatus;     // computed by backend
  createdAt: string;       // ISO datetime
}

export interface AddMeterRequest {
  type: MeterType;
  meterNumber: string;
  eanCode?: string | null;
  installationNumber?: string | null;
  customerNumber?: string | null;
  startDate: string; // ISO date
}

export interface ReplaceMeterRequest {
  newMeterNumber: string;
  newEanCode?: string | null;
  newInstallationNumber?: string | null;
  newCustomerNumber?: string | null;
  newStartDate: string; // ISO date
  reason?: ReplacementReason | null;
}

export interface RemoveMeterRequest {
  endDate: string; // ISO date
}

// ─── Display helpers ─────────────────────────────────────────────────────────

export const METER_TYPE_LABELS: Record<MeterType, string> = {
  WATER:       'Water',
  GAS:         'Gas',
  ELECTRICITY: 'Electricity',
};

export const METER_TYPE_ICONS: Record<MeterType, string> = {
  WATER:       'water_drop',
  GAS:         'local_fire_department',
  ELECTRICITY: 'bolt',
};

export const REPLACEMENT_REASON_LABELS: Record<ReplacementReason, string> = {
  BROKEN:            'Broken',
  END_OF_LIFE:       'End of life',
  UPGRADE:           'Upgrade',
  CALIBRATION_ISSUE: 'Calibration issue',
  OTHER:             'Other',
};

export const ALL_METER_TYPES: MeterType[] = ['WATER', 'GAS', 'ELECTRICITY'];
export const ALL_REPLACEMENT_REASONS: ReplacementReason[] = [
  'BROKEN', 'END_OF_LIFE', 'UPGRADE', 'CALIBRATION_ISSUE', 'OTHER',
];

/**
 * Computes the duration in months between startDate and endDate (or today if active).
 */
export function meterDurationMonths(meter: MeterDTO): number {
  const start = new Date(meter.startDate);
  const end   = meter.endDate ? new Date(meter.endDate) : new Date();
  return (end.getFullYear() - start.getFullYear()) * 12
      + (end.getMonth() - start.getMonth());
}
