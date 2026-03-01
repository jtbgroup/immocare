// features/boiler/components/boiler-section/boiler-section.component.ts — UC011
import { CommonModule } from "@angular/common";
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { BoilerService } from "../../../../core/services/boiler.service";
import {
  BoilerDTO,
  BoilerOwnerType,
  FUEL_TYPE_LABELS,
  SaveBoilerRequest,
} from "../../../../models/boiler.model";

// Re-export constant for template binding
const BOILEROWNER_HOUSING_UNIT_CONST = "HOUSING_UNIT";

@Component({
  selector: "app-boiler-section",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: "./boiler-section.component.html",
  styleUrls: ["./boiler-section.component.scss"],
})
export class BoilerSectionComponent implements OnInit, OnDestroy {
  @Input() ownerType!: BoilerOwnerType;
  @Input() ownerId!: number;

  boilers: BoilerDTO[] = [];
  loading = false;
  showForm = false;
  saving = false;
  editingId: number | null = null;
  error: string | null = null;
  saveError: string | null = null;
  deleteConfirmId: number | null = null;

  form!: FormGroup;
  today = new Date().toISOString().split("T")[0];

  readonly FUEL_TYPE_LABELS = FUEL_TYPE_LABELS;
  readonly FUEL_TYPES = Object.keys(
    FUEL_TYPE_LABELS,
  ) as (keyof typeof FUEL_TYPE_LABELS)[];

  private destroy$ = new Subject<void>();

  constructor(
    private boilerService: BoilerService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadBoilers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Load ─────────────────────────────────────────────────────────────────

  loadBoilers(): void {
    this.loading = true;
    this.error = null;
    const obs =
      this.ownerType === "HOUSING_UNIT"
        ? this.boilerService.getUnitBoilers(this.ownerId)
        : this.boilerService.getBuildingBoilers(this.ownerId);

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.boilers = data;
        this.loading = false;
      },
      error: () => {
        this.error = "Failed to load boilers.";
        this.loading = false;
      },
    });
  }

  // ─── Form ─────────────────────────────────────────────────────────────────

  buildForm(): void {
    this.form = this.fb.group({
      fuelType: ["GAS", Validators.required],
      installationDate: ["", Validators.required],
      brand: [""],
      model: [""],
      serialNumber: [""],
      lastServiceDate: [""],
      nextServiceDate: [""],
      notes: [""],
    });
  }

  openAddForm(): void {
    this.editingId = null;
    this.form.reset({ fuelType: "GAS" });
    this.showForm = true;
    this.saveError = null;
  }

  openEditForm(boiler: BoilerDTO): void {
    this.editingId = boiler.id;
    this.form.patchValue({
      fuelType: boiler.fuelType,
      installationDate: boiler.installationDate,
      brand: boiler.brand ?? "",
      model: boiler.model ?? "",
      serialNumber: boiler.serialNumber ?? "",
      lastServiceDate: boiler.lastServiceDate ?? "",
      nextServiceDate: boiler.nextServiceDate ?? "",
      notes: boiler.notes ?? "",
    });
    this.showForm = true;
    this.saveError = null;
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.saveError = null;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.saveError = null;

    const raw = this.form.value;
    const req: SaveBoilerRequest = {
      fuelType: raw.fuelType,
      installationDate: raw.installationDate,
      brand: raw.brand || null,
      model: raw.model || null,
      serialNumber: raw.serialNumber || null,
      lastServiceDate: raw.lastServiceDate || null,
      nextServiceDate: raw.nextServiceDate || null,
      notes: raw.notes || null,
    };

    const obs$ =
      this.editingId !== null
        ? this.boilerService.update(this.editingId, req)
        : this.ownerType === "HOUSING_UNIT"
          ? this.boilerService.createUnitBoiler(this.ownerId, req)
          : this.boilerService.createBuildingBoiler(this.ownerId, req);

    obs$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.closeForm();
        this.loadBoilers();
      },
      error: (err) => {
        this.saving = false;
        this.saveError = err?.error?.message ?? "Failed to save boiler.";
      },
    });
  }

  // ─── Delete ───────────────────────────────────────────────────────────────

  confirmDelete(id: number): void {
    this.deleteConfirmId = id;
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  doDelete(id: number): void {
    this.boilerService
      .delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.deleteConfirmId = null;
          this.loadBoilers();
        },
        error: () => {
          this.deleteConfirmId = null;
          this.error = "Failed to delete boiler.";
        },
      });
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  serviceAlertClass(boiler: BoilerDTO): string {
    if (!boiler.nextServiceDate) return "";
    if (boiler.daysUntilNextService !== null && boiler.daysUntilNextService < 0)
      return "alert-overdue";
    if (boiler.serviceAlert) return "alert-warning";
    return "";
  }

  fieldError(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }
}
