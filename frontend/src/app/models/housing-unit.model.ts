export type Orientation = "N" | "S" | "E" | "W" | "NE" | "NW" | "SE" | "SW";

export const ORIENTATIONS: Orientation[] = [
  "N",
  "S",
  "E",
  "W",
  "NE",
  "NW",
  "SE",
  "SW",
];

export interface HousingUnit {
  id: number;
  buildingId: number;
  buildingName?: string;
  unitNumber: string;
  floor: number;
  landingNumber?: string;
  totalSurface?: number;
  hasTerrace: boolean;
  terraceSurface?: number;
  terraceOrientation?: Orientation;
  hasGarden: boolean;
  gardenSurface?: number;
  gardenOrientation?: Orientation;
  ownerName?: string;
  roomCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateHousingUnitRequest {
  buildingId: number;
  unitNumber: string;
  floor: number;
  landingNumber?: string;
  totalSurface?: number;
  hasTerrace: boolean;
  terraceSurface?: number;
  terraceOrientation?: Orientation;
  hasGarden: boolean;
  gardenSurface?: number;
  gardenOrientation?: Orientation;
  ownerName?: string;
}

export interface UpdateHousingUnitRequest {
  unitNumber: string;
  floor: number;
  landingNumber?: string;
  totalSurface?: number;
  hasTerrace: boolean;
  terraceSurface?: number;
  terraceOrientation?: Orientation;
  hasGarden: boolean;
  gardenSurface?: number;
  gardenOrientation?: Orientation;
  ownerName?: string;
}
