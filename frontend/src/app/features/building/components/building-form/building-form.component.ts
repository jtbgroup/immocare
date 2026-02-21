import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BuildingService } from '../../../../core/services/building.service';
import { Building, CreateBuildingRequest, UpdateBuildingRequest } from '../../../../models/building.model';

@Component({
  selector: 'app-building-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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

  constructor(private fb: FormBuilder, private buildingService: BuildingService, private route: ActivatedRoute, private router: Router) {
    this.buildingForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      streetAddress: ['', [Validators.required, Validators.maxLength(200)]],
      postalCode: ['', [Validators.required, Validators.maxLength(20)]],
      city: ['', [Validators.required, Validators.maxLength(100)]],
      country: ['Belgium', [Validators.required, Validators.maxLength(100)]],
      ownerName: ['', [Validators.maxLength(200)]]
    });
  }

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['id']) { this.isEditMode = true; this.buildingId = +params['id']; this.loadBuilding(this.buildingId); }
    });
    this.buildingForm.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => { this.hasUnsavedChanges = true; });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  private loadBuilding(id: number): void {
    this.loading = true;
    this.buildingService.getBuildingById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (building) => { this.buildingForm.patchValue(building); this.loading = false; },
      error: () => { this.error = 'Failed to load building'; this.loading = false; }
    });
  }

  onSubmit(): void {
    if (this.buildingForm.invalid) { this.buildingForm.markAllAsTouched(); return; }
    this.loading = true;
    this.error = null;
    const formValue = this.buildingForm.value;
    const obs = this.isEditMode && this.buildingId
      ? this.buildingService.updateBuilding(this.buildingId, formValue as UpdateBuildingRequest)
      : this.buildingService.createBuilding(formValue as CreateBuildingRequest);
    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: (building) => { this.router.navigate(['/buildings', building.id]); },
      error: () => { this.error = 'Failed to save building'; this.loading = false; }
    });
  }

  onCancel(): void { this.router.navigate(['/buildings']); }

  hasError(field: string, error: string): boolean {
    const control = this.buildingForm.get(field);
    return !!(control && control.hasError(error) && (control.dirty || control.touched));
  }

  getErrorMessage(field: string): string {
    const control = this.buildingForm.get(field);
    if (!control) return '';
    if (control.hasError('required')) return `${field} is required`;
    if (control.hasError('maxlength')) return `${field} is too long`;
    return '';
  }
}
