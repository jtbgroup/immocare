import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AuthUser {
  username: string;
  role: string;
}

/**
 * Handles authentication state and communication with the backend.
 *
 * Login flow  : POST /api/v1/auth/login  (Spring Security form endpoint)
 * Logout flow : POST /api/v1/auth/logout
 * State check : GET  /api/v1/auth/me
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly loginUrl  = `${environment.apiUrl}/auth/login`;
  private readonly meUrl     = `${environment.apiUrl}/auth/me`;
  private readonly logoutUrl = `${environment.apiUrl}/auth/logout`;

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<void> {
    const body = new URLSearchParams();
    body.set('username', username);
    body.set('password', password);

    return this.http.post<void>(this.loginUrl, body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      withCredentials: true
    }).pipe(
      tap(() => this.fetchCurrentUser().subscribe())
    );
  }

  logout(): void {
    this.http.post<void>(this.logoutUrl, {}, { withCredentials: true })
      .pipe(catchError(() => of(null)))
      .subscribe(() => {
        this.currentUserSubject.next(null);
        this.router.navigate(['/login']);
      });
  }

  getCurrentUser(): Observable<AuthUser | null> {
    return this.http.get<AuthUser>(this.meUrl, { withCredentials: true })
      .pipe(
        tap(user => this.currentUserSubject.next(user)),
        catchError(() => {
          this.currentUserSubject.next(null);
          return of(null);
        })
      );
  }

  isAuthenticated(): Observable<boolean> {
    return this.getCurrentUser().pipe(map(user => user !== null));
  }

  private fetchCurrentUser(): Observable<AuthUser | null> {
    return this.getCurrentUser();
  }
}
