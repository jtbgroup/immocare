import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BuildingService } from '../../../../core/services/building.service';
import { HousingUnitService } from '../../../../core/services/housing-unit.service';
import { LeaseService } from '../../../../core/services/lease.service';
import { TagSubcategoryService } from '../../../../core/services/tag-subcategory.service';
import { LeaseGlobalFilters, LeaseGlobalSummary } from '../../../../models/lease.model';
import { ImportPreviewRow, TagSubcategory, TransactionDirection } from '../../../../models/transaction.model';
import { Building } from '../../../../models/building.model';
import { HousingUnit } from '../../../../models/housing-unit.model';

@Component({
  selector: 'app-import-row-detail-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="detail-panel" *ngIf="row">

  <div class="panel-header">
    <h3>Row {{ row.rowNumber }}</h3>
    <button class="btn-close" (click)="close.emit()">✕</button>
  </div>

  <div class="panel-body">

    <!-- Raw info -->
    <div class="panel-section">
      <div class="panel-meta">
        <span class="meta-label">Date</span>
        <span>{{ row.transactionDate ?? '—' }}</span>
      </div>
      <div class="panel-meta">
        <span class="meta-label">Amount</span>
        <span [class.income]="row.direction === 'INCOME'" [class.expense]="row.direction === 'EXPENSE'">
          {{ row.amount?.toFixed(2) }} €
        </span>
      </div>
      <div class="panel-meta" *ngIf="row.counterpartyName">
        <span class="meta-label">Counterparty</span>
        <span>{{ row.counterpartyName }}</span>
      </div>
      <div class="panel-meta" *ngIf="row.counterpartyAccount">
        <span class="meta-label">IBAN</span>
        <span class="monospace">{{ row.counterpartyAccount }}</span>
      </div>
      <div class="panel-meta" *ngIf="row.description">
        <span class="meta-label">Description</span>
        <span class="description-text">{{ row.description }}</span>
      </div>
    </div>

    <!-- Direction -->
    <div class="panel-section">
      <label class="field-label">Direction</label>
      <div class="direction-toggle">
        <button
          class="dir-btn"
          [class.dir-btn--active]="row.direction === 'INCOME'"
          (click)="setDirection('INCOME')">
          ↑ Income
        </button>
        <button
          class="dir-btn"
          [class.dir-btn--active]="row.direction === 'EXPENSE'"
          (click)="setDirection('EXPENSE')">
          ↓ Expense
        </button>
      </div>
    </div>

    <!-- Subcategory -->
    <div class="panel-section">
      <label class="field-label">Subcategory</label>

      <!-- Suggestion shortcut -->
      <div *ngIf="row.suggestedSubcategory && !selectedSubcategoryId" class="suggestion-pill"
           (click)="acceptSubcategorySuggestion()">
        ✨ Accept: {{ row.suggestedSubcategory.categoryName }} / {{ row.suggestedSubcategory.subcategoryName }}
      </div>

      <select class="form-control" [(ngModel)]="selectedSubcategoryId" (ngModelChange)="onSubcategoryChange($event)">
        <option [ngValue]="null">— None —</option>
        <optgroup *ngFor="let group of groupedSubcategories" [label]="group.categoryName">
          <option *ngFor="let sub of group.subcategories" [ngValue]="sub.id">
            {{ sub.name }}
          </option>
        </optgroup>
      </select>
    </div>

    <!-- Lease -->
    <div class="panel-section">
      <label class="field-label">Link to lease / unit</label>

      <!-- Suggestion shortcut -->
      <div *ngIf="row.suggestedLease && !selectedLeaseId" class="suggestion-pill suggestion-pill--lease"
           (click)="acceptLeaseSuggestion()">
        🏠 Accept: {{ row.suggestedLease.unitNumber }} · {{ row.suggestedLease.buildingName }}
        ({{ row.suggestedLease.personFullName }})
      </div>

      <!-- Building filter -->
      <select class="form-control" [(ngModel)]="filterBuildingId" (ngModelChange)="onBuildingFilterChange($event)">
        <option [ngValue]="null">— All buildings —</option>
        <option *ngFor="let b of buildings" [ngValue]="b.id">{{ b.name }}</option>
      </select>

      <!-- Lease selector -->
      <select class="form-control mt-xs" [(ngModel)]="selectedLeaseId" (ngModelChange)="onLeaseChange($event)"
              [disabled]="filteredLeases.length === 0">
        <option [ngValue]="null">— No lease —</option>
        <option *ngFor="let l of filteredLeases" [ngValue]="l.id">
          {{ l.housingUnitNumber }} · {{ l.buildingName }} ({{ l.tenantNames.join(', ') }})
        </option>
      </select>
    </div>

  </div>

</div>
  `,
  styles: [`
.detail-panel {
  width: 360px;
  min-width: 320px;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;

  h3 { margin: 0; font-size: 1rem; }
}

.btn-close {
  background: none;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  color: #6b7280;
  padding: 0.25rem;
  &:hover { color: #111; }
}

.panel-body {
  padding: 1rem 1.25rem;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.panel-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.panel-meta {
  display: flex;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.meta-label {
  font-weight: 600;
  min-width: 90px;
  color: #6b7280;
}

.description-text {
  font-size: 0.8rem;
  color: #374151;
  word-break: break-word;
}

.field-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.direction-toggle {
  display: flex;
  gap: 0.5rem;
}

.dir-btn {
  flex: 1;
  padding: 0.4rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #f9fafb;
  cursor: pointer;
  font-size: 0.875rem;
  color: #374151;

  &--active {
    background: #e0f2fe;
    border-color: #0284c7;
    color: #0284c7;
    font-weight: 600;
  }

  &:hover:not(.dir-btn--active) {
    background: #f0f0f0;
  }
}

.suggestion-pill {
  padding: 0.4rem 0.75rem;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 20px;
  font-size: 0.8rem;
  cursor: pointer;
  color: #92400e;
  transition: background 0.15s;

  &:hover { background: #fef3c7; }

  &--lease {
    background: #f0fdf4;
    border-color: #86efac;
    color: #166534;
    &:hover { background: #dcfce7; }
  }
}

.form-control {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.875rem;
  background: #fff;

  &:disabled { background: #f3f4f6; color: #9ca3af; }
}

.mt-xs { margin-top: 0.5rem; }

.income { color: #16a34a; font-weight: 600; }
.expense { color: #dc2626; font-weight: 600; }
.monospace { font-family: monospace; font-size: 0.8rem; }
  `]
})
export class ImportRowDetailPanelComponent implements OnInit, OnChanges {

  @Input() row!: ImportPreviewRow;
  @Output() close = new EventEmitter<void>();
  @Output() rowChanged = new EventEmitter<ImportPreviewRow>();

  // Subcategory data
  allSubcategories: TagSubcategory[] = [];
  groupedSubcategories: { categoryName: string; subcategories: TagSubcategory[] }[] = [];
  selectedSubcategoryId: number | null = null;

  // Lease / building data
  buildings: Building[] = [];
  allLeases: LeaseGlobalSummary[] = [];
  filteredLeases: LeaseGlobalSummary[] = [];
  filterBuildingId: number | null = null;
  selectedLeaseId: number | null = null;

  constructor(
    private tagSubcategoryService: TagSubcategoryService,
    private buildingService: BuildingService,
    private leaseService: LeaseService,
  ) {}

  ngOnInit(): void {
    this.loadSubcategories();
    this.loadBuildings();
    this.loadLeases();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['row'] && this.row) {
      this.syncFromRow();
    }
  }

  private syncFromRow(): void {
    this.selectedSubcategoryId = this.row.enrichedSubcategoryId ?? null;
    this.selectedLeaseId = this.row.enrichedLeaseId ?? null;
    if (this.selectedLeaseId && this.allLeases.length) {
      const lease = this.allLeases.find(l => l.id === this.selectedLeaseId);
      this.filterBuildingId = lease?.buildingId ?? null;
      this.applyBuildingFilter();
    }
  }

  private loadSubcategories(): void {
    this.tagSubcategoryService.getAll().subscribe((subs) => {
      this.allSubcategories = subs;
      this.groupedSubcategories = this.groupBy(subs);
    });
  }

  private loadBuildings(): void {
    this.buildingService.getAllBuildings(0, 200, 'name,asc').subscribe((page) => {
      this.buildings = page.content ?? (page as any);
    });
  }

  private loadLeases(): void {
    const emptyFilters: LeaseGlobalFilters = {
      statuses: ['ACTIVE'],
      leaseType: '',
      startDateFrom: '', startDateTo: '',
      endDateFrom: '', endDateTo: '',
      rentMin: null, rentMax: null,
    };
    this.leaseService.getAll(emptyFilters, 0, 200).subscribe((page) => {
      this.allLeases = page.content;
      this.filteredLeases = page.content;
      if (this.row) this.syncFromRow();
    });
  }

  // ── User actions ──────────────────────────────────────────────────────────

  setDirection(dir: TransactionDirection): void {
    this.row.direction = dir;
    // Re-filter subcategories by direction
    this.groupedSubcategories = this.groupBy(
      this.allSubcategories.filter(s =>
        s.direction === dir || s.direction === 'BOTH'
      )
    );
    this.emit();
  }

  acceptSubcategorySuggestion(): void {
    if (!this.row.suggestedSubcategory) return;
    this.selectedSubcategoryId = this.row.suggestedSubcategory.subcategoryId;
    this.onSubcategoryChange(this.selectedSubcategoryId);
  }

  onSubcategoryChange(id: number | null): void {
    this.row.enrichedSubcategoryId = id ?? undefined;
    const sub = this.allSubcategories.find(s => s.id === id);
    this.row.enrichedSubcategoryName = sub ? `${sub.categoryName} / ${sub.name}` : undefined;
    this.emit();
  }

  acceptLeaseSuggestion(): void {
    if (!this.row.suggestedLease) return;
    this.selectedLeaseId = this.row.suggestedLease.leaseId;
    this.filterBuildingId = null;
    this.filteredLeases = this.allLeases;
    this.onLeaseChange(this.selectedLeaseId);
  }

  onBuildingFilterChange(buildingId: number | null): void {
    this.filterBuildingId = buildingId;
    this.applyBuildingFilter();
  }

  applyBuildingFilter(): void {
    this.filteredLeases = this.filterBuildingId
      ? this.allLeases.filter(l => l.buildingId === this.filterBuildingId)
      : this.allLeases;
  }

  onLeaseChange(leaseId: number | null): void {
    const lease = this.allLeases.find(l => l.id === leaseId);
    this.row.enrichedLeaseId = leaseId ?? undefined;
    this.row.enrichedUnitId = lease?.housingUnitId;
    this.row.enrichedUnitNumber = lease?.housingUnitNumber;
    this.row.enrichedBuildingId = lease?.buildingId;
    this.row.enrichedBuildingName = lease?.buildingName;
    this.emit();
  }

  private emit(): void {
    this.rowChanged.emit({ ...this.row });
  }

  private groupBy(subs: TagSubcategory[]): { categoryName: string; subcategories: TagSubcategory[] }[] {
    const map = new Map<string, TagSubcategory[]>();
    subs.forEach(s => {
      const list = map.get(s.categoryName) ?? [];
      list.push(s);
      map.set(s.categoryName, list);
    });
    return Array.from(map.entries()).map(([categoryName, subcategories]) => ({ categoryName, subcategories }));
  }
}
