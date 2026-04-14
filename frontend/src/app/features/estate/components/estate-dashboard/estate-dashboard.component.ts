// features/estate/components/estate-dashboard/estate-dashboard.component.ts — UC016 US102
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
  route: string[];
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
    private activeEstateService: ActiveEstateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
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
    this.estateService.getDashboard(this.estateId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: data => {
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

  get canManageMembers(): boolean {
    return this.activeEstateService.isManager() || this.activeEstateService.isPlatformAdmin();
  }

  get quickAccessCards(): QuickAccessCard[] {
    const cards: QuickAccessCard[] = [
      {
        icon: '🏢',
        label: 'Buildings',
        route: ['/buildings'],
        description: 'Manage buildings and housing units',
      },
      {
        icon: '💶',
        label: 'Transactions',
        route: ['/transactions'],
        description: 'Track income and expenses',
      },
      {
        icon: '👤',
        label: 'Persons',
        route: ['/persons'],
        description: 'Owners, tenants and contacts',
      },
      {
        icon: '🔔',
        label: 'Alerts',
        route: ['/alerts'],
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
