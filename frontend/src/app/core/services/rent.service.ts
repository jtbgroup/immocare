import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { RentHistory, SetRentRequest } from "../../models/rent.model";

@Injectable({ providedIn: "root" })
export class RentService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getRentHistory(unitId: number): Observable<RentHistory[]> {
    return this.http.get<RentHistory[]>(
      `${this.baseUrl}/housing-units/${unitId}/rents`,
    );
  }

  getCurrentRent(unitId: number): Observable<RentHistory | null> {
    return this.http.get<RentHistory | null>(
      `${this.baseUrl}/housing-units/${unitId}/rents/current`,
    );
  }

  addRent(unitId: number, request: SetRentRequest): Observable<RentHistory> {
    return this.http.post<RentHistory>(
      `${this.baseUrl}/housing-units/${unitId}/rents`,
      request,
    );
  }

  updateRent(
    unitId: number,
    rentId: number,
    request: SetRentRequest,
  ): Observable<RentHistory> {
    return this.http.put<RentHistory>(
      `${this.baseUrl}/housing-units/${unitId}/rents/${rentId}`,
      request,
    );
  }

  deleteRent(unitId: number, rentId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/housing-units/${unitId}/rents/${rentId}`,
    );
  }
}
