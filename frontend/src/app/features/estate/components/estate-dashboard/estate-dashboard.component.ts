// features/estate/components/estate-dashboard/estate-dashboard.component.ts
// UC003 — Manage Estates (Phase 6 + estate-manager edit button)
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ActiveEstateService } from '../../../../core/services/active-estate.service';
import { EstateService } from '../../../../core/services/estate.service';
import { EstateDashboard } from '../../../../models/estate.model';

interface QuickAccessCard {
  icon: string;
  label: string;
  route: any[];
  description: string;
}

@Component({
  selector: 'app-estate-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './estate-dashboard.component.html',
  styleUrls: ['./estate-dashboard.component.scss'],
})
export class EstateDashboardComponent implements OnInit, OnDestroy {
  dashboard: EstateDashboard | null = null;
  loading = true;
  error: string | null = null;
  estateId = '';

  private destroy$ = new Subject<void>();

  constructor(
    private estateService: EstateService,
    readonly activeEstateService: ActiveEstateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        this.estateId = params.get('estateId') ?? '';
        this.loadDashboard();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDashboard(): void {
    this.loading = true;
    this.estateService
      .getDashboard(this.estateId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.dashboard = data;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load dashboard.';
          this.loading = false;
        },
      });
  }

  get totalAlerts(): number {
    if (!this.dashboard) return 0;
    const p = this.dashboard.pendingAlerts;
    return p.boiler + p.fireExtinguisher + p.leaseEnd + p.indexation;
  }

  /**
   * True when the current user can edit the estate metadata and manage members.
   * MANAGER and PLATFORM_ADMIN have this capability; VIEWERs do not.
   */
  get canManageEstate(): boolean {
    return (
      this.activeEstateService.isManager() ||
      this.activeEstateService.isPlatformAdmin()
    );
  }

  get canManageMembers(): boolean {
    return this.canManageEstate;
  }

  /**
   * Navigates to the estate edit form.
   * PLATFORM_ADMIN uses the admin route; estate MANAGERs use the estate-scoped route.
   * Both land on the same AdminEstateFormComponent (isEdit = true).
   */
  goToEstateEdit(): void {
    if (this.activeEstateService.isPlatformAdmin()) {
      this.router.navigate(['/admin/estates', this.estateId, 'edit']);
    } else {
      this.router.navigate(['/estates', this.estateId, 'edit']);
    }
  }

  get quickAccessCards(): QuickAccessCard[] {
    const cards: QuickAccessCard[] = [
      {
        icon: '🏢',
        label: 'Buildings',
        route: ['/estates', this.estateId, 'buildings'],
        description: 'Manage buildings and housing units',
      },
      {
        icon: '💶',
        label: 'Transactions',
        route: ['/estates', this.estateId, 'transactions'],
        description: 'Track income and expenses',
      },
      {
        icon: '👤',
        label: 'Persons',
        route: ['/estates', this.estateId, 'persons'],
        description: 'Owners, tenants and contacts',
      },
      {
        icon: '🔔',
        label: 'Alerts',
        route: ['/estates', this.estateId, 'alerts'],
        description: 'Pending actions and deadlines',
      },
    ];

    if (this.canManageMembers) {
      cards.push({
        icon: '👥',
        label: 'Members',
        route: ['/estates', this.estateId, 'members'],
        description: 'Manage estate access',
      });
    }

    return cards;
  }
}
