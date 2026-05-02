// features/transaction/components/transaction-import/transaction-import.component.ts — UC004_ESTATE_PLACEHOLDER Phase 6
// activeEstateService.canEdit() checked before allowing upload/import actions.
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { BankAccountService } from "../../../../core/services/bank-account.service";
import { ImportParserService } from "../../../../core/services/import-parser.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import {
  BankAccount,
  ImportBatchResult,
  ImportParser,
  ImportPreviewRow,
  ImportRowEnrichment,
} from "../../../../models/transaction.model";
import { ImportRowDetailPanelComponent } from "../_partials/import-row-detail-panel/import-row-detail-panel.component";

type Step = "form" | "preview" | "result";

@Component({
  selector: "app-transaction-import",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    ImportRowDetailPanelComponent,
  ],
  templateUrl: "./transaction-import.component.html",
  styleUrls: ["./transaction-import.component.scss"],
})
export class TransactionImportComponent implements OnInit {
  parsers: ImportParser[] = [];
  bankAccounts: BankAccount[] = [];

  selectedParserCode = "";
  selectedBankAccountId: number | null = null;
  selectedFile: File | null = null;
  isDragging = false;

  step: Step = "form";
  loading = false;
  error: string | null = null;

  previewRows: ImportPreviewRow[] = [];
  selectedRow: ImportPreviewRow | null = null;

  result: ImportBatchResult | null = null;

  // ── Sort state ───────────────────────────────────────────────────────────────
  sortField = "";
  sortDirection: "asc" | "desc" = "asc";

  constructor(
    private transactionService: TransactionService,
    private importParserService: ImportParserService,
    private bankAccountService: BankAccountService,
    private router: Router,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    this.importParserService.getAll().subscribe((p) => {
      this.parsers = p;
      if (p.length === 1) this.selectedParserCode = p[0].code;
    });
    this.bankAccountService.getAll().subscribe((accounts) => {
      this.bankAccounts = accounts.filter((a) => a.isActive);
      if (this.bankAccounts.length === 1) {
        this.selectedBankAccountId = this.bankAccounts[0].id;
      }
    });
  }

  get selectedParser(): ImportParser | undefined {
    return this.parsers.find((p) => p.code === this.selectedParserCode);
  }

  get acceptedFileTypes(): string {
    if (!this.selectedParser) return ".csv,.pdf";
    return this.selectedParser.format === "PDF" ? ".pdf" : ".csv";
  }

  get canPreview(): boolean {
    return (
      !!this.selectedFile &&
      !!this.selectedParserCode &&
      this.activeEstateService.canEdit()
    );
  }

  get selectedRows(): ImportPreviewRow[] {
    return this.previewRows.filter((r) => r.selected);
  }

  get validRows(): ImportPreviewRow[] {
    return this.previewRows.filter((r) => !r.parseError);
  }

  get duplicateCount(): number {
    return this.previewRows.filter((r) => r.duplicateInDb).length;
  }

  get enrichedCount(): number {
    return this.previewRows.filter(
      (r) => r.enrichedSubcategoryId || r.enrichedLeaseId,
    ).length;
  }

  get allSelected(): boolean {
    const selectable = this.validRows.filter((r) => !r.duplicateInDb);
    return selectable.length > 0 && selectable.every((r) => r.selected);
  }

  // ── Sort ─────────────────────────────────────────────────────────────────────

  sortBy(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortField = field;
      this.sortDirection = "asc";
    }
    this.previewRows = [...this.previewRows].sort((a, b) => {
      const valA = this.getSortValue(a, field);
      const valB = this.getSortValue(b, field);
      const cmp = valA < valB ? -1 : valA > valB ? 1 : 0;
      return this.sortDirection === "asc" ? cmp : -cmp;
    });
  }

  private getSortValue(
    row: ImportPreviewRow,
    field: string,
  ): string | number | boolean {
    switch (field) {
      case "transactionDate":
        return row.transactionDate ?? "";
      case "amount":
        return row.amount ?? 0;
      case "direction":
        return row.direction ?? "";
      case "counterpartyAccount":
        return (row.counterpartyAccount ?? "").toLowerCase();
      case "subcategory":
        // tri sur la valeur affichée : enriched > suggested > vide
        if (row.enrichedSubcategoryName)
          return row.enrichedSubcategoryName.toLowerCase();
        if (row.suggestedSubcategory) {
          return `${row.suggestedSubcategory.categoryName} / ${row.suggestedSubcategory.subcategoryName}`.toLowerCase();
        }
        return "";
      case "lease":
        // tri sur la valeur affichée : enriched > suggested > vide
        if (row.enrichedUnitNumber) {
          return `${row.enrichedUnitNumber} ${row.enrichedBuildingName ?? ""}`.toLowerCase();
        }
        if (row.suggestedLease) {
          return `${row.suggestedLease.unitNumber} ${row.suggestedLease.buildingName}`.toLowerCase();
        }
        return "";
      case "duplicateInDb":
        // ordre: Error > Dup > Override > New
        if (row.parseError) return 3;
        if (row.duplicateInDb && !row.selected) return 2;
        if (row.duplicateInDb && row.selected) return 1;
        return 0;
      default:
        return (row as any)[field] ?? "";
    }
  }

  sortIcon(field: string): string {
    if (this.sortField !== field) return "↕";
    return this.sortDirection === "asc" ? "↑" : "↓";
  }

  // ── Drag & drop ───────────────────────────────────────────────────────────────

  onDragOver(e: DragEvent): void {
    if (!this.activeEstateService.canEdit()) return;
    e.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(): void {
    this.isDragging = false;
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    this.isDragging = false;
    if (!this.activeEstateService.canEdit()) return;
    const file = e.dataTransfer?.files?.[0];
    if (file) this.selectFile(file);
  }

  onFileChange(e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) this.selectFile(file);
  }

  selectFile(file: File): void {
    this.error = null;
    if (this.selectedParser) {
      const ext = file.name.split(".").pop()?.toLowerCase();
      const expected = this.selectedParser.format.toLowerCase();
      if (ext !== expected) {
        this.error = `This parser expects a ${this.selectedParser.format} file. Got: .${ext}`;
        return;
      }
    }
    this.selectedFile = file;
  }

  clearFile(): void {
    this.selectedFile = null;
    this.previewRows = [];
    this.selectedRow = null;
    this.result = null;
    this.error = null;
    this.step = "form";
    this.sortField = "";
    this.sortDirection = "asc";
  }

  preview(): void {
    if (!this.canPreview) return;
    this.loading = true;
    this.error = null;
    this.selectedRow = null;
    this.sortField = "";
    this.sortDirection = "asc";

    this.transactionService
      .previewFile(this.selectedFile!, this.selectedParserCode)
      .subscribe({
        next: (rows) => {
          this.previewRows = rows.map((r) => ({
            ...r,
            selected: !r.duplicateInDb && !r.parseError,
          }));
          this.step = "preview";
          this.loading = false;
        },
        error: (err) => {
          this.error =
            err?.error?.errors?.[0]?.errorMessage ||
            err?.error?.message ||
            "Preview failed";
          this.loading = false;
        },
      });
  }

  openPanel(row: ImportPreviewRow): void {
    this.selectedRow = row;
  }

  closePanel(): void {
    this.selectedRow = null;
  }

  onRowChanged(updated: ImportPreviewRow): void {
    const idx = this.previewRows.findIndex(
      (r) => r.rowNumber === updated.rowNumber,
    );
    if (idx >= 0) {
      this.previewRows[idx] = updated;
      this.selectedRow = updated;
    }
  }

  toggleAll(checked: boolean): void {
    if (!this.activeEstateService.canEdit()) return;
    this.validRows
      .filter((r) => !r.duplicateInDb)
      .forEach((r) => (r.selected = checked));
  }

  import(): void {
    if (!this.activeEstateService.canEdit() || this.selectedRows.length === 0)
      return;
    this.loading = true;
    this.error = null;

    const enrichments: ImportRowEnrichment[] = this.selectedRows
      .filter(
        (r) =>
          r.fingerprint &&
          (r.enrichedSubcategoryId ||
            r.enrichedLeaseId ||
            r.enrichedUnitId ||
            r.direction),
      )
      .map((r) => ({
        fingerprint: r.fingerprint!,
        subcategoryId: r.enrichedSubcategoryId,
        leaseId: r.enrichedLeaseId,
        housingUnitId: r.enrichedUnitId,
        buildingId: r.enrichedBuildingId,
        directionOverride: r.direction ?? undefined,
      }));

    const selectedFingerprints: string[] = this.selectedRows
      .filter((r) => r.fingerprint)
      .map((r) => r.fingerprint!);

    this.transactionService
      .importFile(
        this.selectedFile!,
        this.selectedParserCode,
        this.selectedBankAccountId,
        enrichments,
        selectedFingerprints,
      )
      .subscribe({
        next: (r) => {
          this.result = r;
          this.step = "result";
          this.loading = false;
        },
        error: (err) => {
          this.error =
            err?.error?.errors?.[0]?.errorMessage ||
            err?.error?.message ||
            "Import failed";
          this.loading = false;
        },
      });
  }

  backToForm(): void {
    this.step = "form";
    this.previewRows = [];
    this.selectedRow = null;
    this.error = null;
    this.sortField = "";
    this.sortDirection = "asc";
  }

  reviewBatch(): void {
    if (this.result?.batchId) {
      const estateId = this.activeEstateService.activeEstateId();
      if (estateId) {
        this.router.navigate([
          "/estates",
          estateId,
          "transactions",
          "import",
          this.result.batchId,
        ]);
      } else {
        this.router.navigate(["/transactions/import", this.result.batchId]);
      }
    }
  }
}
