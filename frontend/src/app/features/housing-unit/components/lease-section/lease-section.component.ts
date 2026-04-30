import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
} from "@angular/core";
import { DecimalPipe, DatePipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { environment } from "../../../../../environments/environment";

export interface LeaseSummary {
  id: number;
  status: "DRAFT" | "ACTIVE" | "FINISHED";
  leaseType: string;
  startDate: string;
  endDate: string | null;
  monthlyRent: number;
  monthlyCharges: number;
  tenants: { personId: number; fullName: string; role: string }[];
}

@Component({
  selector: "app-lease-section",
  standalone: true,
  imports: [DecimalPipe, DatePipe, RouterLink],
  templateUrl: "./lease-section.component.html",
  styleUrls: ["./lease-section.component.scss"],
})
export class LeaseSectionComponent implements OnChanges, OnDestroy {
  @Input() unitId!: number;

  leases: LeaseSummary[] = [];
  loading = false;
  showFinished = false;

  private destroy$ = new Subject<void>();

  constructor(
    private http: HttpClient,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["unitId"] && this.unitId) {
      this.load();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    const estateId = this.activeEstateService.activeEstateId();
    this.http
      .get<LeaseSummary[]>(
        `${environment.apiUrl}/api/v1/estates/${estateId}/housing-units/${this.unitId}/leases`,
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (leases) => {
          // Sort: ACTIVE first, DRAFT second, FINISHED last; within group by startDate desc
          this.leases = leases.sort((a, b) => {
            const order = { ACTIVE: 0, DRAFT: 1, FINISHED: 2 };
            const diff = order[a.status] - order[b.status];
            if (diff !== 0) return diff;
            return b.startDate.localeCompare(a.startDate);
          });
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  get activeOrDraftLeases(): LeaseSummary[] {
    return this.leases.filter((l) => l.status !== "FINISHED");
  }

  get finishedLeases(): LeaseSummary[] {
    return this.leases.filter((l) => l.status === "FINISHED");
  }

  statusLabel(status: string): string {
    return { ACTIVE: "Active", DRAFT: "Draft", FINISHED: "Finished" }[status] ?? status;
  }

  leaseTypeLabel(type: string): string {
    return type
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  tenantNames(lease: LeaseSummary): string {
    return lease.tenants.map((t) => t.fullName).join(", ") || "—";
  }
}