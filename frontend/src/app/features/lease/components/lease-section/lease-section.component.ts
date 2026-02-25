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

  get statusClass(): string {
    const map: Record<string, string> = {
      ACTIVE: "bg-success",
      DRAFT: "bg-secondary",
      FINISHED: "bg-info",
      CANCELLED: "bg-danger",
    };
    return this.activeLease
      ? map[this.activeLease.status] || "bg-secondary"
      : "";
  }

  get canCreateLease(): boolean {
    return !this.activeLease;
  }
}
