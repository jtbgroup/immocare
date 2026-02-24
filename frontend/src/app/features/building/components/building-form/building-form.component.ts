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
import { BuildingService } from "../../../../core/services/building.service";
import {
  Building,
  CreateBuildingRequest,
  UpdateBuildingRequest,
} from "../../../../models/building.model";
import { PersonSummary } from "../../../../models/person.model";
import { PersonPickerComponent } from "../../../../shared/components/person-picker/person-picker.component";

@Component({
  selector: "app-building-form",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PersonPickerComponent],
  templateUrl: "./building-form.component.html",
  styleUrls: ["./building-form.component.scss"],
})
export class BuildingFormComponent implements OnInit, OnDestroy {
  buildingForm: FormGroup;
  isEditMode = false;
  buildingId?: number;
  loading = false;
  error: string | null = null;
  hasUnsavedChanges = false;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private buildingService: BuildingService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.buildingForm = this.fb.group({
      name: ["", [Validators.required, Validators.maxLength(100)]],
      streetAddress: ["", [Validators.required, Validators.maxLength(200)]],
      postalCode: ["", [Validators.required, Validators.maxLength(20)]],
      city: ["", [Validators.required, Validators.maxLength(100)]],
      country: ["Belgium", [Validators.required, Validators.maxLength(100)]],
      // owner holds the full PersonSummary object (via ControlValueAccessor)
      owner: [null],
    });
  }

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      if (params["id"]) {
        this.isEditMode = true;
        this.buildingId = +params["id"];
        this.loadBuilding(this.buildingId);
      }
    });
    this.buildingForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.hasUnsavedChanges = true;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadBuilding(id: number): void {
    this.loading = true;
    this.buildingService
      .getBuildingById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (building: Building) => {
          // Build a PersonSummary stub from ownerId + ownerName for the picker
          let ownerSummary: PersonSummary | null = null;
          if (building.ownerId && building.ownerName) {
            const parts = building.ownerName.trim().split(" ");
            ownerSummary = {
              id: building.ownerId,
              firstName: parts[0] ?? "",
              lastName: parts.slice(1).join(" ") || parts[0],
              city: undefined,
              nationalId: undefined,
              isOwner: true,
              isTenant: false,
            };
          }

          this.buildingForm.patchValue({
            name: building.name,
            streetAddress: building.streetAddress,
            postalCode: building.postalCode,
            city: building.city,
            country: building.country,
            owner: ownerSummary,
          });
          this.loading = false;
        },
        error: () => {
          this.error = "Failed to load building";
          this.loading = false;
        },
      });
  }

  onSubmit(): void {
    if (this.buildingForm.invalid) {
      this.buildingForm.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.error = null;

    const { owner, ...rest } = this.buildingForm.value;
    const ownerId: number | null = (owner as PersonSummary | null)?.id ?? null;

    const payload = { ...rest, ownerId };

    const obs =
      this.isEditMode && this.buildingId
        ? this.buildingService.updateBuilding(
            this.buildingId,
            payload as UpdateBuildingRequest,
          )
        : this.buildingService.createBuilding(payload as CreateBuildingRequest);

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: (building) => {
        this.router.navigate(["/buildings", building.id]);
      },
      error: () => {
        this.error = "Failed to save building";
        this.loading = false;
      },
    });
  }

  onCancel(): void {
    this.router.navigate(["/buildings"]);
  }

  hasError(field: string, error: string): boolean {
    const control = this.buildingForm.get(field);
    return !!(
      control &&
      control.hasError(error) &&
      (control.dirty || control.touched)
    );
  }

  getErrorMessage(field: string): string {
    const control = this.buildingForm.get(field);
    if (!control) return "";
    if (control.hasError("required")) return `${field} is required`;
    if (control.hasError("maxlength")) return `${field} is too long`;
    return "";
  }
}
