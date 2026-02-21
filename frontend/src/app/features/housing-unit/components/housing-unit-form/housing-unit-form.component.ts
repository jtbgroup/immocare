import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HousingUnitService } from '../../../core/services/housing-unit.service';
import { ORIENTATIONS, Orientation } from '../../../models/housing-unit.model';

@Component({
  selector: 'app-housing-unit-form',
  template: `
    <div class="form-container">
      <h2>{{ isEditMode ? 'Edit Housing Unit' : 'New Housing Unit' }}</h2>

      <form [formGroup]="form" (ngSubmit)="onSubmit()">

        <!-- Core fields -->
        <div class="form-group">
          <label>Unit Number <span class="required">*</span></label>
          <input formControlName="unitNumber" placeholder="e.g. A101" />
          <div class="error" *ngIf="isInvalid('unitNumber')">Unit number is required.</div>
        </div>

        <div class="form-group">
          <label>Floor <span class="required">*</span></label>
          <input type="number" formControlName="floor" />
          <div class="error" *ngIf="isInvalid('floor')">Floor is required (−10 to 100).</div>
        </div>

        <div class="form-group">
          <label>Landing / Staircase</label>
          <input formControlName="landingNumber" placeholder="Optional" />
        </div>

        <div class="form-group">
          <label>Total Surface (m²)</label>
          <input type="number" step="0.01" formControlName="totalSurface" />
        </div>

        <div class="form-group">
          <label>Owner Name</label>
          <input formControlName="ownerName" placeholder="Leave empty to inherit from building" />
        </div>

        <!-- Terrace section -->
        <div class="section-toggle">
          <label>
            <input type="checkbox" formControlName="hasTerrace" />
            Has Terrace
          </label>
        </div>

        <div *ngIf="form.value.hasTerrace" class="sub-section">
          <div class="form-group">
            <label>Terrace Surface (m²) <span class="required">*</span></label>
            <input type="number" step="0.01" formControlName="terraceSurface" />
            <div class="error" *ngIf="isInvalid('terraceSurface')">Terrace surface is required.</div>
          </div>
          <div class="form-group">
            <label>Terrace Orientation <span class="required">*</span></label>
            <select formControlName="terraceOrientation">
              <option value="">— Select —</option>
              <option *ngFor="let o of orientations" [value]="o">{{ o }}</option>
            </select>
            <div class="error" *ngIf="isInvalid('terraceOrientation')">Terrace orientation is required.</div>
          </div>
        </div>

        <!-- Garden section -->
        <div class="section-toggle">
          <label>
            <input type="checkbox" formControlName="hasGarden" />
            Has Garden
          </label>
        </div>

        <div *ngIf="form.value.hasGarden" class="sub-section">
          <div class="form-group">
            <label>Garden Surface (m²) <span class="required">*</span></label>
            <input type="number" step="0.01" formControlName="gardenSurface" />
            <div class="error" *ngIf="isInvalid('gardenSurface')">Garden surface is required.</div>
          </div>
          <div class="form-group">
            <label>Garden Orientation <span class="required">*</span></label>
            <select formControlName="gardenOrientation">
              <option value="">— Select —</option>
              <option *ngFor="let o of orientations" [value]="o">{{ o }}</option>
            </select>
            <div class="error" *ngIf="isInvalid('gardenOrientation')">Garden orientation is required.</div>
          </div>
        </div>

        <div class="error-banner" *ngIf="errorMessage">{{ errorMessage }}</div>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" (click)="onCancel()">Cancel</button>
          <button type="submit" class="btn btn-primary" [disabled]="saving">
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .form-container { max-width: 600px; margin: 2rem auto; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.3rem; font-weight: 500; }
    .form-group input, .form-group select { width: 100%; padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; }
    .required { color: red; }
    .error { color: red; font-size: 0.85rem; margin-top: 0.2rem; }
    .section-toggle { margin: 1rem 0 0.5rem; }
    .sub-section { padding-left: 1.5rem; border-left: 3px solid #e0e0e0; margin-bottom: 1rem; }
    .form-actions { display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem; }
    .error-banner { color: red; margin-bottom: 1rem; padding: 0.5rem; background: #fff0f0; border-radius: 4px; }
  `],
})
export class HousingUnitFormComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  isEditMode = false;
  saving = false;
  errorMessage = '';
  orientations: Orientation[] = ORIENTATIONS;

  private unitId?: number;
  private buildingId?: number;
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private housingUnitService: HousingUnitService
  ) {}

  ngOnInit(): void {
    this.buildForm();

    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEditMode = true;
      this.unitId = +id;
      this.loadUnit(this.unitId);
    } else {
      const qp = this.route.snapshot.queryParamMap;
      this.buildingId = +(qp.get('buildingId') ?? 0);
    }

    // Dynamically apply validators for terrace
    this.form.get('hasTerrace')!.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((v) => this.toggleOutdoorValidators('terrace', v));

    // Dynamically apply validators for garden
    this.form.get('hasGarden')!.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((v) => this.toggleOutdoorValidators('garden', v));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      unitNumber:         ['', Validators.required],
      floor:              [0, [Validators.required, Validators.min(-10), Validators.max(100)]],
      landingNumber:      [''],
      totalSurface:       [null],
      ownerName:          [''],
      hasTerrace:         [false],
      terraceSurface:     [null],
      terraceOrientation: [''],
      hasGarden:          [false],
      gardenSurface:      [null],
      gardenOrientation:  [''],
    });
  }

  private loadUnit(id: number): void {
    this.housingUnitService.getUnitById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (unit) => this.form.patchValue(unit),
        error: () => (this.errorMessage = 'Failed to load unit.'),
      });
  }

  private toggleOutdoorValidators(feature: 'terrace' | 'garden', required: boolean): void {
    const surfaceCtrl      = this.form.get(`${feature}Surface`)!;
    const orientationCtrl  = this.form.get(`${feature}Orientation`)!;

    if (required) {
      surfaceCtrl.setValidators([Validators.required, Validators.min(0.01)]);
      orientationCtrl.setValidators(Validators.required);
    } else {
      surfaceCtrl.clearValidators();
      orientationCtrl.clearValidators();
    }
    surfaceCtrl.updateValueAndValidity();
    orientationCtrl.updateValueAndValidity();
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched));
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    const value = this.form.value;

    if (this.isEditMode && this.unitId) {
      this.housingUnitService.update(this.unitId, value)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (unit) => this.router.navigate(['/units', unit.id]),
          error: (err) => {
            this.errorMessage = err.error?.message ?? 'Update failed.';
            this.saving = false;
          },
        });
    } else {
      const request = { ...value, buildingId: this.buildingId };
      this.housingUnitService.create(request)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (unit) => this.router.navigate(['/units', unit.id]),
          error: (err) => {
            this.errorMessage = err.error?.message ?? 'Creation failed.';
            this.saving = false;
          },
        });
    }
  }

  onCancel(): void {
    if (this.isEditMode && this.unitId) {
      this.router.navigate(['/units', this.unitId]);
    } else {
      this.router.navigate(['/buildings', this.buildingId]);
    }
  }
}
