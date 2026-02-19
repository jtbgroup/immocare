import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { BuildingService } from '../../../../core/services/building.service';
import { Building, Page } from '../../../../models/building.model';

/**
 * Component for displaying the list of buildings.
 * Implements US004 - View Buildings List and US005 - Search Buildings.
 */
@Component({
  selector: 'app-building-list',
  templateUrl: './building-list.component.html',
  styleUrls: ['./building-list.component.scss']
})
export class BuildingListComponent implements OnInit, OnDestroy {
  buildings: Building[] = [];
  cities: string[] = [];
  
  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  
  // Filters
  selectedCity = '';
  searchTerm = '';
  sortField = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Search subject for debouncing
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();
  
  loading = false;
  error: string | null = null;

  constructor(
    private buildingService: BuildingService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCities();
    this.loadBuildings();
    
    // Setup debounced search
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      this.loadBuildings();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load buildings with current filters.
   */
  loadBuildings(): void {
    this.loading = true;
    this.error = null;
    
    const sort = `${this.sortField},${this.sortDirection}`;
    
    this.buildingService.getAllBuildings(
      this.currentPage,
      this.pageSize,
      sort,
      this.selectedCity || undefined,
      this.searchTerm || undefined
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: Page<Building>) => {
          this.buildings = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.currentPage = page.number;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to load buildings';
          this.loading = false;
          console.error('Error loading buildings:', err);
        }
      });
  }

  /**
   * Load all cities for filter dropdown.
   */
  loadCities(): void {
    this.buildingService.getAllCities()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cities) => {
          this.cities = cities;
        },
        error: (err) => {
          console.error('Error loading cities:', err);
        }
      });
  }

  /**
   * Handle search input.
   */
  onSearchChange(searchTerm: string): void {
    this.searchSubject.next(searchTerm);
  }

  /**
   * Handle city filter change.
   */
  onCityFilterChange(city: string): void {
    this.selectedCity = city;
    this.currentPage = 0;
    this.loadBuildings();
  }

  /**
   * Handle sort change.
   */
  onSortChange(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.loadBuildings();
  }

  /**
   * Navigate to previous page.
   */
  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadBuildings();
    }
  }

  /**
   * Navigate to next page.
   */
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadBuildings();
    }
  }

  /**
   * Navigate to building details.
   */
  viewBuilding(building: Building): void {
    this.router.navigate(['/buildings', building.id]);
  }

  /**
   * Navigate to create building form.
   */
  createBuilding(): void {
    this.router.navigate(['/buildings/new']);
  }

  /**
   * Clear all filters.
   */
  clearFilters(): void {
    this.selectedCity = '';
    this.searchTerm = '';
    this.currentPage = 0;
    this.loadBuildings();
  }
}
