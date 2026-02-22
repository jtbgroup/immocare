import { CommonModule } from "@angular/common";
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { HousingUnitService } from "../../../../core/services/housing-unit.service";
import { HousingUnit } from "../../../../models/housing-unit.model";
import { PEB_SCORE_DISPLAY } from "../../../../models/peb-score.model";

@Component({
  selector: "app-housing-unit-list",
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="unit-list-container">
      <div class="unit-list-header">
        <h3>Housing Units</h3>
        <button class="btn btn-primary" (click)="addUnit()">
          Add Housing Unit
        </button>
      </div>
      <div *ngIf="loading" class="loading">Loading units…</div>
      <div *ngIf="!loading && units.length === 0" class="empty-state">
        <p>No units yet. Click "Add Housing Unit" to get started.</p>
      </div>
      <table *ngIf="!loading && units.length > 0" class="unit-table">
        <thead>
          <tr>
            <th>Unit #</th>
            <th>Floor</th>
            <th>Surface (m²)</th>
            <th>Rooms</th>
            <th>Rent</th>
            <th>PEB</th>
            <th>Owner</th>
          </tr>
        </thead>
        <tbody>
          <tr
            *ngFor="let unit of units"
            class="unit-row"
            (click)="viewUnit(unit.id)"
          >
            <td>{{ unit.unitNumber }}</td>
            <td>{{ unit.floor }}</td>
            <td>{{ unit.totalSurface ?? "—" }}</td>
            <td>{{ unit.roomCount }}</td>
            <td>
              {{
                unit.currentMonthlyRent
                  ? "€" + unit.currentMonthlyRent + "/mo"
                  : "—"
              }}
            </td>
            <td>
              <span
                *ngIf="unit.currentPebScore"
                class="peb-badge"
                [style.background]="pebDisplay[unit.currentPebScore].color"
                [style.color]="pebDisplay[unit.currentPebScore].textColor"
              >
                {{ pebDisplay[unit.currentPebScore].label }}
              </span>
              <span *ngIf="!unit.currentPebScore">—</span>
            </td>
            <td>{{ unit.ownerName ?? "—" }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [
    `
      .unit-list-container {
        margin-top: 1.5rem;
      }
      .unit-list-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }
      .unit-table {
        width: 100%;
        border-collapse: collapse;
      }
      .unit-table th,
      .unit-table td {
        padding: 0.6rem 1rem;
        border-bottom: 1px solid #e0e0e0;
        text-align: left;
      }
      .unit-row {
        cursor: pointer;
      }
      .unit-row:hover {
        background: #f5f5f5;
      }
      .empty-state {
        color: #888;
        padding: 1rem 0;
      }
      .loading {
        padding: 1rem 0;
      }
      .btn {
        padding: 0.5rem 1rem;
        border: none;
        border-radius: 4px;
        cursor: pointer;
      }
      .btn-primary {
        background: #007bff;
        color: white;
      }
      .btn-primary:hover {
        background: #0056b3;
      }
      .peb-badge {
        display: inline-block;
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: 700;
        font-size: 0.85rem;
        min-width: 32px;
        text-align: center;
      }
    `,
  ],
})
export class HousingUnitListComponent implements OnInit, OnDestroy {
  @Input() buildingId!: number;
  units: HousingUnit[] = [];
  loading = false;
  private destroy$ = new Subject<void>();
  readonly pebDisplay = PEB_SCORE_DISPLAY;

  constructor(
    private housingUnitService: HousingUnitService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadUnits();
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadUnits(): void {
    if (!this.buildingId) return;
    this.loading = true;
    this.housingUnitService
      .getUnitsByBuilding(this.buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (units) => {
          this.units = units;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  viewUnit(id: number): void {
    this.router.navigate(["/units", id]);
  }
  addUnit(): void {
    this.router.navigate(["/units/new"], {
      queryParams: { buildingId: this.buildingId },
    });
  }
}
