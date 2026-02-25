import { DecimalPipe } from "@angular/common";
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
  imports: [DecimalPipe, ReactiveFormsModule],
  templateUrl: "./room-section.component.html",
  styleUrls: ["./room-section.component.scss"],
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

      if (!roomType && approximateSurface === null) continue;

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

  showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => (this.successMessage = ""), 4000);
  }

  clearMessages(): void {
    this.errorMessage = "";
    this.successMessage = "";
  }
}
