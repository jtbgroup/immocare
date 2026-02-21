import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class VersionService {

  constructor(private http: HttpClient) {}

  getVersion(): Observable<string> {
    return this.http.get<any>('/actuator/info').pipe(
      map(info => info?.build?.version ?? 'N/A'),
      catchError(() => of('N/A'))
    );
  }
}
