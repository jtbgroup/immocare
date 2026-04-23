// features/lease/components/lease-details/lease-details.component.ts — UC004_ESTATE_PLACEHOLDER Phase 3
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { LeaseService } from "../../../../core/services/lease.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import {
  Lease,
  LEASE_TYPE_LABELS,
  LeaseStatus,
} from "../../../../models/lease.model";
import { TransactionStatistics } from "../../../../models/transaction.model";
import { AppDatePipe } from "../../../../shared/pipes/app-date.pipe";
import { RentAdjustmentSectionComponent } from "../rent-adjustment-section/rent-adjustment-section.component";
import { TenantSectionComponent } from "../tenant-section/tenant-section.component";

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
  ],
  templateUrl: "./lease-details.component.html",
  styleUrls: ["./lease-details.component.scss"],
})
export class LeaseDetailsComponent implements OnInit, OnDestroy {
  lease?: Lease;
  isLoading = false;
  errorMessage = "";
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  selectedStatus: LeaseStatus | "" = "";
  isChangingStatus = false;
  statusError = "";

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

  get eligibleStatuses(): LeaseStatus[] {
    if (!this.lease) return [];
    return TRANSITIONS[this.lease.status] ?? [];
  }

  get canChangeStatus(): boolean {
    return this.eligibleStatuses.length > 0;
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

  canEdit(): boolean {
    return this.lease?.status === "DRAFT" || this.lease?.status === "ACTIVE";
  }
}
