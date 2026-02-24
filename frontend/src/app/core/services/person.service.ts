// ============================================================
// core/services/person.service.ts
// ============================================================
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Person,
  PersonSummary,
  PersonPage,
  CreatePersonRequest,
  UpdatePersonRequest
} from '../../models/person.model';

@Injectable({ providedIn: 'root' })
export class PersonService {
  private readonly baseUrl = '/api/v1/persons';

  constructor(private http: HttpClient) {}

  /**
   * Paginated list with optional search.
   */
  getAll(
    page = 0,
    size = 20,
    sort = 'lastName,asc',
    search?: string
  ): Observable<PersonPage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<PersonPage>(this.baseUrl, { params });
  }

  /**
   * Full person details with owned buildings/units and leases.
   */
  getById(id: number): Observable<Person> {
    return this.http.get<Person>(`${this.baseUrl}/${id}`);
  }

  /**
   * Quick search for the person picker (min 2 chars, max 10 results).
   */
  searchForPicker(query: string): Observable<PersonSummary[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<PersonSummary[]>(`${this.baseUrl}/search`, { params });
  }

  /**
   * Create a new person.
   */
  create(request: CreatePersonRequest): Observable<Person> {
    return this.http.post<Person>(this.baseUrl, request);
  }

  /**
   * Update an existing person.
   */
  update(id: number, request: UpdatePersonRequest): Observable<Person> {
    return this.http.put<Person>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Delete a person. Backend returns 409 if still referenced.
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
