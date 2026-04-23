// core/auth/platform-admin.guard.ts — UC004_ESTATE_PLACEHOLDER Phase 1
import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { AuthService } from './auth.service';

/**
 * Protects routes that require PLATFORM_ADMIN access.
 * Redirects to '/' when the authenticated user is not a platform admin.
 */
@Injectable({ providedIn: 'root' })
export class PlatformAdminGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): Observable<boolean> {
    return this.authService.getCurrentUser().pipe(
      map(user => !!user?.isPlatformAdmin),
      tap(isPlatformAdmin => {
        if (!isPlatformAdmin) {
          this.router.navigate(['/']);
        }
      }),
    );
  }
}
