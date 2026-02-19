import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BuildingService } from '../../../../core/services/building.service';
import { Building } from '../../../../models/building.model';

/**
 * Component for displaying building details.
 * Implements viewing and deleting functionality from UC001.
 */
@Component({
  selector: 'app-building-details',
  templateUrl: './building-details.component.html',
  styleUrls: ['./building-details.component.scss']
})
export class BuildingDetailsComponent implements OnInit, OnDestroy {
  building?: Building;
  loading = false;
  error: string | null = null;
  showDeleteConfirm = false;
  deleteError: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private buildingService: BuildingService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.loadBuilding(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load building details.
   */
  private loadBuilding(id: number): void {
    this.loading = true;
    this.error = null;

    this.buildingService.getBuildingById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (building: Building) => {
          this.building = building;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Building not found';
          this.loading = false;
          console.error('Error loading building:', err);
        }
      });
  }

  /**
   * Navigate to edit form.
   */
  editBuilding(): void {
    if (this.building) {
      this.router.navigate(['/buildings', this.building.id, 'edit']);
    }
  }

  /**
   * Show delete confirmation dialog.
   */
  confirmDelete(): void {
    this.showDeleteConfirm = true;
    this.deleteError = null;
  }

  /**
   * Cancel delete operation.
   */
  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deleteError = null;
  }

  /**
   * Delete the building.
   * Implements US003 - Delete Building.
   */
  deleteBuilding(): void {
    if (!this.building) {
      return;
    }

    this.loading = true;
    this.deleteError = null;

    this.buildingService.deleteBuilding(this.building.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.router.navigate(['/buildings']);
        },
        error: (err) => {
          this.loading = false;
          this.showDeleteConfirm = false;

          // Check if error is due to housing units
          if (err.error?.unitCount) {
            this.deleteError = `Cannot delete building. This building contains ${err.error.unitCount} housing unit(s). Delete all housing units first, or archive the building instead.`;
          } else {
            this.deleteError = err.error?.message || 'Failed to delete building';
          }

          console.error('Error deleting building:', err);
        }
      });
  }

  /**
   * Navigate back to buildings list.
   */
  goBack(): void {
    this.router.navigate(['/buildings']);
  }

  /**
   * Format date for display.
   */
  formatDate(date?: string): string {
    if (!date) {
      return 'N/A';
    }
    return new Date(date).toLocaleString();
  }
}
