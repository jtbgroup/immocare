/**
 * Models for UC006 — Water Meter Management (US026-US030).
 */

export interface WaterMeterHistory {
  id: number;
  housingUnitId: number;
  meterNumber: string;
  meterLocation: string | null;
  installationDate: string;   // ISO date string YYYY-MM-DD
  removalDate: string | null; // null = active meter
  createdAt: string;
  isActive: boolean;
  durationMonths: number;
  status: 'Active' | 'Replaced';
}

/** US026 — Assign first meter */
export interface AssignMeterRequest {
  meterNumber: string;
  meterLocation?: string | null;
  installationDate: string; // ISO date string
}

/** US027 — Replace meter */
export interface ReplaceMeterRequest {
  newMeterNumber: string;
  newMeterLocation?: string | null;
  newInstallationDate: string; // ISO date string
  reason?: ReplacementReason;
}

/** US029 — Remove meter */
export interface RemoveMeterRequest {
  removalDate: string; // ISO date string
}

export type ReplacementReason =
  | 'BROKEN'
  | 'END_OF_LIFE'
  | 'UPGRADE'
  | 'CALIBRATION_ISSUE'
  | 'OTHER';

export const REPLACEMENT_REASON_LABELS: Record<ReplacementReason, string> = {
  BROKEN: 'Broken',
  END_OF_LIFE: 'End of life',
  UPGRADE: 'Upgrade',
  CALIBRATION_ISSUE: 'Calibration issue',
  OTHER: 'Other',
};
