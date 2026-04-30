import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
} from "@angular/core";
import { DecimalPipe, LowerCasePipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { LeaseService } from "../../../../core/services/lease.service";
import { LeaseSummary } from "../../../../models/lease.model";


@Component({
  selector: "app-lease-section",
  standalone: true,
  imports: [DecimalPipe, LowerCasePipe, RouterLink],
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
    private leaseService: LeaseService,
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
    this.leaseService
      .getByUnit(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (leases) => {
          const order: Record<string, number> = { ACTIVE: 0, DRAFT: 1, FINISHED: 2 };
          this.leases = leases.sort((a, b) => {
            const diff = (order[a.status] ?? 3) - (order[b.status] ?? 3);
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