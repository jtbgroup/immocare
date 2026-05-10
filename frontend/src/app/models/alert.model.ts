// models/alert.model.ts

export type AlertCategory = "LEASE" | "BOILER" | "PEB";
export type AlertType =
  | "INDEXATION"
  | "END_NOTICE"
  | "SERVICE_DUE"
  | "SERVICE_OVERDUE"
  | "CERTIFICATE_EXPIRED"
  | "CERTIFICATE_EXPIRING";
export type AlertSeverity = "WARNING" | "DANGER";

export interface AlertDTO {
  category: AlertCategory;
  type: AlertType;
  severity: AlertSeverity;
  label: string;
  deadline: string | null; // ISO date
  actionUrl: string;
  detail: string | null;
}
