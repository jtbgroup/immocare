import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

import { HousingUnitService } from "../../../../core/services/housing-unit.service";
import { HousingUnit } from "../../../../models/housing-unit.model";
import { PEB_SCORE_DISPLAY } from "../../../../models/peb-score.model";

@Component({
  selector: "app-unit-global-list",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./unit-global-list.component.html",
  styleUrls: ["./unit-global-list.component.scss"],
})
export class UnitGlobalListComponent implements OnInit, OnDestroy {
  allUnits: HousingUnit[] = [];
  filteredUnits: HousingUnit[] = [];

  loading = false;
  error: string | null = null;

  searchTerm = "";

  readonly pebDisplay = PEB_SCORE_DISPLAY;

  private destroy$ = new Subject<void>();

  constructor(
    private housingUnitService: HousingUnitService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.housingUnitService
      .getAllUnits()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (units) => {
          this.allUnits = units;
          this.applyFilter();
          this.loading = false;
        },
        error: () => {
          this.error = "Failed to load housing units.";
          this.loading = false;
        },
      });
  }

  onSearchChange(): void {
    this.applyFilter();
  }

  clearSearch(): void {
    this.searchTerm = "";
    this.applyFilter();
  }

  private applyFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredUnits = [...this.allUnits];
      return;
    }
    this.filteredUnits = this.allUnits.filter((u) =>
      (u.buildingName ?? "").toLowerCase().includes(term),
    );
  }

  viewUnit(id: number): void {
    this.router.navigate(["/units", id]);
  }

  formatRent(value: number | null | undefined): string {
    if (value == null) return "—";
    return (
      new Intl.NumberFormat("fr-BE", {
        style: "currency",
        currency: "EUR",
        minimumFractionDigits: 0,
      }).format(value) + "/mo"
    );
  }
}
