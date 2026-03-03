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
  AddBoilerServiceRecordRequest,
  BoilerDTO,
  BoilerOwnerType,
  BoilerServiceRecordDTO,
  FUEL_TYPE_LABELS,
  SaveBoilerRequest,
  SERVICE_STATUS_CSS,
  SERVICE_STATUS_LABELS,
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
  error: string | null = null;

  // Boiler form state
  showForm = false;
  saving = false;
  editingId: number | null = null;
  saveError: string | null = null;
  form!: FormGroup;
  deleteConfirmId: number | null = null;

  // Service history state (keyed by boilerId)
  serviceHistoryMap: Record<number, BoilerServiceRecordDTO[]> = {};
  serviceHistoryOpen: Record<number, boolean> = {};
  serviceHistoryLoading: Record<number, boolean> = {};

  // Add service form state
  serviceFormOpenId: number | null = null;
  serviceForm!: FormGroup;
  savingService = false;
  serviceFormError: string | null = null;

  today = new Date().toISOString().split("T")[0];

  readonly FUEL_TYPE_LABELS = FUEL_TYPE_LABELS;
  readonly FUEL_TYPES = Object.keys(
    FUEL_TYPE_LABELS,
  ) as (keyof typeof FUEL_TYPE_LABELS)[];
  readonly SERVICE_STATUS_LABELS = SERVICE_STATUS_LABELS;
  readonly SERVICE_STATUS_CSS = SERVICE_STATUS_CSS;

  private destroy$ = new Subject<void>();

  constructor(
    private boilerService: BoilerService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.buildBoilerForm();
    this.buildServiceForm();
    this.loadBoilers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Boiler CRUD ──────────────────────────────────────────────────────────

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

  private buildBoilerForm(): void {
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
        error: () => {
          this.deleteConfirmId = null;
        },
      });
  }

  // ─── Service history ──────────────────────────────────────────────────────

  toggleServiceHistory(boilerId: number): void {
    const isOpen = this.serviceHistoryOpen[boilerId];
    this.serviceHistoryOpen[boilerId] = !isOpen;
    if (!isOpen && !this.serviceHistoryMap[boilerId]) {
      this.loadServiceHistory(boilerId);
    }
  }

  loadServiceHistory(boilerId: number): void {
    this.serviceHistoryLoading[boilerId] = true;
    this.boilerService
      .getServiceHistory(boilerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (records) => {
          this.serviceHistoryMap[boilerId] = records;
          this.serviceHistoryLoading[boilerId] = false;
        },
        error: () => {
          this.serviceHistoryLoading[boilerId] = false;
        },
      });
  }

  // ─── Add service form ─────────────────────────────────────────────────────

  private buildServiceForm(): void {
    this.serviceForm = this.fb.group({
      serviceDate: ["", Validators.required],
      validUntil: [""],
      notes: [""],
    });
  }

  openServiceForm(boilerId: number): void {
    this.serviceFormOpenId = boilerId;
    this.serviceFormError = null;
    this.serviceForm.reset();
    this.serviceHistoryOpen[boilerId] = true;
    if (!this.serviceHistoryMap[boilerId]) this.loadServiceHistory(boilerId);
  }

  closeServiceForm(): void {
    this.serviceFormOpenId = null;
    this.serviceFormError = null;
  }

  saveServiceRecord(boilerId: number): void {
    if (this.serviceForm.invalid) {
      this.serviceForm.markAllAsTouched();
      return;
    }
    this.savingService = true;
    this.serviceFormError = null;
    const req: AddBoilerServiceRecordRequest = {
      serviceDate: this.serviceForm.value.serviceDate,
      validUntil: this.serviceForm.value.validUntil || null,
      notes: this.serviceForm.value.notes || null,
    };
    this.boilerService
      .addServiceRecord(boilerId, req)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.savingService = false;
          this.closeServiceForm();
          delete this.serviceHistoryMap[boilerId];
          this.loadServiceHistory(boilerId);
          this.loadBoilers();
        },
        error: (err) => {
          this.savingService = false;
          this.serviceFormError =
            err?.error?.message ?? "Failed to save service record.";
        },
      });
  }
}
