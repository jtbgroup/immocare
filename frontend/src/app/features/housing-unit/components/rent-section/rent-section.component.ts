import { DecimalPipe } from "@angular/common";
import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { RentService } from "../../../../core/services/rent.service";
import {
  RentChange,
  RentHistory,
  SetRentRequest,
  computeRentChange,
} from "../../../../models/rent.model";

@Component({
  selector: "app-rent-section",
  standalone: true,
  imports: [DecimalPipe, FormsModule],
  templateUrl: "./rent-section.component.html",
  styleUrls: ["./rent-section.component.scss"],
})
export class RentSectionComponent implements OnChanges, OnDestroy {
  @Input() unitId!: number;

  currentRent: RentHistory | null = null;
  history: RentHistory[] = [];
  rowChanges: (RentChange | null)[] = [];
  lastChange: RentChange | null = null;
  totalChange: RentChange | null = null;

  showForm = false;
  showHistory = false;
  loading = false;
  saving = false;
  submitted = false;
  saveError = "";

  editingRecord: RentHistory | null = null;
  deleteTarget: RentHistory | null = null;

  formRent: number | null = null;
  formDate: string = "";
  formNotes: string = "";
  previewChange: RentChange | null = null;

  private destroy$ = new Subject<void>();

  constructor(private rentService: RentService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["unitId"] && this.unitId) {
      this.loadCurrentRent();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Load ──────────────────────────────────────────────────────────────────

  loadCurrentRent(): void {
    this.loading = true;
    this.rentService
      .getCurrentRent(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rent) => {
          this.currentRent = rent ?? null;
          this.loading = false;
          if (this.currentRent) {
            this.loadHistoryForChange();
          }
        },
        error: (err) => {
          if (err.status === 204 || err.status === 404) this.currentRent = null;
          this.loading = false;
        },
      });
  }

  loadHistoryForChange(): void {
    this.rentService
      .getRentHistory(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (history) => {
          this.history = history;
          this.computeChanges();
        },
      });
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory && this.history.length === 0) {
      this.loadHistory();
    }
  }

  loadHistory(): void {
    this.rentService
      .getRentHistory(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (history) => {
          this.history = history;
          this.computeChanges();
        },
      });
  }

  computeChanges(): void {
    this.rowChanges = this.history.map((row, i) => {
      const older = this.history[i + 1];
      return older
        ? computeRentChange(older.monthlyRent, row.monthlyRent)
        : null;
    });
    this.lastChange = this.rowChanges[0] ?? null;
    if (this.history.length >= 2) {
      const oldest = this.history[this.history.length - 1];
      const newest = this.history[0];
      this.totalChange = computeRentChange(
        oldest.monthlyRent,
        newest.monthlyRent,
      );
    } else {
      this.totalChange = null;
    }
  }

  // ── Form ──────────────────────────────────────────────────────────────────

  openAddForm(): void {
    this.editingRecord = null;
    this.showForm = true;
    this.deleteTarget = null;
    this.submitted = false;
    this.saveError = "";
    this.formRent = null;
    this.formDate = "";
    this.formNotes = "";
    this.previewChange = null;
  }

  openEditForm(record: RentHistory): void {
    this.editingRecord = record;
    this.showForm = true;
    this.showHistory = false;
    this.deleteTarget = null;
    this.submitted = false;
    this.saveError = "";
    this.formRent = record.monthlyRent;
    this.formDate = record.effectiveFrom;
    this.formNotes = record.notes ?? "";
    this.previewChange = null;
  }

  closeForm(): void {
    this.showForm = false;
    this.editingRecord = null;
    this.submitted = false;
    this.saveError = "";
  }

  onAmountChange(): void {
    this.previewChange =
      this.editingRecord && this.formRent && this.formRent > 0
        ? computeRentChange(this.editingRecord.monthlyRent, this.formRent)
        : null;
  }

  applyTemplate(template: string): void {
    this.formNotes = template;
  }

  saveForm(): void {
    this.submitted = true;
    if (!this.formRent || this.formRent <= 0 || !this.formDate) return;

    const request: SetRentRequest = {
      monthlyRent: this.formRent,
      effectiveFrom: this.formDate,
      notes: this.formNotes || null,
    };

    this.saving = true;
    this.saveError = "";

    const op$ = this.editingRecord
      ? this.rentService.updateRent(this.unitId, this.editingRecord.id, request)
      : this.rentService.addRent(this.unitId, request);

    op$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.showForm = false;
        this.saving = false;
        this.editingRecord = null;
        this.loadCurrentRent();
        if (this.showHistory) this.loadHistory();
      },
      error: (err) => {
        this.saving = false;
        this.saveError = err.error?.message ?? "Failed to save rent.";
      },
    });
  }

  // ── Delete ────────────────────────────────────────────────────────────────

  askDelete(record: RentHistory): void {
    this.deleteTarget = record;
    this.showForm = false;
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.saving = true;
    this.rentService
      .deleteRent(this.unitId, this.deleteTarget.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.deleteTarget = null;
          this.loadCurrentRent();
          if (this.showHistory) this.loadHistory();
        },
        error: (err) => {
          this.saving = false;
          this.saveError = err.error?.message ?? "Failed to delete rent.";
          this.deleteTarget = null;
        },
      });
  }
}
