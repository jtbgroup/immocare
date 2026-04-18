// features/lease/components/lease-global-list/lease-global-list.component.ts — UC016 Phase 3
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { LeaseService } from "../../../../core/services/lease.service";
import {
  LEASE_TYPE_LABELS,
  LeaseGlobalSummary,
  LeaseStatus,
  LeaseType,
} from "../../../../models/lease.model";
import { Page } from "../../../../models/page.model";

export interface LeaseGlobalFilters {
  statuses: LeaseStatus[];
  leaseType: LeaseType | "";
  startDateFrom: string;
  startDateTo: string;
  endDateFrom: string;
  endDateTo: string;
  rentMin: number | null;
  rentMax: number | null;
}

@Component({
  selector: "app-lease-global-list",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./lease-global-list.component.html",
  styleUrls: ["./lease-global-list.component.scss"],
})
export class LeaseGlobalListComponent implements OnInit, OnDestroy {
  leases: LeaseGlobalSummary[] = [];
  loading = false;
  error: string | null = null;

  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  sortField = "startDate";
  sortDirection: "asc" | "desc" = "desc";

  readonly allStatuses: LeaseStatus[] = [
    "ACTIVE",
    "DRAFT",
    "FINISHED",
    "CANCELLED",
  ];
  readonly leaseTypeLabels = LEASE_TYPE_LABELS;
  readonly leaseTypeKeys = Object.keys(LEASE_TYPE_LABELS) as LeaseType[];

  filters: LeaseGlobalFilters = {
    statuses: ["ACTIVE"],
    leaseType: "",
    startDateFrom: "",
    startDateTo: "",
    endDateFrom: "",
    endDateTo: "",
    rentMin: null,
    rentMax: null,
  };

  private destroy$ = new Subject<void>();

  constructor(
    private leaseService: LeaseService,
    private router: Router,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    this.leaseService
      .getAll(
        this.filters,
        this.currentPage,
        this.pageSize,
        `${this.sortField},${this.sortDirection}`,
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: Page<LeaseGlobalSummary>) => {
          this.leases = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.currentPage = page.number;
          this.loading = false;
        },
        error: () => {
          this.error = "Failed to load leases.";
          this.loading = false;
        },
      });
  }

  isStatusSelected(status: LeaseStatus): boolean {
    return this.filters.statuses.includes(status);
  }

  toggleStatus(status: LeaseStatus): void {
    const idx = this.filters.statuses.indexOf(status);
    if (idx >= 0) {
      this.filters.statuses = this.filters.statuses.filter((s) => s !== status);
    } else {
      this.filters.statuses = [...this.filters.statuses, status];
    }
    this.currentPage = 0;
    this.load();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  clearFilters(): void {
    this.filters = {
      statuses: ["ACTIVE"],
      leaseType: "",
      startDateFrom: "",
      startDateTo: "",
      endDateFrom: "",
      endDateTo: "",
      rentMin: null,
      rentMax: null,
    };
    this.currentPage = 0;
    this.load();
  }

  onSortChange(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortField = field;
      this.sortDirection = "asc";
    }
    this.load();
  }

  sortIcon(field: string): string {
    if (this.sortField !== field) return "↕";
    return this.sortDirection === "asc" ? "↑" : "↓";
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.load();
    }
  }

  viewLease(id: number): void {
    this.router.navigate(["/estates", this.estateId, "leases", id]);
  }

  viewUnit(unitId: number): void {
    this.router.navigate(["/estates", this.estateId, "units", unitId]);
  }

  viewBuilding(buildingId: number): void {
    this.router.navigate(["/estates", this.estateId, "buildings", buildingId]);
  }

  statusClass(status: LeaseStatus): string {
    return (
      {
        ACTIVE: "badge badge-active",
        DRAFT: "badge badge-draft",
        FINISHED: "badge badge-finished",
        CANCELLED: "badge badge-cancelled",
      }[status] ?? "badge"
    );
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat("fr-BE", {
      style: "currency",
      currency: "EUR",
      minimumFractionDigits: 0,
    }).format(value);
  }

  hasActiveFilters(): boolean {
    return (
      this.filters.statuses.length !== 1 ||
      !this.filters.statuses.includes("ACTIVE") ||
      !!this.filters.leaseType ||
      !!this.filters.startDateFrom ||
      !!this.filters.startDateTo ||
      !!this.filters.endDateFrom ||
      !!this.filters.endDateTo ||
      this.filters.rentMin !== null ||
      this.filters.rentMax !== null
    );
  }
}
