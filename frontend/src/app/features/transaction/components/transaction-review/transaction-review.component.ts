import { CommonModule, DecimalPipe } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import {
  DIRECTION_LABELS,
  FinancialTransactionSummary,
  PagedTransactionResponse,
  STATUS_LABELS,
} from "../../../../models/transaction.model";
import { SortIconPipe } from "../../../../shared/pipes/sort-icon.pipe";

@Component({
  selector: "app-transaction-review",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DecimalPipe, SortIconPipe],
  templateUrl: "./transaction-review.component.html",
  styleUrls: ["./transaction-review.component.scss"],
})
export class TransactionReviewComponent implements OnInit {
  batchId!: number;
  response: PagedTransactionResponse | null = null;
  loading = false;
  confirmingAll = false;

  readonly DIRECTION_LABELS = DIRECTION_LABELS;
  readonly STATUS_LABELS = STATUS_LABELS;

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService,
    private tagSubcategoryService: TagSubcategoryService,
  ) {}

  sortField = "transactionDate";
  sortDirection: "asc" | "desc" = "asc";

  sortBy(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortField = field;
      this.sortDirection = "asc";
    }
    this.load();
  }

  ngOnInit(): void {
    this.batchId = Number(this.route.snapshot.paramMap.get("batchId"));
    this.load();
  }

  load(): void {
    this.loading = true;
    this.transactionService
      .getBatch(this.batchId, 0, 200, `${this.sortField},${this.sortDirection}`)
      .subscribe({
        next: (r) => {
          this.response = r;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  confirmRow(tx: FinancialTransactionSummary): void {
    this.transactionService.confirm(tx.id, {}).subscribe(() => this.load());
  }

  confirmAll(): void {
    this.confirmingAll = true;
    this.transactionService.confirmBatch(this.batchId).subscribe({
      next: () => {
        this.confirmingAll = false;
        this.load();
      },
      error: () => {
        this.confirmingAll = false;
      },
    });
  }

  get draftCount(): number {
    return (
      this.response?.content.filter((t) => t.status === "DRAFT").length ?? 0
    );
  }

  get confirmedCount(): number {
    return (
      this.response?.content.filter((t) => t.status === "CONFIRMED").length ?? 0
    );
  }
}
