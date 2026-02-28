// features/lease/components/lease-global-list/lease-global-list.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

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
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: "./lease-global-list.component.html",
  styleUrls: ["./lease-global-list.component.scss"],
})
export class LeaseGlobalListComponent implements OnInit, OnDestroy {
  leases: LeaseGlobalSummary[] = [];
  loading = false;
  error: string | null = null;

  // ── Pagination ──────────────────────────────────────────────────────────────
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // ── Sort ────────────────────────────────────────────────────────────────────
  sortField = "startDate";
  sortDirection: "asc" | "desc" = "desc";

  // ── Filters ─────────────────────────────────────────────────────────────────
  readonly allStatuses: LeaseStatus[] = [
    "ACTIVE",
    "DRAFT",
    "FINISHED",
    "CANCELLED",
  ];
  readonly leaseTypeLabels = LEASE_TYPE_LABELS;
  readonly leaseTypeKeys = Object.keys(LEASE_TYPE_LABELS) as LeaseType[];

  filters: LeaseGlobalFilters = {
    statuses: ["ACTIVE"], // default: active only
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
  ) {}

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

  // ── Filter helpers ──────────────────────────────────────────────────────────

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

  // ── Sort ────────────────────────────────────────────────────────────────────

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

  // ── Pagination ──────────────────────────────────────────────────────────────

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

  // ── Navigation ──────────────────────────────────────────────────────────────

  viewLease(id: number): void {
    this.router.navigate(["/leases", id]);
  }

  viewUnit(unitId: number): void {
    this.router.navigate(["/units", unitId]);
  }

  // ── Display helpers ─────────────────────────────────────────────────────────

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
