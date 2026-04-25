// core/services/estate.service.ts
import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  AddEstateMemberRequest,
  CreateEstateRequest,
  Estate,
  EstateDashboard,
  EstateMember,
  EstateSummary,
  UpdateEstateMemberRoleRequest,
  UpdateEstateRequest,
} from "../../models/estate.model";
import { Page } from "../../models/page.model";

@Injectable({ providedIn: "root" })
export class EstateService {
  constructor(private http: HttpClient) {}

  // ─── Admin endpoints (PLATFORM_ADMIN only) ──────────────────────────────────

  /**
   * UC003.004 — Paginated list of all estates.
   * Only callable by PLATFORM_ADMIN.
   */
  getAllEstates(
    page: number,
    size: number,
    search?: string,
  ): Observable<Page<Estate>> {
    let params = new HttpParams().set("page", page).set("size", size);
    if (search) {
      params = params.set("search", search);
    }
    return this.http.get<Page<Estate>>("/api/v1/admin/estates", { params });
  }

  /**
   * UC003.001 — Create a new estate with an optional members list.
   * Only callable by PLATFORM_ADMIN.
   */
  createEstate(req: CreateEstateRequest): Observable<Estate> {
    return this.http.post<Estate>("/api/v1/admin/estates", req);
  }

  /**
   * UC003.003 — Delete an estate.
   * Only callable by PLATFORM_ADMIN.
   */
  deleteEstate(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/admin/estates/${id}`);
  }

  // ─── Estate-scoped endpoints (MANAGER + PLATFORM_ADMIN) ────────────────────

  /**
   * UC003.002 — Load a single estate for the edit form.
   * Accessible to MANAGER and PLATFORM_ADMIN via the estate-scoped route.
   */
  getEstateById(id: string): Observable<Estate> {
    return this.http.get<Estate>(`/api/v1/estates/${id}`);
  }

  /**
   * UC003.002 — Update estate metadata (name, description).
   * Accessible to MANAGER and PLATFORM_ADMIN via the estate-scoped route.
   */
  updateEstate(id: string, req: UpdateEstateRequest): Observable<Estate> {
    return this.http.put<Estate>(`/api/v1/estates/${id}`, req);
  }

  // ─── User endpoints (all authenticated) ────────────────────────────────────

  /**
   * UC003.012 — List estates the current user belongs to.
   */
  getMyEstates(): Observable<EstateSummary[]> {
    return this.http.get<EstateSummary[]>("/api/v1/estates/mine");
  }

  /**
   * UC003.011 — Load the estate dashboard.
   */
  getDashboard(estateId: string): Observable<EstateDashboard> {
    return this.http.get<EstateDashboard>(
      `/api/v1/estates/${estateId}/dashboard`,
    );
  }

  // ─── Member endpoints (MANAGER + PLATFORM_ADMIN) ───────────────────────────

  /**
   * UC003.006 — List all members of an estate.
   */
  getMembers(estateId: string): Observable<EstateMember[]> {
    return this.http.get<EstateMember[]>(`/api/v1/estates/${estateId}/members`);
  }

  /**
   * UC003.007 — Add a member to an estate.
   */
  addMember(
    estateId: string,
    req: AddEstateMemberRequest,
  ): Observable<EstateMember> {
    return this.http.post<EstateMember>(
      `/api/v1/estates/${estateId}/members`,
      req,
    );
  }

  /**
   * UC003.008 — Update a member's role.
   */
  updateMemberRole(
    estateId: string,
    userId: number,
    req: UpdateEstateMemberRoleRequest,
  ): Observable<EstateMember> {
    return this.http.patch<EstateMember>(
      `/api/v1/estates/${estateId}/members/${userId}`,
      req,
    );
  }

  /**
   * UC003.009 — Remove a member from an estate.
   */
  removeMember(estateId: string, userId: number): Observable<void> {
    return this.http.delete<void>(
      `/api/v1/estates/${estateId}/members/${userId}`,
    );
  }
}
