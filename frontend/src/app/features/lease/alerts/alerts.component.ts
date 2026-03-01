// features/lease/alerts/alerts.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { RouterModule } from "@angular/router";
import { forkJoin } from "rxjs";
import { BoilerService } from "../../../core/services/boiler.service";
import { LeaseService } from "../../../core/services/lease.service";
import { BoilerDTO } from "../../../models/boiler.model";
import { LeaseAlert } from "../../../models/lease.model";

@Component({
  selector: "app-lease-alerts",
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="container py-4">
      <h1 class="h3 mb-4">
        Alerts
        <span *ngIf="totalCount > 0" class="badge bg-warning text-dark ms-2">{{
          totalCount
        }}</span>
      </h1>

      <div *ngIf="isLoading" class="text-center py-5">
        <div class="spinner-border"></div>
      </div>

      <div *ngIf="!isLoading && totalCount === 0" class="alert alert-success">
        ✅ No pending alerts.
      </div>

      <!-- Lease alerts -->
      <div *ngIf="!isLoading && alerts.length > 0" class="card shadow-sm mb-4">
        <div class="card-header fw-semibold">📄 Lease Alerts</div>
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

      <!-- Boiler service alerts -->
      <div *ngIf="!isLoading && boilerAlerts.length > 0" class="card shadow-sm">
        <div class="card-header fw-semibold">🔥 Boiler Service Alerts</div>
        <div class="table-responsive">
          <table class="table table-hover mb-0">
            <thead class="table-light">
              <tr>
                <th>Owner</th>
                <th>Brand / Model</th>
                <th>Fuel</th>
                <th>Next Service</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let b of boilerAlerts">
                <td>
                  {{ b.ownerType === "HOUSING_UNIT" ? "Unit" : "Building" }}
                  #{{ b.ownerId }}
                </td>
                <td>{{ b.brand || "—" }} {{ b.model || "" }}</td>
                <td>{{ b.fuelType }}</td>
                <td class="fw-semibold">
                  {{ b.nextServiceDate | date: "dd/MM/yyyy" }}
                </td>
                <td>
                  <span
                    *ngIf="
                      b.daysUntilNextService !== null &&
                      b.daysUntilNextService < 0
                    "
                    class="badge bg-danger"
                    >Overdue by {{ -b.daysUntilNextService }} day(s)</span
                  >
                  <span
                    *ngIf="
                      b.daysUntilNextService !== null &&
                      b.daysUntilNextService >= 0
                    "
                    class="badge bg-warning text-dark"
                    >Due in {{ b.daysUntilNextService }} day(s)</span
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
  boilerAlerts: BoilerDTO[] = [];
  isLoading = false;

  get totalCount(): number {
    return this.alerts.length + this.boilerAlerts.length;
  }

  constructor(
    private leaseService: LeaseService,
    private boilerService: BoilerService,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    forkJoin({
      leaseAlerts: this.leaseService.getAlerts(),
      boilerAlerts: this.boilerService.getServiceAlerts(),
    }).subscribe({
      next: ({ leaseAlerts, boilerAlerts }) => {
        this.alerts = leaseAlerts;
        this.boilerAlerts = boilerAlerts;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }
}
