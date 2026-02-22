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

@Component({
  selector: "app-rent-section",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="rent-section">
      <!-- ── Header ── -->
      <div class="section-header">
        <h3>Rent</h3>
        <button
          *ngIf="!showForm && !loading"
          class="btn btn-sm btn-primary"
          (click)="openAddForm()"
        >
          + Set Rent
        </button>
      </div>

      <div *ngIf="loading" class="loading">Loading…</div>

      <!-- ── Empty state ── -->
      <p *ngIf="!loading && !currentRent && !showForm" class="no-data">
        No rent recorded.
      </p>

      <!-- ── Current rent card ── -->
      <div *ngIf="currentRent && !showForm" class="rent-card">
        <div class="rent-amount-badge">
          €{{ currentRent.monthlyRent | number: "1.0-0" }}
        </div>
        <div class="rent-details">
          <div class="rent-date">
            Effective from: <strong>{{ currentRent.effectiveFrom }}</strong>
          </div>
          <div
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
            vs previous
          </div>
        </div>
      </div>

      <!-- ── View History link ── -->
      <div *ngIf="currentRent && !showForm" class="rent-actions">
        <button class="btn-link" (click)="toggleHistory()">
          {{ showHistory ? "Hide History" : "View History" }}
        </button>
      </div>

      <!-- ── Inline form ── -->
      <div *ngIf="showForm" class="rent-form-panel">
        <h4>{{ editingRecord ? "Edit Rent" : "Add Rent" }}</h4>

        <div *ngIf="editingRecord" class="current-read-only">
          <span>Editing record from:</span>
          <strong>{{ editingRecord.effectiveFrom }}</strong>
        </div>

        <form (ngSubmit)="saveForm()">
          <div class="form-field">
            <label>Monthly Rent (€) <span class="required">*</span></label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              [(ngModel)]="formRent"
              name="monthlyRent"
              required
              (input)="onAmountChange()"
              [class.invalid]="submitted && (!formRent || formRent <= 0)"
            />
            <span
              class="field-error"
              *ngIf="submitted && (!formRent || formRent <= 0)"
            >
              Rent must be positive
            </span>
            <span
              *ngIf="editingRecord && formRent && formRent > 0 && previewChange"
              class="change-preview"
              [class.positive]="previewChange.isIncrease"
              [class.negative]="!previewChange.isIncrease"
            >
              {{ previewChange.isIncrease ? "+" : "" }}€{{
                previewChange.amount | number: "1.2-2"
              }}
              ({{ previewChange.isIncrease ? "+" : ""
              }}{{ previewChange.percentage }}%)
            </span>
          </div>

          <div class="form-field">
            <label>Effective From <span class="required">*</span></label>
            <input
              type="date"
              [(ngModel)]="formDate"
              name="effectiveFrom"
              required
              [class.invalid]="submitted && !formDate"
            />
            <span class="field-error" *ngIf="submitted && !formDate"
              >Date is required</span
            >
          </div>

          <div class="form-field">
            <label>Notes (optional)</label>
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
              [(ngModel)]="formNotes"
              name="notes"
              rows="2"
              maxlength="500"
              placeholder="Optional — reason for this change"
            ></textarea>
          </div>

          <div *ngIf="saveError" class="error-banner">{{ saveError }}</div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" [disabled]="saving">
              {{ saving ? "Saving…" : "Save" }}
            </button>
            <button
              type="button"
              class="btn btn-secondary"
              (click)="closeForm()"
              [disabled]="saving"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>

      <!-- ── History panel ── -->
      <div *ngIf="showHistory && !showForm" class="history-panel">
        <div class="history-header">
          <h4>Rent History</h4>
          <button class="btn-close" (click)="toggleHistory()">✕</button>
        </div>

        <div
          *ngIf="totalChange"
          class="improvement-summary"
          [class.improved]="totalChange.isIncrease"
          [class.degraded]="!totalChange.isIncrease"
        >
          Total since first rent:
          {{ totalChange.isIncrease ? "+" : "" }}€{{
            totalChange.amount | number: "1.2-2"
          }}
          ({{ totalChange.isIncrease ? "+" : "" }}{{ totalChange.percentage }}%)
        </div>

        <p *ngIf="history.length === 0" class="no-data">
          No history available.
        </p>

        <table *ngIf="history.length > 0" class="history-table">
          <thead>
            <tr>
              <th>Monthly Rent</th>
              <th>From</th>
              <th>To</th>
              <th>Duration</th>
              <th>Change</th>
              <th>Notes</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let row of history; let i = index">
              <td>
                <strong>€{{ row.monthlyRent | number: "1.2-2" }}</strong>
              </td>
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
                    {{ change.isIncrease ? "↑ +" : "↓ " }}€{{
                      change.amount | number: "1.2-2"
                    }}
                    ({{ change.isIncrease ? "+" : "" }}{{ change.percentage }}%)
                  </span>
                </ng-container>
                <ng-template #noChange>—</ng-template>
              </td>
              <td>{{ row.notes ?? "—" }}</td>
              <td class="row-actions">
                <button
                  class="icon-btn-sm"
                  title="Edit"
                  (click)="openEditForm(row)"
                >
                  ✏️
                </button>
                <button
                  class="icon-btn-sm icon-btn-delete"
                  title="Delete"
                  (click)="askDelete(row)"
                >
                  🗑️
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  `,
  styles: [
    `
      .rent-section {
        margin-top: 0;
      }

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }

      .rent-card {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1rem;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        background: #fafafa;
      }

      .rent-amount-badge {
        display: flex;
        align-items: center;
        justify-content: center;
        min-width: 4.5rem;
        height: 3rem;
        padding: 0 0.75rem;
        border-radius: 6px;
        font-size: 1.05rem;
        font-weight: 700;
        background: #e3f2fd;
        color: #1565c0;
        flex-shrink: 0;
        white-space: nowrap;
      }

      .rent-details {
        flex: 1;
        line-height: 1.8;
      }
      .rent-date {
        font-size: 0.9rem;
        color: #555;
      }
      .rent-change {
        font-size: 0.85rem;
      }

      .icon-btn-sm {
        background: none;
        border: none;
        cursor: pointer;
        font-size: 0.85rem;
        padding: 0.2rem 0.3rem;
        border-radius: 4px;
        opacity: 0.6;
        transition: opacity 0.15s;
      }
      .icon-btn-sm:hover {
        opacity: 1;
        background: #f5f5f5;
      }
      .icon-btn-sm.icon-btn-delete:hover {
        background: #ffebee;
      }

      .row-actions {
        white-space: nowrap;
        text-align: right;
      }

      .rent-actions {
        margin-top: 0.5rem;
      }
      .btn-link {
        background: none;
        border: none;
        color: #007bff;
        cursor: pointer;
        padding: 0;
        font-size: 0.9rem;
      }
      .btn-link:hover {
        text-decoration: underline;
      }

      .warning-banner {
        background: #fff3cd;
        border: 1px solid #ffc107;
        padding: 0.75rem;
        border-radius: 4px;
        font-size: 0.875rem;
        margin-top: 0.75rem;
      }
      .rent-form-panel {
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        padding: 1.25rem;
        margin-top: 1rem;
        background: #fff;
      }
      .rent-form-panel h4 {
        margin: 0 0 1rem;
        font-size: 1rem;
        font-weight: 600;
      }

      .current-read-only {
        display: flex;
        gap: 0.75rem;
        align-items: center;
        padding: 0.5rem 0.75rem;
        background: #f5f5f5;
        border-radius: 4px;
        margin-bottom: 1rem;
        font-size: 0.9rem;
        color: #555;
      }

      .form-field {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        margin-bottom: 1rem;
      }
      .form-field label {
        font-size: 0.875rem;
        font-weight: 500;
      }
      .form-field input,
      .form-field textarea {
        padding: 0.45rem 0.6rem;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-size: 0.9rem;
      }
      .form-field input.invalid {
        border-color: #c62828;
      }
      .required {
        color: #d32f2f;
      }
      .field-error {
        color: #d32f2f;
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

      .error-banner {
        background: #ffebee;
        color: #c62828;
        padding: 0.75rem;
        border-radius: 4px;
        margin-bottom: 0.75rem;
        font-size: 0.875rem;
      }
      .form-actions {
        display: flex;
        gap: 0.5rem;
        margin-top: 1rem;
      }

      .history-panel {
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        padding: 1.25rem;
        margin-top: 1rem;
        background: #fff;
      }
      .history-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }
      .history-header h4 {
        margin: 0;
        font-size: 1rem;
        font-weight: 600;
      }
      .btn-close {
        background: none;
        border: none;
        font-size: 1.1rem;
        cursor: pointer;
        color: #555;
      }
      .btn-close:hover {
        color: #000;
      }

      .improvement-summary {
        padding: 0.75rem 1rem;
        background: #f5f5f5;
        border-radius: 6px;
        margin-bottom: 1rem;
        font-size: 0.9rem;
        font-weight: 500;
      }
      .improvement-summary.improved {
        color: #2e7d32;
      }
      .improvement-summary.degraded {
        color: #c62828;
      }

      .history-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.9rem;
      }
      .history-table th,
      .history-table td {
        padding: 0.5rem 0.6rem;
        border-bottom: 1px solid #e0e0e0;
        text-align: left;
      }
      .history-table th {
        background: #fafafa;
        font-weight: 600;
      }

      .positive {
        color: #2e7d32;
      }
      .negative {
        color: #c62828;
      }
      .no-data {
        color: #888;
        font-style: italic;
      }
      .loading {
        color: #888;
        padding: 0.5rem 0;
      }

      .btn {
        padding: 0.45rem 1rem;
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
      .btn-secondary:hover:not(:disabled) {
        background: #bdbdbd;
      }
      .btn-danger {
        background: #d32f2f;
        color: white;
      }
      .btn-danger:hover:not(:disabled) {
        background: #b71c1c;
      }
      .btn-sm {
        padding: 0.3rem 0.7rem;
        font-size: 0.85rem;
      }
      .btn:disabled {
        opacity: 0.6;
        cursor: default;
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

  editingRecord: RentHistory | null = null; // null = add mode, set = edit mode
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
        },
        error: (err) => {
          if (err.status === 204 || err.status === 404) this.currentRent = null;
          this.loading = false;
        },
      });
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory) this.loadHistory();
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
    if (this.editingRecord && this.formRent && this.formRent > 0) {
      this.previewChange = computeRentChange(
        this.editingRecord.monthlyRent,
        this.formRent,
      );
    } else {
      this.previewChange = null;
    }
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
