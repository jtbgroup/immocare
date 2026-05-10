// features/alerts/alerts.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { RouterModule } from "@angular/router";
import { AlertService } from "../../../../core/services/alert.service";
import { AlertDTO } from "../../../../models/alert.model";
import { AppDatePipe } from "../../../../shared/pipes/app-date.pipe";

@Component({
  selector: "app-alerts",
  standalone: true,
  imports: [CommonModule, RouterModule, AppDatePipe],
  templateUrl: "./alerts.component.html",
  styleUrls: ["./alerts.component.scss"],
})
export class AlertsComponent implements OnInit {
  alerts: AlertDTO[] = [];
  isLoading = false;
  error: string | null = null;

  get leaseAlerts(): AlertDTO[] {
    return this.alerts.filter((a) => a.category === "LEASE");
  }

  get boilerAlerts(): AlertDTO[] {
    return this.alerts.filter((a) => a.category === "BOILER");
  }

  constructor(private alertService: AlertService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.alertService.getAlerts().subscribe({
      next: (data) => {
        this.alerts = data;
        this.isLoading = false;
      },
      error: () => {
        this.error = "Failed to load alerts.";
        this.isLoading = false;
      },
    });
  }

  categoryIcon(category: string): string {
    return category === "LEASE" ? "📄" : "🔥";
  }

  categoryLabel(category: string): string {
    return category === "LEASE" ? "Lease Alerts" : "Boiler Service Alerts";
  }

  get pebAlerts(): AlertDTO[] {
    return this.alerts.filter((a) => a.category === "PEB");
  }
}
