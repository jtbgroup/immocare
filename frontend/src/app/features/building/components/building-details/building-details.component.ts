import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ActiveEstateService } from '../../../../core/services/active-estate.service';
import { BuildingService } from '../../../../core/services/building.service';
import { HousingUnitService } from '../../../../core/services/housing-unit.service';
import { Building } from '../../../../models/building.model';
import { HousingUnit } from '../../../../models/housing-unit.model';

/**
 * Building detail page with its housing units.
 * UC016 Phase 2: all navigation includes estateId.
 */
@Component({
  selector: 'app-building-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './building-details.component.html',
  styleUrls: ['./building-details.component.scss'],
})
export class BuildingDetailsComponent implements OnInit {
  building?: Building;
  units: HousingUnit[] = [];
  loading = false;
  errorMessage = '';
  deleteError = '';
  deleting = false;

  constructor(
    private buildingService: BuildingService,
    private housingUnitService: HousingUnitService,
    private route: ActivatedRoute,
    private router: Router,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading = true;
    this.buildingService.getBuildingById(id).subscribe({
      next: (b) => {
        this.building = b;
        this.loading = false;
        this.loadUnits(b.id);
      },
      error: () => {
        this.errorMessage = 'Building not found.';
        this.loading = false;
      },
    });
  }

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  private loadUnits(buildingId: number): void {
    this.housingUnitService.getUnitsByBuilding(buildingId).subscribe({
      next: (units) => (this.units = units),
      error: () => { /* non-blocking */ },
    });
  }

  edit(): void {
    this.router.navigate(['/estates', this.estateId, 'buildings', this.building!.id, 'edit']);
  }

  addUnit(): void {
    this.router.navigate(['/estates', this.estateId, 'units', 'new'], {
      queryParams: { buildingId: this.building!.id },
    });
  }

  navigateToUnit(unit: HousingUnit): void {
    this.router.navigate(['/estates', this.estateId, 'units', unit.id]);
  }

  back(): void {
    this.router.navigate(['/estates', this.estateId, 'buildings']);
  }

  confirmDelete(): void {
    if (!this.building) return;
    if (!confirm(`Delete building "${this.building.name}"? This cannot be undone.`)) return;
    this.deleting = true;
    this.buildingService.deleteBuilding(this.building.id).subscribe({
      next: () => this.router.navigate(['/estates', this.estateId, 'buildings']),
      error: (err) => {
        this.deleting = false;
        this.deleteError = err.error?.message ?? 'Failed to delete building.';
      },
    });
  }
}
