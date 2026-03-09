import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { BankAccountService } from "../../../../core/services/bank-account.service";
import { TagCategoryService } from "../../../../core/services/tag-category.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import {
  DIRECTION_LABELS,
  FinancialTransactionSummary,
  PagedTransactionResponse,
  STATUS_LABELS,
  TransactionDirection,
  TransactionFilter,
  TransactionStatus,
} from "../../../../models/transaction.model";
import { BelgianCurrencyPipe } from "../../../../shared/pipes/belgian-currency.pipe";
import { SortIconPipe } from "../../../../shared/pipes/sort-icon.pipe";

@Component({
  selector: "app-transaction-list",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    SortIconPipe,
    BelgianCurrencyPipe,
  ],
  templateUrl: "./transaction-list.component.html",
  styleUrls: ["./transaction-list.component.scss"],
})
export class TransactionListComponent implements OnInit {
  response: PagedTransactionResponse | null = null;
  loading = false;

  sortField = "transactionDate";
  sortDirection: "asc" | "desc" = "desc";

  filter: TransactionFilter = {
    page: 0,
    size: 20,
    sort: "transactionDate,desc",
  };
  showFilters = false;

  categories: any[] = [];
  subcategories: any[] = [];
  bankAccounts: any[] = [];
  allSubcategories: any[] = []; // for bulk picker (no direction filter)

  readonly DIRECTION_LABELS = DIRECTION_LABELS;
  readonly STATUS_LABELS = STATUS_LABELS;
  readonly statuses: TransactionStatus[] = ["DRAFT", "CONFIRMED", "RECONCILED"];
  readonly directions: TransactionDirection[] = ["INCOME", "EXPENSE"];

  // ── Selection state ──────────────────────────────────────────────────────────
  selectedIds = new Set<number>();
  showBulkPanel = false;

  // Bulk edit fields
  bulkStatus: TransactionStatus | "" = "";
  bulkSubcategoryId: number | "" = "";
  bulkApplying = false;
  bulkResult: { updatedCount: number; skippedCount: number } | null = null;

  constructor(
    private transactionService: TransactionService,
    private tagCategoryService: TagCategoryService,
    private tagSubcategoryService: TagSubcategoryService,
    private bankAccountService: BankAccountService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
    this.tagCategoryService.getAll().subscribe((c) => (this.categories = c));
    this.bankAccountService.getAll().subscribe((b) => (this.bankAccounts = b));
    this.tagSubcategoryService
      .getAll()
      .subscribe((s) => (this.allSubcategories = s));
  }

  load(): void {
    this.loading = true;
    this.selectedIds.clear();
    this.bulkResult = null;
    this.transactionService.getTransactions(this.filter).subscribe({
      next: (r) => {
        this.response = r;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  onCategoryChange(): void {
    if (this.filter.categoryId) {
      this.tagSubcategoryService
        .getAll(this.filter.categoryId)
        .subscribe((s) => (this.subcategories = s));
    } else {
      this.subcategories = [];
      this.filter.subcategoryId = undefined;
    }
  }

  clearFilters(): void {
    this.filter = { page: 0, size: 20, sort: "transactionDate,desc" };
    this.sortField = "transactionDate";
    this.sortDirection = "desc";
    this.load();
  }

  sortBy(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortField = field;
      this.sortDirection = "asc";
    }
    this.filter = {
      ...this.filter,
      page: 0,
      sort: `${this.sortField},${this.sortDirection}`,
    };
    this.load();
  }

  goToPage(page: number): void {
    this.filter = { ...this.filter, page };
    this.load();
  }

  // ── Selection ────────────────────────────────────────────────────────────────

  toggleRow(tx: FinancialTransactionSummary, event: Event): void {
    event.stopPropagation();
    if (this.selectedIds.has(tx.id)) {
      this.selectedIds.delete(tx.id);
    } else {
      this.selectedIds.add(tx.id);
    }
    this.bulkResult = null;
  }

  toggleAll(checked: boolean): void {
    this.response?.content.forEach((tx) => {
      if (checked) this.selectedIds.add(tx.id);
      else this.selectedIds.delete(tx.id);
    });
    this.bulkResult = null;
  }

  isSelected(tx: FinancialTransactionSummary): boolean {
    return this.selectedIds.has(tx.id);
  }

  get allSelected(): boolean {
    return (
      !!this.response?.content.length &&
      this.response.content.every((tx) => this.selectedIds.has(tx.id))
    );
  }

  get someSelected(): boolean {
    return this.selectedIds.size > 0;
  }

  // ── Bulk apply ───────────────────────────────────────────────────────────────

  applyBulk(): void {
    if (!this.someSelected) return;
    if (!this.bulkStatus && this.bulkSubcategoryId === "") return;

    const req: any = { ids: Array.from(this.selectedIds) };
    if (this.bulkStatus) req.status = this.bulkStatus;
    if (this.bulkSubcategoryId !== "")
      req.subcategoryId = this.bulkSubcategoryId;

    this.bulkApplying = true;
    this.bulkResult = null;
    this.transactionService.bulkPatch(req).subscribe({
      next: (r) => {
        this.bulkResult = r;
        this.bulkApplying = false;
        this.bulkStatus = "";
        this.bulkSubcategoryId = "";
        this.load();
      },
      error: () => {
        this.bulkApplying = false;
      },
    });
  }

  clearBulkSelection(): void {
    this.selectedIds.clear();
    this.bulkStatus = "";
    this.bulkSubcategoryId = "";
    this.bulkResult = null;
  }

  // ── Navigation ───────────────────────────────────────────────────────────────

  navigateToNew(): void {
    this.router.navigate(["/transactions/new"]);
  }

  navigateToDetail(tx: FinancialTransactionSummary): void {
    if (this.someSelected) return; // don't navigate when in selection mode
    this.router.navigate(["/transactions", tx.id]);
  }

  navigateToEdit(tx: FinancialTransactionSummary, event: Event): void {
    event.stopPropagation();
    this.router.navigate(["/transactions", tx.id, "edit"]);
  }

  deleteTransaction(tx: FinancialTransactionSummary, event: Event): void {
    event.stopPropagation();
    if (!confirm("Delete transaction " + tx.reference + "?")) return;
    this.transactionService.delete(tx.id).subscribe(() => this.load());
  }

  exportCsv(): void {
    this.transactionService.exportCsv(this.filter).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "transactions.csv";
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  get activeFilterCount(): number {
    return Object.entries(this.filter).filter(
      ([k, v]) =>
        !["page", "size", "sort"].includes(k) &&
        v !== null &&
        v !== undefined &&
        v !== "",
    ).length;
  }
}
