import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { HousingUnitService } from "../../../../core/services/housing-unit.service";
import {
  ORIENTATIONS,
  Orientation,
} from "../../../../models/housing-unit.model";
import { PersonSummary } from "../../../../models/person.model";
import { PersonPickerComponent } from "../../../../shared/components/person-picker/person-picker.component";

@Component({
  selector: "app-housing-unit-form",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PersonPickerComponent],
  templateUrl: "./housing-unit-form.component.html",
  styleUrls: ["./housing-unit-form.component.scss"],
})
export class HousingUnitFormComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  isEditMode = false;
  unitId?: number;
  buildingId?: number;
  saving = false;
  errorMessage = "";
  orientations: Orientation[] = ORIENTATIONS;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private housingUnitService: HousingUnitService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.buildingId = this.route.snapshot.queryParams["buildingId"]
      ? +this.route.snapshot.queryParams["buildingId"]
      : undefined;
    const id = this.route.snapshot.paramMap.get("id");
    this.isEditMode = !!id;
    this.buildForm();
    if (this.isEditMode && id) {
      this.unitId = +id;
      this.loadUnit(this.unitId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      unitNumber: ["", Validators.required],
      floor: [
        0,
        [Validators.required, Validators.min(-10), Validators.max(100)],
      ],
      landingNumber: [""],
      totalSurface: [null],
      owner: [null],
      hasTerrace: [false],
      terraceSurface: [null, [Validators.min(0.01)]],
      terraceOrientation: [""],
      hasGarden: [false],
      gardenSurface: [null, [Validators.min(0.01)]],
      gardenOrientation: [""],
    });

    // Dynamically add/remove required validator on surface fields
    this.form
      .get("hasTerrace")!
      .valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((checked: boolean) =>
        this.updateSurfaceValidators("terraceSurface", checked),
      );

    this.form
      .get("hasGarden")!
      .valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((checked: boolean) =>
        this.updateSurfaceValidators("gardenSurface", checked),
      );
  }

  private updateSurfaceValidators(field: string, required: boolean): void {
    const ctrl = this.form.get(field)!;
    if (required) {
      ctrl.setValidators([Validators.required, Validators.min(0.01)]);
    } else {
      ctrl.setValidators([Validators.min(0.01)]);
    }
    ctrl.updateValueAndValidity();
  }

  private loadUnit(id: number): void {
    this.housingUnitService
      .getUnitById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (unit) => {
          this.form.patchValue(unit);
          this.buildingId = unit.buildingId;
          if (unit.ownerId && unit.ownerName) {
            const parts = unit.ownerName.trim().split(" ");
            const ownerSummary: PersonSummary = {
              id: unit.ownerId,
              firstName: parts.slice(1).join(" ") ?? "",
              lastName: parts[0] ?? unit.ownerName,
            } as PersonSummary;
            this.form.patchValue({ owner: ownerSummary });
          }
        },
        error: () => {
          this.errorMessage = "Failed to load unit.";
        },
      });
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const { owner, ...rest } = this.form.value;
    const value = {
      ...rest,
      buildingId: this.buildingId,
      ownerId: (owner as PersonSummary | null)?.id ?? null,
      terraceOrientation: rest.terraceOrientation || null,
      gardenOrientation: rest.gardenOrientation || null,
    };
    const obs =
      this.isEditMode && this.unitId
        ? this.housingUnitService.update(this.unitId, value)
        : this.housingUnitService.create(value);
    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: (unit) => {
        this.router.navigate(["/units", unit.id]);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message ?? "Failed to save unit.";
      },
    });
  }

  onCancel(): void {
    if (this.buildingId) {
      this.router.navigate(["/buildings", this.buildingId]);
    } else {
      this.router.navigate(["/buildings"]);
    }
  }
}
