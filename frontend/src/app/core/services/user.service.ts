import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  ChangePasswordRequest,
  CreateUserRequest,
  UpdateUserRequest,
  User,
} from "../../models/user.model";

const API = "/api/v1/users";

/**
 * Service for UC008 — Manage Users.
 * All methods require ROLE_ADMIN (enforced server-side).
 */
@Injectable({ providedIn: "root" })
export class UserService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<User[]> {
    return this.http.get<User[]>(API);
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${API}/${id}`);
  }

  create(req: CreateUserRequest): Observable<User> {
    return this.http.post<User>(API, req);
  }

  update(id: number, req: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${API}/${id}`, req);
  }

  changePassword(id: number, req: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${API}/${id}/password`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }
}
