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
import { AppDatePipe } from "../../../../shared/pipes/app-date.pipe";

@Component({
  selector: "app-boiler-section",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppDatePipe],
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

  private buildForm(): void {
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
    this.saveError = null;
    this.form.reset({ fuelType: "GAS" });
    this.showForm = true;
  }

  openEditForm(boiler: BoilerDTO): void {
    this.editingId = boiler.id;
    this.saveError = null;
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

    const req: SaveBoilerRequest = {
      fuelType: this.form.value.fuelType,
      installationDate: this.form.value.installationDate,
      brand: this.form.value.brand || null,
      model: this.form.value.model || null,
      serialNumber: this.form.value.serialNumber || null,
      lastServiceDate: this.form.value.lastServiceDate || null,
      nextServiceDate: this.form.value.nextServiceDate || null,
      notes: this.form.value.notes || null,
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
        error: (err) => {
          this.error = err?.error?.message ?? "Failed to delete boiler.";
          this.deleteConfirmId = null;
        },
      });
  }

  fieldError(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!ctrl && ctrl.invalid && ctrl.touched;
  }
}
