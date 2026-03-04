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
  identificationNumber: string;
  notes: string | null;
  revisions: FireExtinguisherRevision[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveFireExtinguisherRequest {
  identificationNumber: string;
  unitId: number | null;
  notes: string | null;
}

export interface AddRevisionRequest {
  revisionDate: string; // ISO date 'yyyy-MM-dd'
  notes: string | null;
}
