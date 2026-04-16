import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { BuildingService } from "../../../../core/services/building.service";
import { HousingUnitService } from "../../../../core/services/housing-unit.service";
import { Building } from "../../../../models/building.model";
import { HousingUnit } from "../../../../models/housing-unit.model";

/**
 * Cross-building unit list scoped to the active estate.
 * UC016 Phase 2: loads units per building within the estate.
 */
@Component({
  selector: "app-unit-global-list",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./unit-global-list.component.html",
  styleUrls: ["./unit-global-list.component.scss"],
})
export class UnitGlobalListComponent implements OnInit {
  units: HousingUnit[] = [];
  buildings: Building[] = [];
  loading = false;
  errorMessage = "";

  // Filters
  selectedBuildingId: number | null = null;
  searchTerm = "";

  constructor(
    private housingUnitService: HousingUnitService,
    private buildingService: BuildingService,
    private router: Router,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    this.loadBuildings();
  }

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  private loadBuildings(): void {
    this.loading = true;
    this.buildingService.getAllBuildings(0, 200, "name,asc").subscribe({
      next: (page) => {
        this.buildings = page.content;
        this.loadAllUnits();
      },
      error: () => {
        this.errorMessage = "Failed to load buildings.";
        this.loading = false;
      },
    });
  }

  private loadAllUnits(): void {
    if (this.buildings.length === 0) {
      this.units = [];
      this.loading = false;
      return;
    }

    // If a building filter is active, load only that building's units
    if (this.selectedBuildingId) {
      this.housingUnitService
        .getUnitsByBuilding(this.selectedBuildingId)
        .subscribe({
          next: (units) => {
            this.units = units;
            this.loading = false;
          },
          error: () => {
            this.errorMessage = "Failed to load units.";
            this.loading = false;
          },
        });
      return;
    }

    // Load units for all buildings and merge
    const allUnits: HousingUnit[] = [];
    let pending = this.buildings.length;

    this.buildings.forEach((b) => {
      this.housingUnitService.getUnitsByBuilding(b.id).subscribe({
        next: (units) => {
          allUnits.push(...units);
          pending--;
          if (pending === 0) {
            this.units = allUnits.sort(
              (a, b) =>
                (a.buildingName ?? "").localeCompare(b.buildingName ?? "") ||
                a.floor - b.floor ||
                a.unitNumber.localeCompare(b.unitNumber),
            );
            this.loading = false;
          }
        },
        error: () => {
          pending--;
          if (pending === 0) this.loading = false;
        },
      });
    });
  }

  onBuildingFilter(): void {
    this.loading = true;
    this.loadAllUnits();
  }

  clearFilters(): void {
    this.selectedBuildingId = null;
    this.searchTerm = "";
    this.loading = true;
    this.loadAllUnits();
  }

  get filteredUnits(): HousingUnit[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) return this.units;
    return this.units.filter(
      (u) =>
        u.unitNumber.toLowerCase().includes(term) ||
        (u.buildingName ?? "").toLowerCase().includes(term),
    );
  }

  navigateTo(unit: HousingUnit): void {
    this.router.navigate(["/estates", this.estateId, "units", unit.id]);
  }

  create(): void {
    this.router.navigate(["/estates", this.estateId, "units", "new"]);
  }
}
