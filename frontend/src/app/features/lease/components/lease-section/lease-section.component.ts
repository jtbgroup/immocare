// features/lease/lease-section/lease-section.component.ts
import { CommonModule } from "@angular/common";
import { Component, Input, OnInit } from "@angular/core";
import { RouterModule } from "@angular/router";
import { LeaseService } from "../../../../core/services/lease.service";
import {
  LEASE_TYPE_LABELS,
  LeaseSummary,
} from "../../../../models/lease.model";

@Component({
  selector: "app-lease-section",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./lease-section.component.html",
  styleUrls: ["./lease-section.component.scss"],
})
export class LeaseSectionComponent implements OnInit {
  @Input() unitId!: number;

  leases: LeaseSummary[] = [];
  activeLease?: LeaseSummary;
  isLoading = false;
  showHistory = false;
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  constructor(private leaseService: LeaseService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.leaseService.getByUnit(this.unitId).subscribe({
      next: (leases) => {
        this.leases = leases;
        this.activeLease = leases.find(
          (l) => l.status === "ACTIVE" || l.status === "DRAFT",
        );
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  get pastLeases(): LeaseSummary[] {
    return this.leases.filter(
      (l) => l.status !== "ACTIVE" && l.status !== "DRAFT",
    );
  }

  statusClass(lease: LeaseSummary): string {
    const map: Record<string, string> = {
      ACTIVE: "badge-active",
      DRAFT: "badge-draft",
      FINISHED: "badge-finished",
      CANCELLED: "badge-cancelled",
    };
    return map[lease.status] || "badge-draft";
  }

  get activeStatusClass(): string {
    return this.activeLease ? this.statusClass(this.activeLease) : "";
  }

  get canCreateLease(): boolean {
    return !this.activeLease;
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
  }
}
