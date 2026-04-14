// core/services/active-estate.service.ts — UC016 Phase 1
import { computed, Injectable, signal } from '@angular/core';
import { EstateSummary } from '../../models/estate.model';

/**
 * Stores the currently active estate context using Angular signals.
 * The estateId is always explicit in the URL — this service provides
 * convenience accessors for role checks throughout the UI.
 *
 * BR-UC016-06: estateId always explicit in URL — never stored server-side.
 * BR-UC016-08: VIEWER cannot mutate — checked via canEdit().
 */
@Injectable({ providedIn: 'root' })
export class ActiveEstateService {
  private readonly _activeEstate = signal<EstateSummary | null>(null);

  readonly activeEstate   = this._activeEstate.asReadonly();
  readonly activeEstateId = computed(() => this._activeEstate()?.id ?? null);
  readonly isManager      = computed(() => this._activeEstate()?.myRole === 'MANAGER');
  readonly isViewer       = computed(() => this._activeEstate()?.myRole === 'VIEWER');

  /**
   * Returns true when myRole is null — PLATFORM_ADMIN has transversal access
   * without being a member (BR-UC016-05).
   */
  readonly isPlatformAdmin = computed(() => this._activeEstate()?.myRole === null);

  /**
   * Returns true if the current user can create/edit/delete resources.
   * VIEWER cannot mutate (BR-UC016-08).
   */
  readonly canEdit = computed(() => this.isManager() || this.isPlatformAdmin());

  setActiveEstate(estate: EstateSummary): void {
    this._activeEstate.set(estate);
  }

  clearActiveEstate(): void {
    this._activeEstate.set(null);
  }
}
