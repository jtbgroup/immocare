import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ActiveEstateService } from '../../../../core/services/active-estate.service';
import { HousingUnitService } from '../../../../core/services/housing-unit.service';
import { HousingUnit } from '../../../../models/housing-unit.model';

/**
 * Housing unit detail page.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all navigation includes estateId.
 */
@Component({
  selector: 'app-housing-unit-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './housing-unit-details.component.html',
  styleUrls: ['./housing-unit-details.component.scss'],
})
export class HousingUnitDetailsComponent implements OnInit {
  unit?: HousingUnit;
  loading = false;
  errorMessage = '';
  deleteError = '';
  deleting = false;

  constructor(
    private housingUnitService: HousingUnitService,
    private route: ActivatedRoute,
    private router: Router,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading = true;
    this.housingUnitService.getUnitById(id).subscribe({
      next: (u) => {
        this.unit = u;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unit not found.';
        this.loading = false;
      },
    });
  }

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  edit(): void {
    this.router.navigate(['/estates', this.estateId, 'units', this.unit!.id, 'edit']);
  }

  backToBuilding(): void {
    this.router.navigate(['/estates', this.estateId, 'buildings', this.unit!.buildingId]);
  }

  back(): void {
    this.router.navigate(['/estates', this.estateId, 'units']);
  }

  confirmDelete(): void {
    if (!this.unit) return;
    if (!confirm(`Delete unit "${this.unit.unitNumber}"? This cannot be undone.`)) return;
    this.deleting = true;
    this.housingUnitService.delete(this.unit.id).subscribe({
      next: () => this.router.navigate(['/estates', this.estateId, 'buildings', this.unit!.buildingId]),
      error: (err) => {
        this.deleting = false;
        this.deleteError = err.error?.message ?? 'Failed to delete unit.';
      },
    });
  }
}
