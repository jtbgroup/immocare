// models/estate.model.ts — UC016 Phase 1

export type EstateRole = 'MANAGER' | 'VIEWER';

export interface Estate {
  id: string;              // UUID as string
  name: string;
  description?: string;
  memberCount: number;
  buildingCount: number;
  createdAt: string;
  createdByUsername?: string;
}

export interface EstateSummary {
  id: string;
  name: string;
  description?: string;
  myRole: EstateRole | null; // null = PLATFORM_ADMIN transversal access
  buildingCount: number;
  unitCount: number;
}

export interface EstateDashboard {
  estateId: string;
  estateName: string;
  totalBuildings: number;
  totalUnits: number;
  activeLeases: number;
  pendingAlerts: {
    boiler: number;
    fireExtinguisher: number;
    leaseEnd: number;
    indexation: number;
  };
}

export interface EstateMember {
  userId: number;
  username: string;
  email: string;
  role: EstateRole;
  addedAt: string;
}

export interface CreateEstateRequest {
  name: string;
  description?: string;
  firstManagerId?: number;
}

export interface UpdateEstateRequest {
  name: string;
  description?: string;
}

export interface AddEstateMemberRequest {
  userId: number;
  role: EstateRole;
}

export interface UpdateEstateMemberRoleRequest {
  role: EstateRole;
}

export const ESTATE_ROLE_LABELS: Record<EstateRole, string> = {
  MANAGER: 'Manager',
  VIEWER:  'Viewer',
};

export const ESTATE_ROLE_COLORS: Record<EstateRole, { bg: string; text: string }> = {
  MANAGER: { bg: '#d4edda', text: '#155724' },
  VIEWER:  { bg: '#e2e3e5', text: '#383d41' },
};
