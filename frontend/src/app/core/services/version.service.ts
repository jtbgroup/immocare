import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, of } from "rxjs";
import { catchError, map } from "rxjs/operators";
import { environment } from "../../../environments/environment";

@Injectable({ providedIn: "root" })
export class VersionService {
  constructor(private http: HttpClient) {}

  getVersion(): Observable<string> {
    return this.http
      .get<{
        build?: { version?: string };
      }>(`${environment.actuatorUrl}/actuator/info`)
      .pipe(
        map((info): string => info?.build?.version ?? "N/A"),
        catchError((): Observable<string> => of("N/A")),
      );
  }
}
