export interface RentHistory {
  id: number;
  housingUnitId: number;
  monthlyRent: number;
  effectiveFrom: string;   // ISO date string
  effectiveTo: string | null;
  notes: string | null;
  createdAt: string;
  isCurrent: boolean;
  durationMonths: number;
}

export interface SetRentRequest {
  monthlyRent: number;
  effectiveFrom: string;
  notes?: string | null;
}

/** Computed change between two rent records (US022 AC3, US024). */
export interface RentChange {
  amount: number;         // absolute € change
  percentage: number;     // % change rounded to 2 decimals
  isIncrease: boolean;
}

export function computeRentChange(from: number, to: number): RentChange {
  const amount = to - from;
  const percentage = Math.round((amount / from) * 10000) / 100;
  return { amount, percentage, isIncrease: amount >= 0 };
}
