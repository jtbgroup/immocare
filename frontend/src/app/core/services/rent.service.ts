import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { ActiveEstateService } from "./active-estate.service";
import { RentHistory, SetRentRequest } from "../../models/rent.model";

@Injectable({ providedIn: "root" })
export class RentService {
  private baseUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id) throw new Error("No active estate — cannot call RentService without an estate context.");
    return id;
  }

  private unitBase(unitId: number): string {
    return `${this.baseUrl}/api/v1/estates/${this.estateId}/housing-units/${unitId}/rents`;
  }

  getRentHistory(unitId: number): Observable<RentHistory[]> {
    return this.http.get<RentHistory[]>(this.unitBase(unitId));
  }

  getCurrentRent(unitId: number): Observable<RentHistory | null> {
    return this.http.get<RentHistory | null>(`${this.unitBase(unitId)}/current`);
  }

  addRent(unitId: number, request: SetRentRequest): Observable<RentHistory> {
    return this.http.post<RentHistory>(this.unitBase(unitId), request);
  }

  updateRent(
    unitId: number,
    rentId: number,
    request: SetRentRequest,
  ): Observable<RentHistory> {
    return this.http.put<RentHistory>(
      `${this.unitBase(unitId)}/${rentId}`,
      request,
    );
  }

  deleteRent(unitId: number, rentId: number): Observable<void> {
    return this.http.delete<void>(`${this.unitBase(unitId)}/${rentId}`);
  }
}