// core/services/date-format.service.ts — UC004_ESTATE_PLACEHOLDER Phase 5
// Updated to load date format from estate-scoped config endpoint.
// Falls back to default if no active estate is set (e.g. at login time).
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { PlatformConfigDTO } from '../../models/platform-config.model';
import { ActiveEstateService } from './active-estate.service';

const DEFAULT_FORMAT = 'dd/MM/yyyy';
const CONFIG_KEY = 'app.date_format';

/**
 * Holds the application-wide date display format loaded from estate-scoped
 * platform_config (Phase 5: /api/v1/estates/{estateId}/config/settings/{key}).
 *
 * Falls back to the global endpoint when no estate is active (e.g. during
 * the initial APP_INITIALIZER boot before the user selects an estate).
 *
 * Usage in templates:  {{ value | appDate }}
 * Usage in components: this.dateFormatService.getFormat()
 */
@Injectable({ providedIn: 'root' })
export class DateFormatService {

  private format$ = new BehaviorSubject<string>(DEFAULT_FORMAT);

  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  /**
   * Called once by APP_INITIALIZER.
   * If an active estate is available, uses the estate-scoped endpoint.
   * Otherwise uses the legacy global endpoint as fallback.
   * Swallows errors so a backend failure never blocks the app from booting.
   */
  load(): Observable<void> {
    const estateId = this.activeEstateService.activeEstateId();

    const url = estateId
      ? `/api/v1/estates/${estateId}/config/settings/${CONFIG_KEY}`
      : `/api/v1/platform-config/${CONFIG_KEY}`;

    return this.http.get<PlatformConfigDTO>(url).pipe(
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
   * Uses the estate-scoped endpoint when an estate is active.
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
