// ============================================================
// models/person.model.ts
// ============================================================

export interface Person {
  id: number;
  lastName: string;
  firstName: string;
  birthDate?: string;         // ISO date string
  birthPlace?: string;
  nationalId?: string;
  gsm?: string;
  email?: string;
  streetAddress?: string;
  postalCode?: string;
  city?: string;
  country?: string;
  isOwner: boolean;
  isTenant: boolean;
  ownedBuildings?: OwnedBuilding[];
  ownedUnits?: OwnedUnit[];
  leases?: TenantLease[];
  createdAt: string;
  updatedAt: string;
}

export interface PersonSummary {
  id: number;
  lastName: string;
  firstName: string;
  city?: string;
  nationalId?: string;
  isOwner: boolean;
  isTenant: boolean;
}

export interface OwnedBuilding {
  id: number;
  name: string;
  city: string;
}

export interface OwnedUnit {
  id: number;
  unitNumber: string;
  buildingId: number;
  buildingName: string;
}

export interface TenantLease {
  leaseId: number;
  unitId: number;
  unitNumber: string;
  buildingName: string;
  status: string;
}

export interface CreatePersonRequest {
  lastName: string;
  firstName: string;
  birthDate?: string;
  birthPlace?: string;
  nationalId?: string;
  gsm?: string;
  email?: string;
  streetAddress?: string;
  postalCode?: string;
  city?: string;
  country?: string;
}

export type UpdatePersonRequest = CreatePersonRequest;

export interface PersonPage {
  content: PersonSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
