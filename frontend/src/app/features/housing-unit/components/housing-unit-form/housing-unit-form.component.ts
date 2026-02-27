import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
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

/**
 * Cross-field validator: when the feature is enabled (hasFeature = true) and
 * a surface value > 0 has been entered, the orientation field is required.
 */
function orientationRequiredWhenSurface(
  hasFlagField: string,
  surfaceField: string,
  orientationField: string,
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const hasFlag = group.get(hasFlagField)?.value as boolean;
    const surface = group.get(surfaceField)?.value as number | null;
    const orientation = group.get(orientationField)?.value as string | null;

    if (hasFlag && surface && surface > 0 && !orientation) {
      return { [`${orientationField}Required`]: true };
    }
    return null;
  };
}

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
    this.form = this.fb.group(
      {
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
      },
      {
        validators: [
          orientationRequiredWhenSurface("hasTerrace", "terraceSurface", "terraceOrientation"),
          orientationRequiredWhenSurface("hasGarden", "gardenSurface", "gardenOrientation"),
        ],
      },
    );

    // Re-validate when relevant fields change so error messages update live
    ["hasTerrace", "terraceSurface", "hasGarden", "gardenSurface"].forEach((field) => {
      this.form.get(field)!.valueChanges
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => this.form.updateValueAndValidity());
    });
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

  /** Returns true when the orientation is required but missing (after touch). */
  isOrientationRequired(orientationField: string): boolean {
    const errorKey = `${orientationField}Required`;
    return !!(
      this.form.errors?.[errorKey] &&
      (this.form.get(orientationField)?.touched || this.form.touched)
    );
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
