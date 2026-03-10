import { CommonModule } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  inject,
} from "@angular/core";
import { FormsModule } from "@angular/forms";

import { BuildingService } from "../../../../core/services/building.service";
import { LeaseService } from "../../../../core/services/lease.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import { Building } from "../../../../models/building.model";
import {
  LeaseGlobalFilters,
  LeaseGlobalSummary,
} from "../../../../models/lease.model";
import {
  ImportPreviewRow,
  TagSubcategory,
  TransactionDirection,
} from "../../../../models/transaction.model";

@Component({
  selector: "app-import-row-detail-panel",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./import-row-detail-panel.component.html",
  styleUrls: ["./import-row-detail-panel.component.scss"],
})
export class ImportRowDetailPanelComponent implements OnInit, OnChanges {
  @Input() row!: ImportPreviewRow;
  @Output() close = new EventEmitter<void>();
  @Output() rowChanged = new EventEmitter<ImportPreviewRow>();

  private tagSubcategoryService = inject(TagSubcategoryService);
  private buildingService = inject(BuildingService);
  private leaseService = inject(LeaseService);

  allSubcategories: TagSubcategory[] = [];
  groupedSubcategories: { categoryName: string; subcategories: TagSubcategory[] }[] = [];
  selectedSubcategoryId: number | null = null;

  buildings: Building[] = [];
  allLeases: LeaseGlobalSummary[] = [];
  filteredLeases: LeaseGlobalSummary[] = [];
  filterBuildingId: number | null = null;
  selectedLeaseId: number | null = null;

  ngOnInit(): void {
    this.loadSubcategories();
    this.loadBuildings();
    this.loadLeases();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["row"] && this.row) {
      this.syncFromRow();
    }
  }

  // ── User actions ────────────────────────────────────────────────────────────

  setDirection(dir: TransactionDirection): void {
    this.row.direction = dir;
    this.groupedSubcategories = this.groupBy(
      this.allSubcategories.filter(
        (s) => s.direction === dir || s.direction === "BOTH",
      ),
    );
    this.emit();
  }

  acceptSubcategorySuggestion(): void {
    if (!this.row.suggestedSubcategory) return;
    this.selectedSubcategoryId = this.row.suggestedSubcategory.subcategoryId;
    this.onSubcategoryChange(this.selectedSubcategoryId);
  }

  onSubcategoryChange(id: number | null): void {
    this.row.enrichedSubcategoryId = id ?? undefined;
    const sub = this.allSubcategories.find((s) => s.id === id);
    this.row.enrichedSubcategoryName = sub
      ? `${sub.categoryName} / ${sub.name}`
      : undefined;
    this.emit();
  }

  acceptLeaseSuggestion(): void {
    if (!this.row.suggestedLease) return;
    const suggested = this.row.suggestedLease;
    this.filterBuildingId = suggested.buildingId ?? null;
    this.applyBuildingFilter();
    this.selectedLeaseId = suggested.leaseId;
    this.onLeaseChange(suggested.leaseId);
  }

  onBuildingFilterChange(buildingId: number | null): void {
    this.filterBuildingId = buildingId;
    this.selectedLeaseId = null;
    this.onLeaseChange(null);
    this.applyBuildingFilter();
  }

  applyBuildingFilter(): void {
    this.filteredLeases = this.filterBuildingId
      ? this.allLeases.filter((l) => l.buildingId === this.filterBuildingId)
      : this.allLeases;
  }

  onLeaseChange(leaseId: number | null): void {
    const lease = this.allLeases.find((l) => l.id === leaseId);
    this.row.enrichedLeaseId = leaseId ?? undefined;
    this.row.enrichedUnitId = lease?.housingUnitId;
    this.row.enrichedUnitNumber = lease?.housingUnitNumber;
    this.row.enrichedBuildingId = lease?.buildingId;
    this.row.enrichedBuildingName = lease?.buildingName;
    this.emit();
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  private syncFromRow(): void {
    this.selectedSubcategoryId = this.row.enrichedSubcategoryId ?? null;
    this.selectedLeaseId = this.row.enrichedLeaseId ?? null;

    const suggestedLeaseId = this.row.suggestedLease?.leaseId ?? null;
    const targetLeaseId = this.selectedLeaseId ?? suggestedLeaseId;

    if (targetLeaseId && this.allLeases.length) {
      const lease = this.allLeases.find((l) => l.id === targetLeaseId);
      if (lease) {
        this.filterBuildingId = lease.buildingId;
        this.applyBuildingFilter();
        this.selectedLeaseId = targetLeaseId;
      }
    } else if (this.row.suggestedLease?.buildingId) {
      this.filterBuildingId = this.row.suggestedLease.buildingId;
    }
  }

  private loadSubcategories(): void {
    this.tagSubcategoryService.getAll().subscribe((subs) => {
      this.allSubcategories = subs;
      this.groupedSubcategories = this.groupBy(subs);
    });
  }

  private loadBuildings(): void {
    this.buildingService
      .getAllBuildings(0, 200, "name,asc")
      .subscribe((page) => {
        this.buildings = page.content ?? (page as any);
      });
  }

  private loadLeases(): void {
    const filters: LeaseGlobalFilters = {
      statuses: ["DRAFT", "ACTIVE", "FINISHED", "CANCELLED"],
      leaseType: "",
      startDateFrom: "",
      startDateTo: "",
      endDateFrom: "",
      endDateTo: "",
      rentMin: null,
      rentMax: null,
    };
    this.leaseService.getAll(filters, 0, 500).subscribe((page) => {
      this.allLeases = page.content;
      this.filteredLeases = page.content;
      if (this.row) this.syncFromRow();
    });
  }

  private emit(): void {
    this.rowChanged.emit({ ...this.row });
  }

  private groupBy(
    subs: TagSubcategory[],
  ): { categoryName: string; subcategories: TagSubcategory[] }[] {
    const map = new Map<string, TagSubcategory[]>();
    subs.forEach((s) => {
      const list = map.get(s.categoryName) ?? [];
      list.push(s);
      map.set(s.categoryName, list);
    });
    return Array.from(map.entries()).map(([categoryName, subcategories]) => ({
      categoryName,
      subcategories,
    }));
  }
}
