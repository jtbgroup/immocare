import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { BuildingService } from "../../../../core/services/building.service";
import { Building } from "../../../../models/building.model";

/**
 * Displays the paginated list of buildings scoped to the active estate.
 * UC004_ESTATE_PLACEHOLDER Phase 2: navigation includes estateId in all routerLink / router.navigate calls.
 */
@Component({
  selector: "app-building-list",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./building-list.component.html",
  styleUrls: ["./building-list.component.scss"],
})
export class BuildingListComponent implements OnInit {
  buildings: Building[] = [];
  cities: string[] = [];
  loading = false;
  errorMessage = "";
  deleteError = "";

  // Filters
  searchTerm = "";
  selectedCity = "";

  // Pagination
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  readonly pageSize = 20;

  // Sort
  sortField = "name";
  sortDir: "asc" | "desc" = "asc";

  constructor(
    private buildingService: BuildingService,
    private router: Router,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    this.loadCities();
    this.load();
  }

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  load(): void {
    this.loading = true;
    this.errorMessage = "";
    const sort = `${this.sortField},${this.sortDir}`;
    this.buildingService
      .getAllBuildings(
        this.currentPage,
        this.pageSize,
        sort,
        this.selectedCity || undefined,
        this.searchTerm.trim() || undefined,
      )
      .subscribe({
        next: (page) => {
          this.buildings = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.loading = false;
        },
        error: () => {
          this.errorMessage = "Failed to load buildings.";
          this.loading = false;
        },
      });
  }

  private loadCities(): void {
    this.buildingService.getAllCities().subscribe({
      next: (cities) => (this.cities = cities),
      error: () => {
        /* non-blocking */
      },
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.load();
  }

  onCityFilter(): void {
    this.currentPage = 0;
    this.load();
  }

  clearFilters(): void {
    this.searchTerm = "";
    this.selectedCity = "";
    this.currentPage = 0;
    this.load();
  }

  sort(field: string): void {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === "asc" ? "desc" : "asc";
    } else {
      this.sortField = field;
      this.sortDir = "asc";
    }
    this.currentPage = 0;
    this.load();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.load();
    }
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  navigateTo(building: Building): void {
    this.router.navigate(["/estates", this.estateId, "buildings", building.id]);
  }

  create(): void {
    this.router.navigate(["/estates", this.estateId, "buildings", "new"]);
  }

  confirmDelete(building: Building, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Delete building "${building.name}"? This cannot be undone.`))
      return;
    this.deleteError = "";
    this.buildingService.deleteBuilding(building.id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.deleteError = err.error?.message ?? "Failed to delete building.";
      },
    });
  }
}
