// core/services/date-format.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { PlatformConfigDTO } from '../../models/platform-config.model';

const DEFAULT_FORMAT = 'dd/MM/yyyy';
const CONFIG_KEY     = 'app.date_format';

/**
 * Holds the application-wide date display format loaded from platform_config.
 *
 * Initialised via APP_INITIALIZER in main.ts so the format is available
 * synchronously before any component renders.
 *
 * Usage in templates:  {{ value | appDate }}
 * Usage in components: this.dateFormatService.getFormat()
 */
@Injectable({ providedIn: 'root' })
export class DateFormatService {

  private format$ = new BehaviorSubject<string>(DEFAULT_FORMAT);

  constructor(private http: HttpClient) {}

  /**
   * Called once by APP_INITIALIZER.
   * Returns an Observable that completes after the format is fetched.
   * Swallows errors so a backend failure never blocks the app from booting.
   */
  load(): Observable<void> {
    return this.http
      .get<PlatformConfigDTO>(`/api/v1/platform-config/${CONFIG_KEY}`)
      .pipe(
        tap(dto => this.format$.next(dto.configValue?.trim() || DEFAULT_FORMAT)),
        map(() => void 0),
        catchError(() => {
          // Keep the default — do not block startup
          return of(void 0);
        }),
      );
  }

  /**
   * Re-fetches the format from the API (call after saving settings).
   */
  reload(): void {
    this.load().subscribe();
  }

  /**
   * Synchronous accessor — safe to use after APP_INITIALIZER has run.
   */
  getFormat(): string {
    return this.format$.getValue();
  }

  /**
   * Observable — useful when a component needs to react to live changes.
   */
  getFormat$(): Observable<string> {
    return this.format$.asObservable();
  }
}
