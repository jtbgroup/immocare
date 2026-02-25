// features/lease/lease-list/lease-list.component.ts
import { CommonModule } from "@angular/common";
import { Component, Input, OnInit } from "@angular/core";
import { RouterModule } from "@angular/router";
import { LeaseService } from "../../../../core/services/lease.service";
import {
  LEASE_TYPE_LABELS,
  LeaseSummary,
} from "../../../../models/lease.model";

@Component({
  selector: "app-lease-list",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./lease-list.component.html",
  styleUrls: ["./lease-list.component.scss"],
})
export class LeaseListComponent implements OnInit {
  @Input() unitId!: number;
  leases: LeaseSummary[] = [];
  isLoading = false;
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  constructor(private leaseService: LeaseService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.leaseService.getByUnit(this.unitId).subscribe({
      next: (leases) => {
        this.leases = leases;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: "bg-success",
      DRAFT: "bg-secondary",
      FINISHED: "bg-info",
      CANCELLED: "bg-danger",
    };
    return map[status] || "bg-secondary";
  }
}
