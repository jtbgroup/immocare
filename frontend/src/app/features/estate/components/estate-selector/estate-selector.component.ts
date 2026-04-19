// features/estate/components/estate-selector/estate-selector.component.ts — UC016 US101
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { AuthService } from "../../../../core/auth/auth.service";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { EstateService } from "../../../../core/services/estate.service";
import {
  ESTATE_ROLE_COLORS,
  ESTATE_ROLE_LABELS,
  EstateSummary,
} from "../../../../models/estate.model";

@Component({
  selector: "app-estate-selector",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./estate-selector.component.html",
  styleUrls: ["./estate-selector.component.scss"],
})
export class EstateSelectorComponent implements OnInit, OnDestroy {
  estates: EstateSummary[] = [];
  loading = true;
  error: string | null = null;
  isPlatformAdmin = false;

  readonly ESTATE_ROLE_LABELS = ESTATE_ROLE_LABELS;
  readonly ESTATE_ROLE_COLORS = ESTATE_ROLE_COLORS;

  private destroy$ = new Subject<void>();

  constructor(
    private estateService: EstateService,
    private activeEstateService: ActiveEstateService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.authService
      .getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe((user) => {
        this.isPlatformAdmin = user?.isPlatformAdmin ?? false;
      });

    this.estateService
      .getMyEstates()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (estates) => {
          this.loading = false;

          // Auto-select if exactly one estate (US101 AC1)
          if (estates.length === 1) {
            this.selectEstate(estates[0]);
            return;
          }

          this.estates = estates;
        },
        error: () => {
          this.loading = false;
          this.error = "Failed to load estates. Please try again.";
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  selectEstate(estate: EstateSummary): void {
    this.activeEstateService.setActiveEstate(estate);
    this.router.navigate(["/estates", estate.id, "dashboard"]);
  }

  goToAdmin(): void {
    this.router.navigate(["/admin/estates"]);
  }

  totalAlerts(estate: EstateSummary): number {
    // Placeholder — Phase 6 will populate real counts
    return 0;
  }

  roleLabel(estate: EstateSummary): string {
    if (estate.myRole === null) return "Platform Admin";
    return ESTATE_ROLE_LABELS[estate.myRole];
  }

  roleStyle(estate: EstateSummary): { background: string; color: string } {
    if (estate.myRole === null)
      return { background: "#e3f2fd", color: "#1565c0" };
    const c = ESTATE_ROLE_COLORS[estate.myRole];
    return { background: c.bg, color: c.text };
  }
}
