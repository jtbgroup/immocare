// features/lease/components/lease-details/lease-details.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { TranslocoModule } from "@ngneat/transloco";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { LeaseService } from "../../../../core/services/lease.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import { Lease, LeaseStatus } from "../../../../models/lease.model";
import { TransactionStatistics } from "../../../../models/transaction.model";
import { AppDatePipe } from "../../../../shared/pipes/app-date.pipe";
import { RentAdjustmentSectionComponent } from "../_partials/rent-adjustment-section/rent-adjustment-section.component";
import { TenantSectionComponent } from "../_partials/tenant-section/tenant-section.component";

const TRANSITIONS: Record<LeaseStatus, LeaseStatus[]> = {
  DRAFT: ["ACTIVE", "CANCELLED"],
  ACTIVE: ["FINISHED", "CANCELLED"],
  FINISHED: [],
  CANCELLED: [],
};

@Component({
  selector: "app-lease-details",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    TenantSectionComponent,
    RentAdjustmentSectionComponent,
    AppDatePipe,
    TranslocoModule,
  ],
  templateUrl: "./lease-details.component.html",
  styleUrls: ["./lease-details.component.scss"],
})
export class LeaseDetailsComponent implements OnInit, OnDestroy {
  lease?: Lease;
  isLoading = false;
  errorMessage = "";

  // Status change
  selectedStatus: LeaseStatus | "" = "";
  isChangingStatus = false;
  statusError = "";

  // Delete
  deleting = false;
  deleteError = "";

  leaseStats: TransactionStatistics | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private leaseService: LeaseService,
    private transactionService: TransactionService,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get("id");
    if (id) this.loadLease(+id);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: (lease) => {
        this.lease = lease;
        this.selectedStatus = "";
        this.statusError = "";
        this.isLoading = false;
        this.loadLeaseStats(lease);
      },
      error: () => {
        this.errorMessage = "Lease not found.";
        this.isLoading = false;
      },
    });
  }

  loadLeaseStats(lease: Lease): void {
    this.transactionService
      .getStatistics({ unitId: lease.housingUnitId })
      .pipe(takeUntil(this.destroy$))
      .subscribe((s) => (this.leaseStats = s));
  }

  viewLeaseTransactions(): void {
    if (!this.lease) return;
    this.router.navigate(["/transactions"], {
      queryParams: { tab: "list", unitId: this.lease.housingUnitId },
    });
  }

  back(): void {
    this.router.navigate(["/estates", this.estateId, "leases"]);
  }

  // ── Status change ─────────────────────────────────────────────────────────

  get eligibleStatuses(): LeaseStatus[] {
    if (!this.lease) return [];
    return TRANSITIONS[this.lease.status] ?? [];
  }

  get canChangeStatus(): boolean {
    return (
      this.eligibleStatuses.length > 0 && this.activeEstateService.canEdit()
    );
  }

  applyStatusChange(): void {
    if (!this.lease || !this.selectedStatus) return;
    this.isChangingStatus = true;
    this.statusError = "";
    this.leaseService
      .changeStatus(this.lease.id, { targetStatus: this.selectedStatus })
      .subscribe({
        next: () => {
          this.isChangingStatus = false;
          this.loadLease(this.lease!.id);
        },
        error: (err) => {
          this.statusError = err.error?.message || "Status change failed.";
          this.isChangingStatus = false;
        },
      });
  }

  // ── Edit / Delete ─────────────────────────────────────────────────────────

  canEdit(): boolean {
    return this.lease?.status === "DRAFT" || this.lease?.status === "ACTIVE";
  }

  confirmDelete(): void {
    if (!this.lease) return;
    if (!confirm(`Delete lease #${this.lease.id}? This cannot be undone.`))
      return;
    this.deleting = true;
    this.leaseService.deleteLease(this.lease.id).subscribe({
      next: () =>
        this.router.navigate([
          "/estates",
          this.estateId,
          "units",
          this.lease!.housingUnitId,
        ]),
      error: (err) => {
        this.deleting = false;
        this.deleteError = err.error?.message ?? "Failed to delete lease.";
      },
    });
  }

  edit(): void {
    this.router.navigate([
      "/estates",
      this.estateId,
      "leases",
      this.lease!.id,
      "edit",
    ]);
  }
}
