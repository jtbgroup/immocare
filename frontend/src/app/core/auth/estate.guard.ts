// core/auth/estate.guard.ts — UC016 Phase 1
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { ActiveEstateService } from '../services/active-estate.service';
import { EstateService } from '../services/estate.service';

/**
 * Ensures an estate context is active before activating estate-scoped routes.
 *
 * Logic:
 *   1. If no active estate → load getMyEstates():
 *      - 1 estate          → auto-select, continue
 *      - Multiple estates  → redirect /select-estate
 *      - 0 + platformAdmin → redirect /admin/estates
 *      - 0 + normal user   → redirect /select-estate (shows "no estate" msg)
 *   2. If active estate set → verify estateId in URL matches, else /select-estate
 */
@Injectable({ providedIn: 'root' })
export class EstateGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private estateService: EstateService,
    private activeEstateService: ActiveEstateService,
    private router: Router,
  ) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const routeEstateId = route.paramMap.get('estateId');

    return this.authService.getCurrentUser().pipe(
      switchMap(user => {
        if (!user) {
          this.router.navigate(['/login']);
          return of(false);
        }

        const active = this.activeEstateService.activeEstate();

        // Active estate already set
        if (active) {
          // Check URL estateId matches active estate
          if (routeEstateId && active.id !== routeEstateId) {
            this.router.navigate(['/select-estate']);
            return of(false);
          }
          return of(true);
        }

        // No active estate — load from API
        return this.estateService.getMyEstates().pipe(
          map(estates => {
            if (estates.length === 1) {
              this.activeEstateService.setActiveEstate(estates[0]);
              // If the route has an estateId, verify it matches
              if (routeEstateId && estates[0].id !== routeEstateId) {
                this.router.navigate(['/select-estate']);
                return false;
              }
              return true;
            }

            if (estates.length > 1) {
              this.router.navigate(['/select-estate']);
              return false;
            }

            // 0 estates
            if (user.isPlatformAdmin) {
              this.router.navigate(['/admin/estates']);
            } else {
              this.router.navigate(['/select-estate']);
            }
            return false;
          }),
          catchError(() => {
            this.router.navigate(['/select-estate']);
            return of(false);
          }),
        );
      }),
    );
  }
}
