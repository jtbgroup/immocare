// features/lease/components/lease-details/lease-details.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { LeaseService } from "../../../../core/services/lease.service";
import {
  Lease,
  LEASE_TYPE_LABELS,
  LeaseStatus,
} from "../../../../models/lease.model";
import { RentAdjustmentSectionComponent } from "../rent-adjustment-section/rent-adjustment-section.component";
import { TenantSectionComponent } from "../tenant-section/tenant-section.component";

/** Valid status transitions mirroring backend state machine */
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
  ],
  templateUrl: "./lease-details.component.html",
  styleUrls: ["./lease-details.component.scss"],
})
export class LeaseDetailsComponent implements OnInit {
  lease?: Lease;
  isLoading = false;
  errorMessage = "";
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  // Status change
  selectedStatus: LeaseStatus | "" = "";
  isChangingStatus = false;
  statusError = "";

  constructor(
    private route: ActivatedRoute,
    private leaseService: LeaseService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get("id");
    if (id) this.loadLease(+id);
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: (lease) => {
        this.lease = lease;
        this.selectedStatus = "";
        this.statusError = "";
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Lease not found.";
        this.isLoading = false;
      },
    });
  }

  // ── Status ──────────────────────────────────────────────────────────────────

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

  // ── Display helpers ─────────────────────────────────────────────────────────

  get statusClass(): string {
    const map: Record<string, string> = {
      ACTIVE: "bg-success",
      DRAFT: "bg-secondary",
      FINISHED: "bg-info",
      CANCELLED: "bg-danger",
    };
    return this.lease ? map[this.lease.status] || "bg-secondary" : "";
  }

  statusLabel(status: LeaseStatus): string {
    const labels: Record<LeaseStatus, string> = {
      ACTIVE: "Activate",
      FINISHED: "Mark as Finished",
      CANCELLED: "Cancel",
      DRAFT: "Revert to Draft",
    };
    return labels[status] ?? status;
  }

  canEdit(): boolean {
    return this.lease?.status === "DRAFT" || this.lease?.status === "ACTIVE";
  }
}
