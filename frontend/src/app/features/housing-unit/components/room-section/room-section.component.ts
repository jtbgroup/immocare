import { CommonModule } from "@angular/common";
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { RoomService } from "../../../../core/services/room.service";
import {
  ALL_ROOM_TYPES,
  BatchRoomEntry,
  Room,
  ROOM_TYPE_LABELS,
  RoomListResponse,
  RoomType,
} from "../../../../models/room.model";

/**
 * Room section embedded inside HousingUnitDetailsComponent.
 * UC003 - Manage Rooms (US012–US016).
 */
@Component({
  selector: "app-room-section",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="room-section">
      <!-- Section header -->
      <div class="section-header">
        <h3>Rooms</h3>
        <div class="header-actions">
          <button class="btn btn-sm btn-primary" (click)="openAddForm()">
            Add Room
          </button>
          <button class="btn btn-sm btn-secondary" (click)="openQuickAdd()">
            Quick Add
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="loading">Loading rooms…</div>

      <!-- Error -->
      <div *ngIf="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <!-- Success banner -->
      <div *ngIf="successMessage" class="success-banner">
        {{ successMessage }}
      </div>

      <!-- Empty state -->
      <p
        *ngIf="!loading && rooms.length === 0 && !showAddForm && !showQuickAdd"
        class="empty-state"
      >
        No rooms defined yet.
      </p>

      <!-- Room table -->
      <div
        class="table-wrapper"
        *ngIf="rooms.length > 0 || editingRoomId !== null"
      >
        <table class="rooms-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Surface (m²)</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let room of rooms">
              <!-- View mode -->
              <ng-container *ngIf="editingRoomId !== room.id">
                <td>{{ roomLabel(room.roomType) }}</td>
                <td>{{ room.approximateSurface | number: "1.2-2" }}</td>
                <td class="actions">
                  <button
                    class="btn-icon"
                    title="Edit"
                    (click)="startEdit(room)"
                    [disabled]="saving"
                  >
                    ✏️
                  </button>
                  <button
                    class="btn-icon"
                    title="Delete"
                    (click)="requestDelete(room)"
                    [disabled]="saving"
                  >
                    🗑️
                  </button>
                </td>
              </ng-container>

              <!-- Edit mode -->
              <ng-container *ngIf="editingRoomId === room.id">
                <td>
                  <select [formControl]="editRoomType" class="select-inline">
                    <option *ngFor="let t of allTypes" [value]="t">
                      {{ roomLabel(t) }}
                    </option>
                  </select>
                  <span
                    class="field-error"
                    *ngIf="editRoomType.invalid && editRoomType.touched"
                  >
                    Type is required
                  </span>
                </td>
                <td>
                  <input
                    type="number"
                    [formControl]="editApproximateSurface"
                    class="input-inline"
                    min="0.01"
                    max="999.99"
                    step="0.01"
                  />
                  <span
                    class="field-error"
                    *ngIf="
                      editApproximateSurface.invalid &&
                      editApproximateSurface.touched
                    "
                  >
                    Surface must be between 0.01 and 999.99
                  </span>
                </td>
                <td class="actions">
                  <button
                    class="btn btn-sm btn-primary"
                    (click)="saveEdit(room)"
                    [disabled]="saving"
                  >
                    {{ saving ? "…" : "Save" }}
                  </button>
                  <button
                    class="btn btn-sm btn-secondary"
                    (click)="cancelEdit()"
                  >
                    Cancel
                  </button>
                </td>
              </ng-container>
            </tr>
          </tbody>
          <tfoot>
            <tr class="total-row">
              <td><strong>Total</strong></td>
              <td>
                <strong>{{ totalSurface | number: "1.2-2" }} m²</strong>
              </td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- Add single room form -->
      <div class="form-card" *ngIf="showAddForm">
        <h4>Add Room</h4>
        <form [formGroup]="addForm" (ngSubmit)="saveAdd()">
          <div class="form-row">
            <div class="form-field">
              <label>Type <span class="required">*</span></label>
              <select
                formControlName="roomType"
                [class.invalid]="isInvalidAdd('roomType')"
              >
                <option value="">— Select type —</option>
                <option *ngFor="let t of allTypes" [value]="t">
                  {{ roomLabel(t) }}
                </option>
              </select>
              <span class="field-error" *ngIf="isInvalidAdd('roomType')"
                >Type is required</span
              >
            </div>
            <div class="form-field">
              <label>Surface (m²) <span class="required">*</span></label>
              <input
                type="number"
                formControlName="approximateSurface"
                [class.invalid]="isInvalidAdd('approximateSurface')"
                min="0.01"
                max="999.99"
                step="0.01"
                placeholder="e.g. 15.50"
              />
              <span
                class="field-error"
                *ngIf="isInvalidAdd('approximateSurface')"
              >
                Surface must be between 0.01 and 999.99
              </span>
            </div>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" [disabled]="saving">
              {{ saving ? "Saving…" : "Save" }}
            </button>
            <button
              type="button"
              class="btn btn-secondary"
              (click)="cancelAdd()"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>

      <!-- Quick Add form -->
      <div class="form-card" *ngIf="showQuickAdd">
        <h4>Quick Add Rooms</h4>
        <div *ngIf="quickError" class="form-error">{{ quickError }}</div>
        <form [formGroup]="quickForm" (ngSubmit)="saveQuickAdd()">
          <table class="quick-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Surface (m²)</th>
                <th></th>
              </tr>
            </thead>
            <tbody formArrayName="rows">
              <tr
                *ngFor="let row of quickRows.controls; let i = index"
                [formGroupName]="i"
              >
                <td>
                  <select
                    formControlName="roomType"
                    [class.invalid]="isRowInvalid(i, 'roomType')"
                  >
                    <option value="">— Select —</option>
                    <option *ngFor="let t of allTypes" [value]="t">
                      {{ roomLabel(t) }}
                    </option>
                  </select>
                </td>
                <td>
                  <input
                    type="number"
                    formControlName="approximateSurface"
                    [class.invalid]="isRowInvalid(i, 'approximateSurface')"
                    min="0.01"
                    max="999.99"
                    step="0.01"
                    placeholder="m²"
                  />
                </td>
                <td>
                  <button
                    type="button"
                    class="btn-icon"
                    (click)="removeQuickRow(i)"
                    [disabled]="quickRows.length <= 1"
                    title="Remove row"
                  >
                    ✕
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <div class="quick-footer">
            <button
              type="button"
              class="btn btn-sm btn-secondary"
              (click)="addQuickRow()"
              [disabled]="quickRows.length >= 20"
            >
              + Add Row
            </button>
            <span class="row-count">{{ quickRows.length }} / 20 rows</span>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" [disabled]="saving">
              {{ saving ? "Saving…" : "Save All" }}
            </button>
            <button
              type="button"
              class="btn btn-secondary"
              (click)="cancelQuickAdd()"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>

      <!-- Delete confirmation dialog -->
      <div class="dialog-overlay" *ngIf="roomToDelete">
        <div class="dialog">
          <h4>Delete this room?</h4>
          <p>
            <strong>{{ roomLabel(roomToDelete.roomType) }}</strong>
            — {{ roomToDelete.approximateSurface | number: "1.2-2" }} m²
          </p>
          <p class="warning">This action cannot be undone.</p>
          <div class="dialog-actions">
            <button class="btn btn-secondary" (click)="cancelDelete()">
              Cancel
            </button>
            <button
              class="btn btn-danger"
              (click)="confirmDelete()"
              [disabled]="deleting"
            >
              {{ deleting ? "Deleting…" : "Confirm" }}
            </button>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .room-section {
        margin-top: 1rem;
      }

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;
      }
      .section-header h3 {
        margin: 0;
      }
      .header-actions {
        display: flex;
        gap: 0.5rem;
      }

      /* Table */
      .table-wrapper {
        overflow-x: auto;
      }
      .rooms-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.95rem;
      }
      .rooms-table th,
      .rooms-table td {
        padding: 0.5rem 0.75rem;
        border-bottom: 1px solid #e0e0e0;
        text-align: left;
      }
      .rooms-table th {
        background: #f5f5f5;
        font-weight: 600;
      }
      .total-row td {
        border-top: 2px solid #ccc;
        border-bottom: none;
      }
      .actions {
        display: flex;
        gap: 0.4rem;
        white-space: nowrap;
      }

      /* Inline edit */
      .select-inline,
      .input-inline {
        padding: 0.3rem 0.5rem;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-size: 0.9rem;
        width: 100%;
      }

      /* Quick add table */
      .quick-table {
        width: 100%;
        border-collapse: collapse;
        margin-bottom: 0.75rem;
      }
      .quick-table th,
      .quick-table td {
        padding: 0.4rem 0.5rem;
      }
      .quick-table th {
        font-weight: 600;
        font-size: 0.85rem;
        color: #555;
      }
      .quick-table select,
      .quick-table input[type="number"] {
        width: 100%;
        padding: 0.3rem 0.5rem;
        border: 1px solid #ccc;
        border-radius: 4px;
      }
      .quick-table .invalid {
        border-color: #c62828;
      }
      .quick-footer {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-bottom: 0.75rem;
      }
      .row-count {
        font-size: 0.85rem;
        color: #888;
      }

      /* Form card */
      .form-card {
        background: #f9f9f9;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        padding: 1rem;
        margin-top: 0.5rem;
      }
      .form-card h4 {
        margin: 0 0 0.75rem 0;
      }
      .form-row {
        display: flex;
        gap: 1rem;
        flex-wrap: wrap;
      }
      .form-field {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        flex: 1;
        min-width: 150px;
      }
      .form-field label {
        font-weight: 500;
        font-size: 0.9rem;
      }
      .form-field select,
      .form-field input {
        padding: 0.4rem 0.6rem;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-size: 0.95rem;
      }
      .form-field select.invalid,
      .form-field input.invalid {
        border-color: #c62828;
      }
      .form-actions {
        display: flex;
        gap: 0.5rem;
        margin-top: 0.75rem;
      }

      /* Buttons */
      .btn {
        padding: 0.4rem 0.9rem;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.9rem;
      }
      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
      .btn-primary {
        background: #007bff;
        color: white;
      }
      .btn-primary:hover:not(:disabled) {
        background: #0056b3;
      }
      .btn-secondary {
        background: #6c757d;
        color: white;
      }
      .btn-secondary:hover:not(:disabled) {
        background: #545b62;
      }
      .btn-danger {
        background: #dc3545;
        color: white;
      }
      .btn-danger:hover:not(:disabled) {
        background: #a71d2a;
      }
      .btn-sm {
        padding: 0.3rem 0.65rem;
        font-size: 0.85rem;
      }
      .btn-icon {
        background: none;
        border: none;
        cursor: pointer;
        font-size: 1rem;
        padding: 0.1rem 0.3rem;
      }
      .btn-icon:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      /* States */
      .empty-state {
        color: #888;
        font-style: italic;
      }
      .loading {
        color: #555;
        padding: 0.5rem 0;
      }
      .required {
        color: #c62828;
      }
      .field-error {
        color: #c62828;
        font-size: 0.8rem;
      }
      .form-error {
        color: #c62828;
        font-size: 0.9rem;
        margin-bottom: 0.5rem;
      }
      .error-banner {
        background: #fdecea;
        border: 1px solid #f5c6cb;
        border-radius: 4px;
        padding: 0.5rem 0.75rem;
        color: #721c24;
        margin-bottom: 0.5rem;
      }
      .success-banner {
        background: #d4edda;
        border: 1px solid #c3e6cb;
        border-radius: 4px;
        padding: 0.5rem 0.75rem;
        color: #155724;
        margin-bottom: 0.5rem;
      }

      /* Delete dialog */
      .dialog-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.4);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
      }
      .dialog {
        background: white;
        border-radius: 8px;
        padding: 1.5rem;
        max-width: 380px;
        width: 90%;
      }
      .dialog h4 {
        margin: 0 0 0.75rem 0;
      }
      .dialog .warning {
        color: #c62828;
        font-size: 0.9rem;
      }
      .dialog-actions {
        display: flex;
        gap: 0.5rem;
        margin-top: 1rem;
        justify-content: flex-end;
      }
    `,
  ],
})
export class RoomSectionComponent implements OnInit, OnDestroy {
  @Input() unitId!: number;

  rooms: Room[] = [];
  totalSurface = 0;
  loading = false;
  saving = false;
  deleting = false;
  errorMessage = "";
  successMessage = "";

  // Add form
  showAddForm = false;
  addForm!: FormGroup;

  // Edit inline
  editingRoomId: number | null = null;
  editForm!: FormGroup;

  // Delete
  roomToDelete: Room | null = null;

  // Quick Add
  showQuickAdd = false;
  quickForm!: FormGroup;
  quickError = "";

  readonly allTypes = ALL_ROOM_TYPES;
  readonly roomLabel = (t: RoomType | string) =>
    ROOM_TYPE_LABELS[t as RoomType] ?? t;

  private destroy$ = new Subject<void>();

  constructor(
    private roomService: RoomService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.buildForms();
    this.loadRooms();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Typed edit form control accessors ───────────────────────────────────

  get editRoomType(): FormControl {
    return this.editForm.get("roomType") as FormControl;
  }

  get editApproximateSurface(): FormControl {
    return this.editForm.get("approximateSurface") as FormControl;
  }

  // ─── Load ─────────────────────────────────────────────────────────────────

  loadRooms(): void {
    this.loading = true;
    this.errorMessage = "";
    this.roomService
      .getRooms(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: RoomListResponse) => {
          this.rooms = res.rooms;
          this.totalSurface = res.totalSurface ?? 0;
          this.loading = false;
        },
        error: () => {
          this.errorMessage = "Failed to load rooms.";
          this.loading = false;
        },
      });
  }

  // ─── Forms builder ────────────────────────────────────────────────────────

  private buildForms(): void {
    this.addForm = this.fb.group({
      roomType: ["", Validators.required],
      approximateSurface: [
        null,
        [Validators.required, Validators.min(0.01), Validators.max(999.99)],
      ],
    });

    this.editForm = this.fb.group({
      roomType: ["", Validators.required],
      approximateSurface: [
        null,
        [Validators.required, Validators.min(0.01), Validators.max(999.99)],
      ],
    });

    this.quickForm = this.fb.group({
      rows: this.fb.array([
        this.buildQuickRow(),
        this.buildQuickRow(),
        this.buildQuickRow(),
      ]),
    });
  }

  private buildQuickRow(): FormGroup {
    return this.fb.group({
      roomType: [""],
      approximateSurface: [null],
    });
  }

  get quickRows(): FormArray {
    return this.quickForm.get("rows") as FormArray;
  }

  // ─── Add single room ──────────────────────────────────────────────────────

  openAddForm(): void {
    this.showAddForm = true;
    this.showQuickAdd = false;
    this.cancelEdit();
    this.addForm.reset();
    this.clearMessages();
  }

  cancelAdd(): void {
    this.showAddForm = false;
    this.addForm.reset();
  }

  isInvalidAdd(field: string): boolean {
    const c = this.addForm.get(field);
    return !!(c && c.invalid && c.touched);
  }

  saveAdd(): void {
    this.addForm.markAllAsTouched();
    if (this.addForm.invalid) return;
    this.saving = true;
    this.clearMessages();
    const { roomType, approximateSurface } = this.addForm.value;
    this.roomService
      .createRoom(this.unitId, { roomType, approximateSurface })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.showAddForm = false;
          this.addForm.reset();
          this.showSuccess("Room added successfully.");
          this.loadRooms();
        },
        error: (err) => {
          this.saving = false;
          this.errorMessage = err.error?.message ?? "Failed to add room.";
        },
      });
  }

  // ─── Inline edit ──────────────────────────────────────────────────────────

  startEdit(room: Room): void {
    this.editingRoomId = room.id;
    this.editForm.patchValue({
      roomType: room.roomType,
      approximateSurface: room.approximateSurface,
    });
    this.showAddForm = false;
    this.showQuickAdd = false;
    this.clearMessages();
  }

  cancelEdit(): void {
    this.editingRoomId = null;
    this.editForm.reset();
  }

  saveEdit(room: Room): void {
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid) return;
    this.saving = true;
    this.clearMessages();
    const { roomType, approximateSurface } = this.editForm.value;
    this.roomService
      .updateRoom(this.unitId, room.id, { roomType, approximateSurface })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.editingRoomId = null;
          this.showSuccess("Room updated successfully.");
          this.loadRooms();
        },
        error: (err) => {
          this.saving = false;
          this.errorMessage = err.error?.message ?? "Failed to update room.";
        },
      });
  }

  // ─── Delete ───────────────────────────────────────────────────────────────

  requestDelete(room: Room): void {
    this.roomToDelete = room;
    this.cancelEdit();
    this.clearMessages();
  }

  cancelDelete(): void {
    this.roomToDelete = null;
  }

  confirmDelete(): void {
    if (!this.roomToDelete) return;
    this.deleting = true;
    this.roomService
      .deleteRoom(this.unitId, this.roomToDelete.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.deleting = false;
          this.roomToDelete = null;
          this.showSuccess("Room deleted successfully.");
          this.loadRooms();
        },
        error: (err) => {
          this.deleting = false;
          this.roomToDelete = null;
          this.errorMessage = err.error?.message ?? "Failed to delete room.";
        },
      });
  }

  // ─── Quick Add ────────────────────────────────────────────────────────────

  openQuickAdd(): void {
    this.showQuickAdd = true;
    this.showAddForm = false;
    this.cancelEdit();
    this.clearMessages();
    this.quickError = "";
    while (this.quickRows.length > 0) this.quickRows.removeAt(0);
    for (let i = 0; i < 3; i++) this.quickRows.push(this.buildQuickRow());
  }

  cancelQuickAdd(): void {
    this.showQuickAdd = false;
    this.quickError = "";
  }

  addQuickRow(): void {
    if (this.quickRows.length < 20) {
      this.quickRows.push(this.buildQuickRow());
    }
  }

  removeQuickRow(index: number): void {
    if (this.quickRows.length > 1) {
      this.quickRows.removeAt(index);
    }
  }

  isRowInvalid(i: number, field: string): boolean {
    const row = this.quickRows.at(i) as FormGroup;
    const ctrl = row.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.touched || this.saving));
  }

  saveQuickAdd(): void {
    this.quickError = "";

    const validEntries: Array<{
      roomType: RoomType;
      approximateSurface: number;
    }> = [];
    let hasErrors = false;

    for (let i = 0; i < this.quickRows.length; i++) {
      const row = this.quickRows.at(i) as FormGroup;
      const { roomType, approximateSurface } = row.value as BatchRoomEntry;

      // Skip fully empty rows
      if (!roomType && approximateSurface === null) continue;

      // Partially filled → error
      if (
        !roomType ||
        approximateSurface === null ||
        approximateSurface <= 0 ||
        approximateSurface >= 1000
      ) {
        row.markAllAsTouched();
        hasErrors = true;
        continue;
      }

      validEntries.push({
        roomType: roomType as RoomType,
        approximateSurface: Number(approximateSurface),
      });
    }

    if (hasErrors) {
      this.quickError =
        "Some rows have invalid data. Please fix them or remove the rows.";
      return;
    }

    if (validEntries.length === 0) {
      this.quickError = "Please fill in at least one room.";
      return;
    }

    this.saving = true;
    this.roomService
      .batchCreateRooms(this.unitId, { rooms: validEntries })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (created) => {
          this.saving = false;
          this.showQuickAdd = false;
          this.showSuccess(`${created.length} room(s) added successfully.`);
          this.loadRooms();
        },
        error: (err) => {
          this.saving = false;
          this.quickError = err.error?.message ?? "Failed to save rooms.";
        },
      });
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private showSuccess(message: string): void {
    this.successMessage = message;
    setTimeout(() => (this.successMessage = ""), 4000);
  }

  private clearMessages(): void {
    this.errorMessage = "";
    this.successMessage = "";
  }
}

