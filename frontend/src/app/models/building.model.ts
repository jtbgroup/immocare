/**
 * Building model interface.
 * Matches the BuildingDTO from backend.
 */
export interface Building {
  id: number;
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
