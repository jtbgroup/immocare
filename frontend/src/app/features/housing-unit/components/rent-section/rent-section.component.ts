import { CommonModule } from "@angular/common";
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

/**
 * Rent section embedded in HousingUnitDetailsComponent.
 * Covers US021, US022, US023, US024, US025.
 */
@Component({
  selector: "app-rent-section",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <!-- ───── Current Rent ───── -->
    <section class="rent-section">
      <div class="section-header">
        <h3>Rent</h3>
        <div class="header-actions" *ngIf="!showForm">
          <button class="btn btn-sm btn-primary" (click)="openForm()">
            {{ currentRent ? "Update Rent" : "Set Rent" }}
          </button>
          <button
            *ngIf="currentRent"
            class="btn btn-sm btn-secondary"
            (click)="toggleHistory()"
          >
            {{ showHistory ? "Hide History" : "View History" }}
          </button>
        </div>
      </div>

      <!-- No rent yet -->
      <p *ngIf="!loading && !currentRent && !showForm" class="empty-state">
        No rent recorded.
      </p>

      <!-- Current rent display -->
      <div *ngIf="currentRent && !showForm" class="current-rent">
        <span class="rent-amount"
          >€{{ currentRent.monthlyRent | number: "1.2-2" }}/month</span
        >
        <span class="rent-date"
          >Effective from: {{ currentRent.effectiveFrom }}</span
        >
        <span
          *ngIf="lastChange"
          class="rent-change"
          [class.positive]="lastChange.isIncrease"
          [class.negative]="!lastChange.isIncrease"
        >
          {{ lastChange.isIncrease ? "↑" : "↓" }}
          {{ lastChange.isIncrease ? "+" : "" }}€{{
            lastChange.amount | number: "1.2-2"
          }}
          ({{ lastChange.isIncrease ? "+" : "" }}{{ lastChange.percentage }}%)
        </span>
      </div>

      <!-- Set / Update form -->
      <form *ngIf="showForm" class="rent-form" (ngSubmit)="saveRent()">
        <div *ngIf="currentRent" class="current-read-only">
          <label>Current Rent</label>
          <span>€{{ currentRent.monthlyRent | number: "1.2-2" }}/month</span>
        </div>

        <div class="form-field">
          <label for="monthlyRent">New Monthly Rent (€) *</label>
          <input
            id="monthlyRent"
            type="number"
            step="0.01"
            min="0.01"
            [(ngModel)]="formRent"
            name="monthlyRent"
            required
            (input)="onAmountChange()"
            [class.invalid]="submitted && (!formRent || formRent <= 0)"
          />
          <span class="error" *ngIf="submitted && (!formRent || formRent <= 0)">
            Rent must be positive
          </span>
          <!-- Live change calculator (US022 AC3) -->
          <span
            *ngIf="currentRent && formRent && formRent > 0"
            class="change-preview"
            [class.positive]="previewChange && previewChange.isIncrease"
            [class.negative]="previewChange && !previewChange.isIncrease"
          >
            {{ previewChange?.isIncrease ? "+" : "" }}€{{
              previewChange?.amount | number: "1.2-2"
            }}
            ({{ previewChange?.isIncrease ? "+" : ""
            }}{{ previewChange?.percentage }}%)
          </span>
        </div>

        <div class="form-field">
          <label for="effectiveFrom">Effective From *</label>
          <input
            id="effectiveFrom"
            type="date"
            [(ngModel)]="formDate"
            name="effectiveFrom"
            required
            [class.invalid]="submitted && !formDate"
          />
          <span class="error" *ngIf="submitted && !formDate">
            Effective from date is required
          </span>
        </div>

        <!-- Notes with templates (US025) -->
        <div class="form-field">
          <label for="notes">Notes (optional)</label>
          <div class="notes-templates">
            <button
              type="button"
              class="tag"
              (click)="applyTemplate('Annual indexation')"
            >
              Annual indexation
            </button>
            <button
              type="button"
              class="tag"
              (click)="applyTemplate('Market adjustment')"
            >
              Market adjustment
            </button>
            <button
              type="button"
              class="tag"
              (click)="applyTemplate('After renovation')"
            >
              After renovation
            </button>
            <button
              type="button"
              class="tag"
              (click)="applyTemplate('Tenant negotiation')"
            >
              Tenant negotiation
            </button>
          </div>
          <textarea
            id="notes"
            [(ngModel)]="formNotes"
            name="notes"
            rows="2"
            maxlength="500"
            placeholder="Optional — reason for this change"
          ></textarea>
        </div>

        <!-- Same amount warning (BR Alternative 3B) -->
        <div *ngIf="sameAmountWarning" class="warning-banner">
          ⚠️ New rent is the same as the current rent. Continue anyway?
        </div>

        <div class="form-error" *ngIf="saveError">{{ saveError }}</div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" [disabled]="saving">
            {{ saving ? "Saving…" : "Save" }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="closeForm()">
            Cancel
          </button>
        </div>
      </form>

      <!-- History table (US023) -->
      <div
        *ngIf="showHistory && history.length > 0"
        class="history-table-wrapper"
      >
        <h4>Rent History</h4>

        <!-- Total increase (US024 AC2) -->
        <div
          *ngIf="totalChange"
          class="total-change"
          [class.positive]="totalChange.isIncrease"
          [class.negative]="!totalChange.isIncrease"
        >
          Total since first rent:
          {{ totalChange.isIncrease ? "+" : "" }}€{{
            totalChange.amount | number: "1.2-2"
          }}
          ({{ totalChange.isIncrease ? "+" : "" }}{{ totalChange.percentage }}%)
        </div>

        <table class="history-table">
          <thead>
            <tr>
              <th>Monthly Rent</th>
              <th>From</th>
              <th>To</th>
              <th>Duration</th>
              <th>Change</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let row of history; let i = index">
              <td>€{{ row.monthlyRent | number: "1.2-2" }}</td>
              <td>{{ row.effectiveFrom }}</td>
              <td>{{ row.effectiveTo ?? "Current" }}</td>
              <td>
                {{ row.durationMonths }} month{{
                  row.durationMonths !== 1 ? "s" : ""
                }}
              </td>
              <td>
                <ng-container *ngIf="rowChanges[i] as change; else noChange">
                  <span
                    [class.positive]="change.isIncrease"
                    [class.negative]="!change.isIncrease"
                  >
                    {{ change.isIncrease ? "↑ +" : "↓ " }}
                    €{{ change.amount | number: "1.2-2" }} ({{
                      change.isIncrease ? "+" : ""
                    }}{{ change.percentage }}%)
                  </span>
                </ng-container>
                <ng-template #noChange>—</ng-template>
              </td>
              <td>{{ row.notes ?? "—" }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="loading" class="loading">Loading…</div>
    </section>
  `,
  styles: [
    `
      .rent-section {
        margin-top: 1.5rem;
      }
      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;
      }
      .header-actions {
        display: flex;
        gap: 0.5rem;
      }
      .current-rent {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .rent-amount {
        font-size: 1.4rem;
        font-weight: 600;
      }
      .rent-date {
        color: #666;
        font-size: 0.9rem;
      }
      .rent-change {
        font-size: 0.9rem;
      }
      .positive {
        color: #2e7d32;
      }
      .negative {
        color: #c62828;
      }

      .rent-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        background: #f9f9f9;
        padding: 1rem;
        border-radius: 6px;
      }
      .current-read-only {
        display: flex;
        gap: 1rem;
        align-items: center;
        padding: 0.5rem 0;
        border-bottom: 1px solid #e0e0e0;
        color: #555;
      }
      .form-field {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .form-field label {
        font-weight: 500;
        font-size: 0.9rem;
      }
      .form-field input,
      .form-field textarea {
        padding: 0.4rem 0.6rem;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-size: 0.95rem;
      }
      .form-field input.invalid {
        border-color: #c62828;
      }
      .error {
        color: #c62828;
        font-size: 0.8rem;
      }
      .change-preview {
        font-size: 0.85rem;
        font-weight: 500;
      }

      .notes-templates {
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
        margin-bottom: 0.4rem;
      }
      .tag {
        background: #e8f0fe;
        border: none;
        padding: 0.2rem 0.6rem;
        border-radius: 12px;
        cursor: pointer;
        font-size: 0.8rem;
      }
      .tag:hover {
        background: #c5cae9;
      }

      .warning-banner {
        background: #fff3cd;
        border: 1px solid #ffc107;
        padding: 0.5rem 0.75rem;
        border-radius: 4px;
        font-size: 0.9rem;
      }
      .form-error {
        color: #c62828;
        font-size: 0.9rem;
      }
      .form-actions {
        display: flex;
        gap: 0.5rem;
      }

      .history-table-wrapper {
        margin-top: 1rem;
      }
      .total-change {
        font-size: 0.9rem;
        font-weight: 500;
        margin-bottom: 0.5rem;
      }
      .history-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.9rem;
      }
      .history-table th,
      .history-table td {
        padding: 0.4rem 0.6rem;
        border-bottom: 1px solid #e0e0e0;
        text-align: left;
      }
      .history-table th {
        background: #f5f5f5;
        font-weight: 600;
      }

      .btn {
        padding: 0.4rem 0.9rem;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.9rem;
      }
      .btn-primary {
        background: #1976d2;
        color: white;
      }
      .btn-primary:hover:not(:disabled) {
        background: #1565c0;
      }
      .btn-secondary {
        background: #e0e0e0;
        color: #333;
      }
      .btn-secondary:hover {
        background: #bdbdbd;
      }
      .btn-sm {
        padding: 0.25rem 0.6rem;
        font-size: 0.85rem;
      }
      .btn:disabled {
        opacity: 0.6;
        cursor: default;
      }
      .empty-state {
        color: #888;
      }
      .loading {
        color: #888;
      }
    `,
  ],
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

  // Form fields
  formRent: number | null = null;
  formDate: string = "";
  formNotes: string = "";
  previewChange: RentChange | null = null;
  get sameAmountWarning(): boolean {
    return !!(
      this.currentRent && this.formRent === this.currentRent.monthlyRent
    );
  }

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

  loadCurrentRent(): void {
    this.loading = true;
    this.rentService
      .getCurrentRent(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rent) => {
          this.currentRent = rent;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
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
          this.computeHistoryChanges();
        },
      });
  }

  computeHistoryChanges(): void {
    // rowChanges[i] = change vs row[i+1] (the previous, older record)
    this.rowChanges = this.history.map((row, i) => {
      const older = this.history[i + 1];
      return older
        ? computeRentChange(older.monthlyRent, row.monthlyRent)
        : null;
    });

    // lastChange: most recent transition
    this.lastChange = this.rowChanges[0] ?? null;

    // totalChange: first rent → current
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

  openForm(): void {
    this.showForm = true;
    this.submitted = false;
    this.saveError = "";
    this.formRent = null;
    this.formDate = "";
    this.formNotes = "";
    this.previewChange = null;
  }

  closeForm(): void {
    this.showForm = false;
    this.submitted = false;
  }

  onAmountChange(): void {
    if (this.currentRent && this.formRent && this.formRent > 0) {
      this.previewChange = computeRentChange(
        this.currentRent.monthlyRent,
        this.formRent,
      );
    } else {
      this.previewChange = null;
    }
  }

  applyTemplate(template: string): void {
    this.formNotes = template;
  }

  saveRent(): void {
    this.submitted = true;
    if (!this.formRent || this.formRent <= 0 || !this.formDate) return;

    const request: SetRentRequest = {
      monthlyRent: this.formRent,
      effectiveFrom: this.formDate,
      notes: this.formNotes || null,
    };

    this.saving = true;
    this.saveError = "";

    this.rentService
      .setOrUpdateRent(this.unitId, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (saved) => {
          this.currentRent = saved;
          this.showForm = false;
          this.saving = false;
          // Refresh history if it was open
          if (this.showHistory) this.loadHistory();
        },
        error: (err) => {
          this.saving = false;
          this.saveError =
            err.error?.message ?? "Failed to save rent. Please try again.";
        },
      });
  }
}
