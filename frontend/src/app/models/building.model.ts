/**
 * Building model — UC004_ESTATE_PLACEHOLDER Phase 2: estateId added.
 * Matches BuildingDTO from backend after V018 migration.
 */
export interface Building {
  id: number;
  estateId: string;           // UUID — Phase 2: every building belongs to an estate
  name: string;
  streetAddress: string;
  postalCode: string;
  city: string;
  country: string;
  ownerId?: number;
  ownerName?: string;
  createdByUsername?: string;
  createdAt?: string;
  updatedAt?: string;
  unitCount: number;
}

/**
 * Request interface for creating a building.
 * estateId is implicit from the URL path — never sent in body.
 */
export interface CreateBuildingRequest {
  name: string;
  streetAddress: string;
  postalCode: string;
  city: string;
  country: string;
  ownerId?: number | null;
}

/**
 * Request interface for updating a building.
 */
export interface UpdateBuildingRequest {
  name: string;
  streetAddress: string;
  postalCode: string;
  city: string;
  country: string;
  ownerId?: number | null;
}

/**
 * Paginated response interface.
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
