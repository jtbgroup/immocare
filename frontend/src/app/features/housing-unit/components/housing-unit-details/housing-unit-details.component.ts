import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HousingUnit } from '../../../models/housing-unit.model';
import { HousingUnitService } from '../../../core/services/housing-unit.service';

@Component({
  selector: 'app-housing-unit-details',
  template: `
    <div class="details-container" *ngIf="unit">

      <!-- Header -->
      <div class="details-header">
        <div>
          <a [routerLink]="['/buildings', unit.buildingId]">{{ unit.buildingName }}</a>
          <span class="separator">›</span>
          <h2>Unit {{ unit.unitNumber }}</h2>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="edit()">Edit</button>
          <button class="btn btn-danger" (click)="confirmDelete()">Delete</button>
        </div>
      </div>

      <!-- Info section -->
      <section class="info-section">
        <h3>Information</h3>
        <div class="info-grid">
          <div><label>Unit Number</label><span>{{ unit.unitNumber }}</span></div>
          <div><label>Floor</label><span>{{ unit.floor }}</span></div>
          <div *ngIf="unit.landingNumber"><label>Landing</label><span>{{ unit.landingNumber }}</span></div>
          <div *ngIf="unit.totalSurface"><label>Total Surface</label><span>{{ unit.totalSurface }} m²</span></div>
          <div><label>Owner</label><span>{{ unit.effectiveOwnerName ?? '—' }}</span></div>
          <div *ngIf="unit.hasTerrace">
            <label>Terrace</label>
            <span>{{ unit.terraceSurface }} m² · {{ unit.terraceOrientation }}</span>
          </div>
          <div *ngIf="unit.hasGarden">
            <label>Garden</label>
            <span>{{ unit.gardenSurface }} m² · {{ unit.gardenOrientation }}</span>
          </div>
        </div>
      </section>

      <!-- Rooms placeholder -->
      <section class="placeholder-section">
        <div class="section-header">
          <h3>Rooms</h3>
          <button class="btn btn-sm" disabled title="Available in UC003">Add Room</button>
        </div>
        <p class="placeholder-text">Room management will be available in UC003.</p>
      </section>

      <!-- PEB placeholder -->
      <section class="placeholder-section">
        <div class="section-header">
          <h3>Energy Performance (PEB)</h3>
          <button class="btn btn-sm" disabled title="Available in UC004">Add PEB Score</button>
        </div>
        <p class="placeholder-text">PEB management will be available in UC004.</p>
      </section>

      <!-- Rent placeholder -->
      <section class="placeholder-section">
        <div class="section-header">
          <h3>Rent</h3>
          <button class="btn btn-sm" disabled title="Available in UC005">Update Rent</button>
        </div>
        <p class="placeholder-text">Rent management will be available in UC005.</p>
      </section>

      <!-- Water Meter placeholder -->
      <section class="placeholder-section">
        <div class="section-header">
          <h3>Water Meter</h3>
          <button class="btn btn-sm" disabled title="Available in UC006">Assign Meter</button>
        </div>
        <p class="placeholder-text">Water meter management will be available in UC006.</p>
      </section>
    </div>

    <div *ngIf="!unit && !loading" class="not-found">Unit not found.</div>
    <div *ngIf="loading" class="loading">Loading…</div>

    <!-- Confirm Delete Dialog -->
    <div class="dialog-overlay" *ngIf="showDeleteConfirm">
      <div class="dialog">
        <h3>Delete Housing Unit?</h3>
        <p>Unit <strong>{{ unit?.unitNumber }}</strong> will be permanently removed.</p>
        <p class="warning">This action cannot be undone.</p>
        <div class="dialog-error" *ngIf="deleteError">{{ deleteError }}</div>
        <div class="dialog-actions">
          <button class="btn btn-secondary" (click)="showDeleteConfirm = false">Cancel</button>
          <button class="btn btn-danger" (click)="delete()" [disabled]="deleting">
            {{ deleting ? 'Deleting…' : 'Confirm Delete' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .details-container { max-width: 800px; margin: 2rem auto; }
    .details-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; }
    .header-actions { display: flex; gap: 0.5rem; }
    .separator { margin: 0 0.5rem; color: #888; }
    section { margin-bottom: 2rem; border: 1px solid #e0e0e0; border-radius: 6px; padding: 1rem 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; }
    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem 1.5rem; margin-top: 0.75rem; }
    .info-grid label { color: #666; font-size: 0.85rem; display: block; }
    .placeholder-text { color: #aaa; font-style: italic; margin-top: 0.5rem; }
    .dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .dialog { background: white; padding: 2rem; border-radius: 8px; max-width: 440px; width: 90%; }
    .dialog-actions { display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem; }
    .warning { color: #c00; font-weight: 500; }
    .dialog-error { color: red; margin-bottom: 0.5rem; }
  `],
})
export class HousingUnitDetailsComponent implements OnInit, OnDestroy {
  unit?: HousingUnit;
  loading = false;
  showDeleteConfirm = false;
  deleting = false;
  deleteError = '';

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private housingUnitService: HousingUnitService
  ) {}

  ngOnInit(): void {
    const id = +(this.route.snapshot.paramMap.get('id') ?? 0);
    this.loading = true;
    this.housingUnitService.getUnitById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (unit) => { this.unit = unit; this.loading = false; },
        error: () => { this.loading = false; },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  edit(): void {
    this.router.navigate(['/units', this.unit!.id, 'edit']);
  }

  confirmDelete(): void {
    this.deleteError = '';
    this.showDeleteConfirm = true;
  }

  delete(): void {
    this.deleting = true;
    this.housingUnitService.delete(this.unit!.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.router.navigate(['/buildings', this.unit!.buildingId]),
        error: (err) => {
          this.deleteError = err.error?.message ?? 'Deletion failed. Please try again.';
          this.deleting = false;
        },
      });
  }
}
