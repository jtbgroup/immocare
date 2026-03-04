import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import {
  AddRevisionRequest,
  FireExtinguisher,
  SaveFireExtinguisherRequest,
} from "../../models/fire-extinguisher.model";

@Injectable({ providedIn: "root" })
export class FireExtinguisherService {
  private readonly api = "/api/v1";

  constructor(private http: HttpClient) {}

  getByBuilding(buildingId: number): Observable<FireExtinguisher[]> {
    return this.http.get<FireExtinguisher[]>(
      `${this.api}/buildings/${buildingId}/fire-extinguishers`,
    );
  }

  getById(id: number): Observable<FireExtinguisher> {
    return this.http.get<FireExtinguisher>(`${this.api}/fire-extinguishers/${id}`);
  }

  create(
    buildingId: number,
    req: SaveFireExtinguisherRequest,
  ): Observable<FireExtinguisher> {
    return this.http.post<FireExtinguisher>(
      `${this.api}/buildings/${buildingId}/fire-extinguishers`,
      req,
    );
  }

  update(id: number, req: SaveFireExtinguisherRequest): Observable<FireExtinguisher> {
    return this.http.put<FireExtinguisher>(
      `${this.api}/fire-extinguishers/${id}`,
      req,
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/fire-extinguishers/${id}`);
  }

  addRevision(extId: number, req: AddRevisionRequest): Observable<FireExtinguisher> {
    return this.http.post<FireExtinguisher>(
      `${this.api}/fire-extinguishers/${extId}/revisions`,
      req,
    );
  }

  deleteRevision(extId: number, revId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.api}/fire-extinguishers/${extId}/revisions/${revId}`,
    );
  }
}
