// UC003 - Manage Rooms

export type RoomType =
  | 'LIVING_ROOM'
  | 'BEDROOM'
  | 'KITCHEN'
  | 'BATHROOM'
  | 'TOILET'
  | 'HALLWAY'
  | 'STORAGE'
  | 'OFFICE'
  | 'DINING_ROOM'
  | 'OTHER';

export const ROOM_TYPE_LABELS: Record<RoomType, string> = {
  LIVING_ROOM: 'Living Room',
  BEDROOM: 'Bedroom',
  KITCHEN: 'Kitchen',
  BATHROOM: 'Bathroom',
  TOILET: 'Toilet',
  HALLWAY: 'Hallway',
  STORAGE: 'Storage',
  OFFICE: 'Office',
  DINING_ROOM: 'Dining Room',
  OTHER: 'Other',
};

export const ALL_ROOM_TYPES: RoomType[] = [
  'LIVING_ROOM', 'BEDROOM', 'KITCHEN', 'BATHROOM',
  'TOILET', 'HALLWAY', 'STORAGE', 'OFFICE', 'DINING_ROOM', 'OTHER',
];

export interface Room {
  id: number;
  housingUnitId: number;
  roomType: RoomType;
  approximateSurface: number;
  createdAt: string;
  updatedAt: string;
}

export interface RoomListResponse {
  rooms: Room[];
  totalSurface: number;
}

export interface CreateRoomRequest {
  roomType: RoomType;
  approximateSurface: number;
}

export interface UpdateRoomRequest {
  roomType: RoomType;
  approximateSurface: number;
}

export interface BatchRoomEntry {
  roomType: RoomType | '';
  approximateSurface: number | null;
}

export interface BatchCreateRoomsRequest {
  rooms: CreateRoomRequest[];
}
