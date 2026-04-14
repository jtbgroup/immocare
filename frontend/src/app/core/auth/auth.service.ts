// core/auth/auth.service.ts — updated for UC016 Phase 1
// CHANGE: AuthUser.role (string) replaced by AuthUser.isPlatformAdmin (boolean)
// The backend V017 migration drops app_user.role and adds is_platform_admin.
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { EstateService } from '../services/estate.service';
import { ActiveEstateService } from '../services/active-estate.service';

export interface AuthUser {
  username: string;
  isPlatformAdmin: boolean;
}

/**
 * Handles authentication state and communication with the backend.
 *
 * Post-login redirect logic (US101):
 *   - 1 estate          → /estates/{id}/dashboard
 *   - Multiple estates  → /select-estate
 *   - 0 + platformAdmin → /admin/estates
 *   - 0 + normal user   → /select-estate
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly loginUrl  = `${environment.apiUrl}/auth/login`;
  private readonly meUrl     = `${environment.apiUrl}/auth/me`;
  private readonly logoutUrl = `${environment.apiUrl}/auth/logout`;

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    private estateService: EstateService,
    private activeEstateService: ActiveEstateService,
  ) {}

  login(username: string, password: string): Observable<void> {
    return this.http
      .post<void>(this.loginUrl, { username, password }, { withCredentials: true })
      .pipe(
        switchMap(() => this.fetchCurrentUser()),
        switchMap(user => {
          if (!user) return of(undefined as void);
          return this.redirectAfterLogin(user);
        }),
      );
  }

  logout(): void {
    this.http
      .post<void>(this.logoutUrl, {}, { withCredentials: true })
      .pipe(catchError(() => of(null)))
      .subscribe(() => {
        this.currentUserSubject.next(null);
        this.activeEstateService.clearActiveEstate();
        this.router.navigate(['/login']);
      });
  }

  getCurrentUser(): Observable<AuthUser | null> {
    return this.http.get<AuthUser>(this.meUrl, { withCredentials: true }).pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError(() => {
        this.currentUserSubject.next(null);
        return of(null);
      }),
    );
  }

  isAuthenticated(): Observable<boolean> {
    return this.getCurrentUser().pipe(map(user => user !== null));
  }

  /**
   * Post-login redirect — US101 AC1-AC3.
   */
  private redirectAfterLogin(user: AuthUser): Observable<void> {
    return this.estateService.getMyEstates().pipe(
      tap(estates => {
        if (estates.length === 1) {
          this.activeEstateService.setActiveEstate(estates[0]);
          this.router.navigate(['/estates', estates[0].id, 'dashboard']);
        } else if (estates.length > 1) {
          this.router.navigate(['/select-estate']);
        } else if (user.isPlatformAdmin) {
          this.router.navigate(['/admin/estates']);
        } else {
          this.router.navigate(['/select-estate']);
        }
      }),
      map(() => undefined as void),
      catchError(() => {
        // Fallback on API error
        this.router.navigate([user.isPlatformAdmin ? '/admin/estates' : '/select-estate']);
        return of(undefined as void);
      }),
    );
  }

  private fetchCurrentUser(): Observable<AuthUser | null> {
    return this.getCurrentUser();
  }
}
