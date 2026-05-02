import { LowerCasePipe } from "@angular/common";
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { HousingUnitService } from "../../../../../core/services/housing-unit.service";
import { HousingUnit } from "../../../../../models/housing-unit.model";
import { PEB_SCORE_DISPLAY } from "../../../../../models/peb-score.model";

@Component({
  selector: "app-housing-unit-list",
  standalone: true,
  imports: [LowerCasePipe],
  templateUrl: "./housing-unit-list.component.html",
  styleUrls: ["./housing-unit-list.component.scss"],
})
export class HousingUnitListComponent implements OnInit, OnDestroy {
  @Input() buildingId!: number;

  units: HousingUnit[] = [];
  loading = false;
  readonly pebDisplay = PEB_SCORE_DISPLAY;

  private destroy$ = new Subject<void>();

  constructor(
    private housingUnitService: HousingUnitService,
    private router: Router,
  ) {}

  ngOnInit(): void {
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
