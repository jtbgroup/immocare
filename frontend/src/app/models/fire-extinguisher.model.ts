export interface FireExtinguisherRevision {
  id: number;
  fireExtinguisherId: number;
  revisionDate: string; // ISO date 'yyyy-MM-dd'
  notes: string | null;
  createdAt: string;
}

export interface FireExtinguisher {
  id: number;
  buildingId: number;
  unitId: number | null;
  unitNumber: string | null;
  ownerType: "BUILDING" | "HOUSING_UNIT";
  ownerId: number;
  identificationNumber: string;
  notes: string | null;
  revisions: FireExtinguisherRevision[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveFireExtinguisherRequest {
  identificationNumber: string;
  unitId?: number | null;
  notes: string | null;
}

export type EquipmentOwnerType = "BUILDING" | "HOUSING_UNIT";

export interface AddRevisionRequest {
  revisionDate: string; // ISO date 'yyyy-MM-dd'
  notes: string | null;
}
