// ============================================================
// core/services/person.service.ts
// ============================================================
import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  CreatePersonRequest,
  Person,
  PersonPage,
  PersonSummary,
  UpdatePersonRequest,
} from "../../models/person.model";
import { ActiveEstateService } from "./active-estate.service";

@Injectable({ providedIn: "root" })
export class PersonService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  private get base(): string {
    return `/api/v1/estates/${this.estateId}/persons`;
  }

  /**
   * Paginated list with optional search.
   */
  getAll(
    page = 0,
    size = 20,
    sort = "lastName,asc",
    search?: string,
  ): Observable<PersonPage> {
    let params = new HttpParams()
      .set("page", page)
      .set("size", size)
      .set("sort", sort);
    if (search && search.trim()) {
      params = params.set("search", search.trim());
    }
    return this.http.get<PersonPage>(this.base, { params });
  }

  /**
   * Full person details with owned buildings/units and leases.
   */
  getPersonById(id: number): Observable<Person> {
    return this.http.get<Person>(`${this.base}/${id}`);
  }

  /**
   * Quick search for the person picker (min 2 chars, max 10 results).
   */
  searchForPicker(query: string): Observable<PersonSummary[]> {
    const params = new HttpParams().set("q", query);
    return this.http.get<PersonSummary[]>(`${this.base}/search`, { params });
  }

  /**
   * Create a new person.
   */
  create(request: CreatePersonRequest): Observable<Person> {
    return this.http.post<Person>(this.base, request);
  }

  /**
   * Update an existing person.
   */
  update(id: number, request: UpdatePersonRequest): Observable<Person> {
    return this.http.put<Person>(`${this.base}/${id}`, request);
  }

  /**
   * Delete a person. Backend returns 409 if still referenced.
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
