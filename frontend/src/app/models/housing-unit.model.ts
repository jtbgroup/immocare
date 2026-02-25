import { PebScore } from "./peb-score.model";
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
  ownerId?: number;
  ownerName?: string;
  roomCount?: number;
  currentMonthlyRent?: number;
  currentPebScore?: PebScore;
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
  ownerId?: number | null;
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
  ownerId?: number | null;
}
