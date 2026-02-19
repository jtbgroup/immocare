import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BuildingService } from '../../../../core/services/building.service';
import { Building, CreateBuildingRequest, UpdateBuildingRequest } from '../../../../models/building.model';

/**
 * Component for creating and editing buildings.
 * Implements US001 - Create Building and US002 - Edit Building.
 */
@Component({
  selector: 'app-building-form',
  templateUrl: './building-form.component.html',
  styleUrls: ['./building-form.component.scss']
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
    private router: Router
  ) {
    this.buildingForm = this.createForm();
  }

  ngOnInit(): void {
    // Check if we're in edit mode
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.buildingId = +params['id'];
        this.loadBuilding(this.buildingId);
      }
    });

    // Track form changes
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

  /**
   * Create the building form with validation.
   */
  private createForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      streetAddress: ['', [Validators.required, Validators.maxLength(200)]],
      postalCode: ['', [Validators.required, Validators.maxLength(20)]],
      city: ['', [Validators.required, Validators.maxLength(100)]],
      country: ['Belgium', [Validators.required, Validators.maxLength(100)]],
      ownerName: ['', [Validators.maxLength(200)]]
    });
  }

  /**
   * Load building data for editing.
   */
  private loadBuilding(id: number): void {
    this.loading = true;
    this.buildingService.getBuildingById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (building: Building) => {
          this.buildingForm.patchValue({
            name: building.name,
            streetAddress: building.streetAddress,
            postalCode: building.postalCode,
            city: building.city,
            country: building.country,
            ownerName: building.ownerName || ''
          });
          this.hasUnsavedChanges = false;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to load building';
          this.loading = false;
          console.error('Error loading building:', err);
        }
      });
  }

  /**
   * Submit the form.
   */
  onSubmit(): void {
    if (this.buildingForm.invalid) {
      this.markFormGroupTouched(this.buildingForm);
      return;
    }

    this.loading = true;
    this.error = null;

    const formValue = this.buildingForm.value;
    const request = {
      name: formValue.name,
      streetAddress: formValue.streetAddress,
      postalCode: formValue.postalCode,
      city: formValue.city,
      country: formValue.country,
      ownerName: formValue.ownerName || undefined
    };

    if (this.isEditMode && this.buildingId) {
      this.updateBuilding(this.buildingId, request);
    } else {
      this.createBuilding(request);
    }
  }

  /**
   * Create a new building.
   */
  private createBuilding(request: CreateBuildingRequest): void {
    this.buildingService.createBuilding(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (building: Building) => {
          this.hasUnsavedChanges = false;
          this.router.navigate(['/buildings', building.id]);
        },
        error: (err) => {
          this.error = this.extractErrorMessage(err);
          this.loading = false;
          console.error('Error creating building:', err);
        }
      });
  }

  /**
   * Update an existing building.
   */
  private updateBuilding(id: number, request: UpdateBuildingRequest): void {
    this.buildingService.updateBuilding(id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (building: Building) => {
          this.hasUnsavedChanges = false;
          this.router.navigate(['/buildings', building.id]);
        },
        error: (err) => {
          this.error = this.extractErrorMessage(err);
          this.loading = false;
          console.error('Error updating building:', err);
        }
      });
  }

  /**
   * Cancel form and navigate back.
   */
  onCancel(): void {
    if (this.hasUnsavedChanges) {
      const confirmed = confirm('You have unsaved changes. Are you sure you want to cancel?');
      if (!confirmed) {
        return;
      }
    }

    if (this.isEditMode && this.buildingId) {
      this.router.navigate(['/buildings', this.buildingId]);
    } else {
      this.router.navigate(['/buildings']);
    }
  }

  /**
   * Check if a field has an error.
   */
  hasError(fieldName: string, errorType: string): boolean {
    const field = this.buildingForm.get(fieldName);
    return !!(field && field.hasError(errorType) && (field.dirty || field.touched));
  }

  /**
   * Get error message for a field.
   */
  getErrorMessage(fieldName: string): string {
    const field = this.buildingForm.get(fieldName);
    if (!field || !field.errors) {
      return '';
    }

    if (field.hasError('required')) {
      return `${this.getFieldLabel(fieldName)} is required`;
    }
    if (field.hasError('maxlength')) {
      const maxLength = field.errors['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} must be ${maxLength} characters or less`;
    }
    return '';
  }

  /**
   * Get human-readable label for field.
   */
  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      name: 'Building name',
      streetAddress: 'Street address',
      postalCode: 'Postal code',
      city: 'City',
      country: 'Country',
      ownerName: 'Owner name'
    };
    return labels[fieldName] || fieldName;
  }

  /**
   * Mark all fields as touched to show validation errors.
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  /**
   * Extract error message from HTTP error response.
   */
  private extractErrorMessage(error: any): string {
    if (error.error?.message) {
      return error.error.message;
    }
    if (error.error?.fieldErrors) {
      const errors = Object.values(error.error.fieldErrors).join(', ');
      return `Validation errors: ${errors}`;
    }
    return 'An error occurred. Please try again.';
  }
}
