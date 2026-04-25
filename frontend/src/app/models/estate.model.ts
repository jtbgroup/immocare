// models/estate.model.ts

export type EstateRole = "MANAGER" | "VIEWER";

export interface Estate {
  id: string; // UUID
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
  myRole: EstateRole | null; // null = PLATFORM_ADMIN transversal
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

// ─── Member input (used when creating an estate) ──────────────────────────────

export interface EstateMemberInput {
  userId: number;
  role: EstateRole;
}

// ─── Request DTOs ─────────────────────────────────────────────────────────────

export interface CreateEstateRequest {
  name: string;
  description?: string;
  members?: EstateMemberInput[]; // replaces firstManagerId — if non-empty, ≥1 MANAGER required
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

// ─── Display helpers ──────────────────────────────────────────────────────────

export const ESTATE_ROLE_LABELS: Record<EstateRole, string> = {
  MANAGER: "Manager",
  VIEWER: "Viewer",
};

export const ESTATE_ROLE_COLORS: Record<
  EstateRole,
  { bg: string; text: string }
> = {
  MANAGER: { bg: "#d4edda", text: "#155724" },
  VIEWER: { bg: "#e2e3e5", text: "#383d41" },
};
