import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { debounceTime, distinctUntilChanged, takeUntil } from "rxjs/operators";
import { BuildingService } from "../../../../core/services/building.service";
import { Building, Page } from "../../../../models/building.model";
import { SortIconPipe } from "../../../../shared/pipes/sort-icon.pipe";

@Component({
  selector: "app-building-list",
  standalone: true,
  imports: [CommonModule, FormsModule, SortIconPipe],
  templateUrl: "./building-list.component.html",
  styleUrls: ["./building-list.component.scss"],
})
export class BuildingListComponent implements OnInit, OnDestroy {
  buildings: Building[] = [];
  cities: string[] = [];

  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  selectedCity = "";
  searchTerm = "";
  sortField = "name";
  sortDirection: "asc" | "desc" = "asc";

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  loading = false;
  error: string | null = null;

  constructor(
    private buildingService: BuildingService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadCities();
    this.loadBuildings();

    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((searchTerm) => {
        this.searchTerm = searchTerm;
        this.currentPage = 0;
        this.loadBuildings();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBuildings(): void {
    this.loading = true;
    this.error = null;
    const sort = `${this.sortField},${this.sortDirection}`;
    this.buildingService
      .getAllBuildings(
        this.currentPage,
        this.pageSize,
        sort,
        this.selectedCity || undefined,
        this.searchTerm || undefined,
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: Page<Building>) => {
          this.buildings = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.currentPage = page.number;
          this.loading = false;
        },
        error: () => {
          this.error = "Failed to load buildings";
          this.loading = false;
        },
      });
  }

  loadCities(): void {
    this.buildingService
      .getAllCities()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cities) => {
          this.cities = cities;
        },
      });
  }

  onSearchChange(searchTerm: string): void {
    this.searchSubject.next(searchTerm);
  }
  onCityFilterChange(city: string): void {
    this.selectedCity = city;
    this.currentPage = 0;
    this.loadBuildings();
  }
  onSortChange(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortField = field;
      this.sortDirection = "asc";
    }
    this.loadBuildings();
  }
  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadBuildings();
    }
  }
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadBuildings();
    }
  }
  viewBuilding(building: Building): void {
    this.router.navigate(["/buildings", building.id]);
  }
  createBuilding(): void {
    this.router.navigate(["/buildings/new"]);
  }
  clearFilters(): void {
    this.selectedCity = "";
    this.searchTerm = "";
    this.currentPage = 0;
    this.loadBuildings();
  }
}
