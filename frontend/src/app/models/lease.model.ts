// models/lease.model.ts

export type LeaseStatus = "DRAFT" | "ACTIVE" | "FINISHED" | "CANCELLED";
export type LeaseType =
  | "SHORT_TERM"
  | "MAIN_RESIDENCE_3Y"
  | "MAIN_RESIDENCE_6Y"
  | "MAIN_RESIDENCE_9Y"
  | "STUDENT"
  | "GLIDING"
  | "COMMERCIAL";
export type ChargesType = "FORFAIT" | "PROVISION";
export type DepositType =
  | "BLOCKED_ACCOUNT"
  | "BANK_GUARANTEE"
  | "CPAS"
  | "INSURANCE";
export type TenantRole = "PRIMARY" | "CO_TENANT" | "GUARANTOR";
export type RentField = "RENT" | "CHARGES";

export const LEASE_TYPE_LABELS: Record<LeaseType, string> = {
  SHORT_TERM: "Short-term",
  MAIN_RESIDENCE_3Y: "Main residence (3y)",
  MAIN_RESIDENCE_6Y: "Main residence (6y)",
  MAIN_RESIDENCE_9Y: "Main residence (9y)",
  STUDENT: "Student",
  GLIDING: "Gliding",
  COMMERCIAL: "Commercial",
};

export const LEASE_TYPES: LeaseType[] = Object.keys(
  LEASE_TYPE_LABELS,
) as LeaseType[];

export const LEASE_DURATION_MONTHS: Record<LeaseType, number> = {
  SHORT_TERM: 3,
  MAIN_RESIDENCE_3Y: 36,
  MAIN_RESIDENCE_6Y: 72,
  MAIN_RESIDENCE_9Y: 108,
  STUDENT: 12,
  GLIDING: 12,
  COMMERCIAL: 108,
};

export const DEFAULT_NOTICE_MONTHS: Record<LeaseType, number> = {
  SHORT_TERM: 1,
  MAIN_RESIDENCE_3Y: 3,
  MAIN_RESIDENCE_6Y: 3,
  MAIN_RESIDENCE_9Y: 3,
  STUDENT: 1,
  GLIDING: 3,
  COMMERCIAL: 6,
};

export interface LeaseTenant {
  personId: number;
  lastName: string;
  firstName: string;
  email?: string;
  gsm?: string;
  role: TenantRole;
}

export interface LeaseRentAdjustment {
  id: number;
  field: RentField;
  oldValue: number;
  newValue: number;
  reason: string;
  effectiveDate: string;
  createdAt: string;
}

export interface Lease {
  id: number;
  housingUnitId: number;
  housingUnitNumber: string;
  buildingId: number;
  buildingName: string;
  status: LeaseStatus;
  signatureDate: string;
  startDate: string;
  endDate: string;
  leaseType: LeaseType;
  durationMonths: number;
  noticePeriodMonths: number;
  monthlyRent: number;
  monthlyCharges: number;
  totalRent: number;
  chargesType: ChargesType;
  chargesDescription?: string;
  // Lease deed registration
  registrationSpf?: string;
  registrationRegion?: string;
  // Inventory (état des lieux) registration
  registrationInventorySpf?: string;
  registrationInventoryRegion?: string;
  depositAmount?: number;
  depositType?: DepositType;
  depositReference?: string;
  tenantInsuranceConfirmed: boolean;
  tenantInsuranceReference?: string;
  tenantInsuranceExpiry?: string;
  tenants: LeaseTenant[];
  rentAdjustments: LeaseRentAdjustment[];
  indexationAlertActive: boolean;
  indexationAlertDate?: string;
  endNoticeAlertActive: boolean;
  endNoticeAlertDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LeaseSummary {
  id: number;
  status: LeaseStatus;
  leaseType: LeaseType;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  monthlyCharges: number;
  totalRent: number;
  chargesType: ChargesType;
  tenantNames: string[];
  indexationAlertActive: boolean;
  indexationAlertDate?: string;
  endNoticeAlertActive: boolean;
  endNoticeAlertDate?: string;
}

export interface LeaseAlert {
  leaseId: number;
  housingUnitId: number;
  housingUnitNumber: string;
  buildingName: string;
  alertType: "INDEXATION" | "END_NOTICE";
  deadline: string;
  tenantNames: string[];
}

export interface CreateLeaseRequest {
  housingUnitId: number;
  signatureDate: string;
  startDate: string;
  endDate: string;
  leaseType: LeaseType;
  durationMonths: number;
  noticePeriodMonths: number;
  monthlyRent: number;
  monthlyCharges: number;
  chargesType: ChargesType;
  chargesDescription?: string;
  // Lease deed registration
  registrationSpf?: string;
  registrationRegion?: string;
  // Inventory (état des lieux) registration
  registrationInventorySpf?: string;
  registrationInventoryRegion?: string;
  depositAmount?: number;
  depositType?: DepositType;
  depositReference?: string;
  tenantInsuranceConfirmed: boolean;
  tenantInsuranceReference?: string;
  tenantInsuranceExpiry?: string;
  tenants: AddTenantRequest[];
}

export type UpdateLeaseRequest = Omit<
  CreateLeaseRequest,
  "housingUnitId" | "tenants"
>;

export interface AddTenantRequest {
  personId: number;
  role: TenantRole;
}

export interface ChangeLeaseStatusRequest {
  targetStatus: LeaseStatus;
}

export interface AdjustRentRequest {
  field: RentField;
  newValue: number;
  reason: string;
  effectiveDate: string;
}

export interface LeaseGlobalSummary {
  id: number;
  status: LeaseStatus;
  leaseType: LeaseType;
  housingUnitId: number;
  housingUnitNumber: string;
  buildingId: number;
  buildingName: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  monthlyCharges: number;
  totalRent: number;
  chargesType: ChargesType;
  tenantNames: string[];
  indexationAlertActive: boolean;
  indexationAlertDate?: string;
  endNoticeAlertActive: boolean;
  endNoticeAlertDate?: string;
}

export interface LeaseGlobalFilters {
  statuses: LeaseStatus[];
  leaseType: LeaseType | "";
  startDateFrom: string;
  startDateTo: string;
  endDateFrom: string;
  endDateTo: string;
  rentMin: number | null;
  rentMax: number | null;
}
