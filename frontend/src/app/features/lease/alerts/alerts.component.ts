// features/lease/alerts/alerts.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { RouterModule } from "@angular/router";
import { LeaseService } from "../../../core/services/lease.service";
import { LeaseAlert } from "../../../models/lease.model";

@Component({
  selector: "app-lease-alerts",
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="container py-4">
      <h1 class="h3 mb-4">
        Lease Alerts
        <span
          *ngIf="alerts.length > 0"
          class="badge bg-warning text-dark ms-2"
          >{{ alerts.length }}</span
        >
      </h1>

      <div *ngIf="isLoading" class="text-center py-5">
        <div class="spinner-border"></div>
      </div>

      <div
        *ngIf="!isLoading && alerts.length === 0"
        class="alert alert-success"
      >
        ✅ No pending alerts.
      </div>

      <div *ngIf="!isLoading && alerts.length > 0" class="card shadow-sm">
        <div class="table-responsive">
          <table class="table table-hover mb-0">
            <thead class="table-light">
              <tr>
                <th>Unit</th>
                <th>Building</th>
                <th>Alert Type</th>
                <th>Deadline</th>
                <th>Tenant(s)</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let alert of alerts">
                <td>{{ alert.housingUnitNumber }}</td>
                <td>{{ alert.buildingName }}</td>
                <td>
                  <span
                    *ngIf="alert.alertType === 'INDEXATION'"
                    class="badge bg-warning text-dark"
                    >🔔 Indexation</span
                  >
                  <span
                    *ngIf="alert.alertType === 'END_NOTICE'"
                    class="badge bg-danger"
                    >⚠ End Notice</span
                  >
                </td>
                <td class="fw-semibold">
                  {{ alert.deadline | date: "dd/MM/yyyy" }}
                </td>
                <td>{{ alert.tenantNames.join(", ") || "—" }}</td>
                <td>
                  <a
                    [routerLink]="['/leases', alert.leaseId]"
                    class="btn btn-sm btn-outline-primary"
                    >View Lease</a
                  >
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class AlertsComponent implements OnInit {
  alerts: LeaseAlert[] = [];
  isLoading = false;

  constructor(private leaseService: LeaseService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.leaseService.getAlerts().subscribe({
      next: (alerts) => {
        this.alerts = alerts;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }
}
