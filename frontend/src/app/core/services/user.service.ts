import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  ChangePasswordRequest,
} from '../../models/user.model';

const API = '/api/v1/users';

/**
 * Service for UC007 — Manage Users.
 * All methods require ROLE_ADMIN (enforced server-side).
 */
@Injectable({ providedIn: 'root' })
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
