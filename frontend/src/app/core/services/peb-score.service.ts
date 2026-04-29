import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  CreatePebScoreRequest,
  PebImprovementDTO,
  PebScoreDTO,
} from "../../models/peb-score.model";
import { ActiveEstateService } from "./active-estate.service";

@Injectable({ providedIn: "root" })
export class PebScoreService {
  // private base = "/api/v1/housing-units";

  constructor(
    private http: HttpClient,
    private readonly activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id)
      throw new Error(
        "No active estate — cannot call MeterService without an estate context.",
      );
    return id;
  }

  private get api(): string {
    return `/api/v1/estates/${this.estateId}`;
  }

  addScore(
    unitId: number,
    request: CreatePebScoreRequest,
  ): Observable<PebScoreDTO> {
    return this.http.post<PebScoreDTO>(
      `${this.api}/${unitId}/peb-scores`,
      request,
    );
  }

  updateScore(
    unitId: number,
    scoreId: number,
    request: CreatePebScoreRequest,
  ): Observable<PebScoreDTO> {
    return this.http.put<PebScoreDTO>(
      `${this.api}/${unitId}/peb-scores/${scoreId}`,
      request,
    );
  }

  deleteScore(unitId: number, scoreId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.api}/${unitId}/peb-scores/${scoreId}`,
    );
  }

  getHistory(unitId: number): Observable<PebScoreDTO[]> {
    return this.http.get<PebScoreDTO[]>(`${this.api}/${unitId}/peb-scores`);
  }

  getCurrentScore(unitId: number): Observable<PebScoreDTO> {
    return this.http.get<PebScoreDTO>(
      `${this.api}/${unitId}/peb-scores/current`,
    );
  }

  getImprovements(unitId: number): Observable<PebImprovementDTO> {
    return this.http.get<PebImprovementDTO>(
      `${this.api}/${unitId}/peb-scores/improvements`,
    );
  }
}
