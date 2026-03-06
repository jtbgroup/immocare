import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
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

@Component({
  selector: "app-transaction-list",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./transaction-list.component.html",
  // styleUrls: ["./transaction-list.component.scss"],
})
export class TransactionListComponent implements OnInit {
  response: PagedTransactionResponse | null = null;
  loading = false;

  filter: TransactionFilter = {
    page: 0,
    size: 20,
    sort: "transactionDate,desc",
  };
  showFilters = false;

  categories: any[] = [];
  subcategories: any[] = [];
  bankAccounts: any[] = [];

  readonly DIRECTION_LABELS = DIRECTION_LABELS;
  readonly STATUS_LABELS = STATUS_LABELS;
  readonly statuses: TransactionStatus[] = ["DRAFT", "CONFIRMED", "RECONCILED"];
  readonly directions: TransactionDirection[] = ["INCOME", "EXPENSE"];

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
  }

  load(): void {
    this.loading = true;
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
    this.load();
  }

  goToPage(page: number): void {
    this.filter = { ...this.filter, page };
    this.load();
  }

  navigateToNew(): void {
    this.router.navigate(["/transactions/new"]);
  }

  navigateToDetail(tx: FinancialTransactionSummary): void {
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

  formatAmount(tx: FinancialTransactionSummary): string {
    const sign = tx.direction === "INCOME" ? "+" : "-";
    return sign + tx.amount.toFixed(2) + " €";
  }

  formatMonth(date: string): string {
    return date ? date.substring(0, 7) : "";
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
