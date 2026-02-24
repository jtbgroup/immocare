import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { HousingUnitService } from "../../../../core/services/housing-unit.service";
import { HousingUnit } from "../../../../models/housing-unit.model";
import { MeterSectionComponent } from "../../../../shared/components/meter-section/meter-section.component";
import { PebSectionComponent } from "../peb-section/peb-section.component";
import { RentSectionComponent } from "../rent-section/rent-section.component";
import { RoomSectionComponent } from "../room-section/room-section.component";

@Component({
  selector: "app-housing-unit-details",
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RoomSectionComponent,
    PebSectionComponent,
    RentSectionComponent,
    MeterSectionComponent, // UC008
  ],
  templateUrl: "./housing-unit-details.component.html",
  styleUrls: ["./housing-unit-details.component.scss"],
})
export class HousingUnitDetailsComponent implements OnInit, OnDestroy {
  unit?: HousingUnit;
  loading = false;
  showDeleteConfirm = false;
  deleting = false;
  deleteError = "";

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private housingUnitService: HousingUnitService,
  ) {}

  ngOnInit(): void {
    const id = +(this.route.snapshot.paramMap.get("id") ?? 0);
    if (id) {
      this.loading = true;
      this.housingUnitService
        .getUnitById(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (u) => {
            this.unit = u;
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          },
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  edit(): void {
    if (this.unit) {
      this.router.navigate(["/units", this.unit.id, "edit"]);
    }
  }

  confirmDelete(): void {
    this.showDeleteConfirm = true;
  }

  delete(): void {
    if (!this.unit) return;
    this.deleting = true;
    this.housingUnitService
      .delete(this.unit.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.router.navigate(["/buildings", this.unit!.buildingId]);
        },
        error: (err) => {
          this.deleting = false;
          this.deleteError = err.error?.message ?? "Failed to delete unit.";
        },
      });
  }
}
