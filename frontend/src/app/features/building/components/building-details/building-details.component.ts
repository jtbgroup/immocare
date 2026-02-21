import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BuildingService } from '../../../../core/services/building.service';
import { Building } from '../../../../models/building.model';
import { HousingUnitListComponent } from '../../../housing-unit/components/housing-unit-list/housing-unit-list.component';

@Component({
  selector: 'app-building-details',
  standalone: true,
  imports: [CommonModule, RouterLink, HousingUnitListComponent],
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

  constructor(private buildingService: BuildingService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = +params['id'];
      if (id) { this.loadBuilding(id); }
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  private loadBuilding(id: number): void {
    this.loading = true;
    this.buildingService.getBuildingById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (building) => { this.building = building; this.loading = false; },
      error: () => { this.error = 'Building not found'; this.loading = false; }
    });
  }

  editBuilding(): void { if (this.building) { this.router.navigate(['/buildings', this.building.id, 'edit']); } }
  confirmDelete(): void { this.showDeleteConfirm = true; this.deleteError = null; }
  cancelDelete(): void { this.showDeleteConfirm = false; this.deleteError = null; }

  deleteBuilding(): void {
    if (!this.building) return;
    this.loading = true;
    this.buildingService.deleteBuilding(this.building.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.router.navigate(['/buildings']); },
      error: (err) => {
        this.loading = false;
        this.showDeleteConfirm = false;
        if (err.error?.unitCount) { this.deleteError = `Cannot delete building. It contains ${err.error.unitCount} housing unit(s).`; }
        else { this.deleteError = 'Failed to delete building. Please try again.'; }
      }
    });
  }

  goBack(): void { this.router.navigate(['/buildings']); }

  formatDate(date: string | undefined): string {
    return date ? new Date(date).toLocaleDateString() : 'N/A';
  }
}
