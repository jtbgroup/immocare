import { CommonModule } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Subject, of } from "rxjs";
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  switchMap,
} from "rxjs/operators";

import {
  AssetSearchResult,
  AssetSearchService,
} from "../../../../../core/services/asset-search.service";
import {
  AssetType,
  TransactionAssetLink,
} from "../../../../../models/transaction.model";

// ─── SaveAssetLinkRequest (sent to backend — no resolved fields) ──────────────

export interface SaveAssetLinkRequest {
  assetType: AssetType;
  assetId: number;
  amount?: number;
  notes?: string;
}

// ─── Internal row model (richer than SaveAssetLinkRequest) ────────────────────

interface AssetLinkRow {
  /** Filled once a device is selected */
  assetId: number | null;
  assetType: AssetType;

  /** Display-only, resolved from search result */
  assetLabel: string;
  unitNumber: string | null;
  buildingName: string | null;

  /** Optional partial amount */
  amount: number | null;
  notes: string;

  /** Search state for this row */
  searchQuery: string;
  searchResults: AssetSearchResult[];
  searching: boolean;
  dropdownOpen: boolean;
}

@Component({
  selector: "app-asset-link-editor",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./asset-link-editor.component.html",
  styleUrls: ["./asset-link-editor.component.scss"],
})
export class AssetLinkEditorComponent implements OnChanges, OnDestroy {
  // ── Inputs ──────────────────────────────────────────────────────────────────

  /** Optional — filters device search by building */
  @Input() buildingId: number | null = null;

  /** Optional — filters device search by unit (currently unused at API level but kept for future) */
  @Input() unitId: number | null = null;

  /** Current links (for edit mode pre-population) */
  @Input() links: TransactionAssetLink[] = [];

  /** Total transaction amount — used for ventilation summary */
  @Input() transactionAmount: number | null = null;

  // ── Outputs ─────────────────────────────────────────────────────────────────

  /** Emits the list of SaveAssetLinkRequests each time the user changes anything */
  @Output() linksChanged = new EventEmitter<SaveAssetLinkRequest[]>();

  /**
   * Emits a subcategoryId when an asset type is selected and a platform config
   * mapping exists for it. Parent form can use this to pre-fill the subcategory.
   */
  @Output() subcategoryPreFill = new EventEmitter<number>();

  // ── Internal state ───────────────────────────────────────────────────────────

  rows: AssetLinkRow[] = [];

  readonly assetTypes: AssetType[] = ["BOILER", "FIRE_EXTINGUISHER", "METER"];

  readonly assetTypeLabels: Record<AssetType, string> = {
    BOILER: "🔥 Boiler",
    FIRE_EXTINGUISHER: "🧯 Fire Extinguisher",
    METER: "⚡ Meter",
  };

  /** One search Subject per row index — rebuilt when rows are added/removed */
  private searchSubjects: Map<number, Subject<{ q: string; rowIdx: number }>> =
    new Map();

  private destroy$ = new Subject<void>();

  constructor(private assetSearchService: AssetSearchService) {}

  // ── Lifecycle ────────────────────────────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["links"] && this.links?.length) {
      this.rows = this.links.map((link) => this.linkToRow(link));
      this.rows.forEach((_, idx) => this.ensureSearchSubject(idx));
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubjects.forEach((s) => s.complete());
  }

  // ── Public actions ───────────────────────────────────────────────────────────

  addRow(): void {
    const idx = this.rows.length;
    this.rows = [...this.rows, this.emptyRow()];
    this.ensureSearchSubject(idx);
  }

  removeRow(idx: number): void {
    this.searchSubjects.get(idx)?.complete();
    this.searchSubjects.delete(idx);
    this.rows = this.rows.filter((_, i) => i !== idx);
    // Re-index subjects
    const oldMap = new Map(this.searchSubjects);
    this.searchSubjects.clear();
    oldMap.forEach((subj, oldIdx) => {
      const newIdx = oldIdx > idx ? oldIdx - 1 : oldIdx;
      this.searchSubjects.set(newIdx, subj);
    });
    this.emit();
  }

  onTypeChange(row: AssetLinkRow): void {
    // Reset device selection when type changes
    row.assetId = null;
    row.assetLabel = "";
    row.unitNumber = null;
    row.buildingName = null;
    row.searchQuery = "";
    row.searchResults = [];
    row.dropdownOpen = false;
    this.emit();
  }

  onSearchInput(row: AssetLinkRow, rowIdx: number, query: string): void {
    row.searchQuery = query;
    row.dropdownOpen = false;

    if (query.trim().length < 2) {
      row.searchResults = [];
      return;
    }

    const subj = this.searchSubjects.get(rowIdx);
    subj?.next({ q: query.trim(), rowIdx });
  }

  selectDevice(row: AssetLinkRow, result: AssetSearchResult): void {
    row.assetId = result.id;
    row.assetLabel = result.label;
    row.unitNumber = result.unitNumber;
    row.buildingName = result.buildingName;
    row.searchQuery = result.label;
    row.dropdownOpen = false;
    row.searchResults = [];
    this.emit();
  }

  clearDevice(row: AssetLinkRow): void {
    row.assetId = null;
    row.assetLabel = "";
    row.unitNumber = null;
    row.buildingName = null;
    row.searchQuery = "";
    row.searchResults = [];
    row.dropdownOpen = false;
    this.emit();
  }

  onAmountChange(): void {
    this.emit();
  }

  onNotesChange(): void {
    this.emit();
  }

  closeDropdown(row: AssetLinkRow): void {
    // Small delay so click on result registers before close
    setTimeout(() => {
      row.dropdownOpen = false;
    }, 150);
  }

  // ── Ventilation summary ──────────────────────────────────────────────────────

  get ventilatedTotal(): number {
    return this.rows
      .filter((r) => r.amount != null && r.amount > 0)
      .reduce((acc, r) => acc + (r.amount ?? 0), 0);
  }

  get ventilationValid(): boolean {
    if (this.transactionAmount == null || this.transactionAmount <= 0)
      return true;
    return this.ventilatedTotal <= this.transactionAmount;
  }

  get hasPartialAmounts(): boolean {
    return this.rows.some((r) => r.amount != null && r.amount > 0);
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private emptyRow(): AssetLinkRow {
    return {
      assetId: null,
      assetType: "BOILER",
      assetLabel: "",
      unitNumber: null,
      buildingName: null,
      amount: null,
      notes: "",
      searchQuery: "",
      searchResults: [],
      searching: false,
      dropdownOpen: false,
    };
  }

  private linkToRow(link: TransactionAssetLink): AssetLinkRow {
    return {
      assetId: link.assetId ?? null,
      assetType: link.assetType,
      assetLabel: link.assetLabel ?? "",
      unitNumber: (link as any).unitNumber ?? null,
      buildingName: (link as any).buildingName ?? null,
      amount: (link as any).amount ?? null,
      notes: (link as any).notes ?? "",
      searchQuery: link.assetLabel ?? "",
      searchResults: [],
      searching: false,
      dropdownOpen: false,
    };
  }

  private ensureSearchSubject(rowIdx: number): void {
    if (this.searchSubjects.has(rowIdx)) return;

    const subj = new Subject<{ q: string; rowIdx: number }>();
    this.searchSubjects.set(rowIdx, subj);

    subj
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => a.q === b.q && a.rowIdx === b.rowIdx),
        switchMap(({ q, rowIdx: idx }) => {
          const row = this.rows[idx];
          if (!row) return of([]);
          row.searching = true;
          return this.searchForRow(row.assetType, q).pipe(
            catchError(() => of([])),
          );
        }),
      )
      .subscribe((results) => {
        // Find row by current search query (robust to reordering)
        const idx = this.findRowIndexBySearch(results);
        const row = this.rows[idx] ?? this.rows[this.rows.length - 1];
        if (row) {
          row.searchResults = results;
          row.searching = false;
          row.dropdownOpen = results.length > 0;
        }
      });
  }

  private searchForRow(type: AssetType, q: string) {
    const bid = this.buildingId ?? undefined;
    switch (type) {
      case "BOILER":
        return this.assetSearchService.searchBoilers(q, bid);
      case "FIRE_EXTINGUISHER":
        return this.assetSearchService.searchFireExtinguishers(q, bid);
      case "METER":
        return this.assetSearchService.searchMeters(q, bid);
    }
  }

  /** Best-effort: find the row that is currently searching */
  private findRowIndexBySearch(_results: AssetSearchResult[]): number {
    const idx = this.rows.findIndex((r) => r.searching);
    return idx >= 0 ? idx : this.rows.length - 1;
  }

  private emit(): void {
    const requests: SaveAssetLinkRequest[] = this.rows
      .filter((r) => r.assetId != null)
      .map((r) => ({
        assetType: r.assetType,
        assetId: r.assetId!,
        ...(r.amount != null && r.amount > 0 ? { amount: r.amount } : {}),
        ...(r.notes?.trim() ? { notes: r.notes.trim() } : {}),
      }));
    this.linksChanged.emit(requests);
  }
}
