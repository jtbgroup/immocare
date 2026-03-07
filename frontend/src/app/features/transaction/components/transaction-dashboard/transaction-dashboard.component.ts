import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { TransactionService } from "../../../../core/services/transaction.service";
import {
  StatisticsFilter,
  TransactionStatistics,
} from "../../../../models/transaction.model";
import { BelgianCurrencyPipe } from "../../../../shared/pipes/belgian-currency.pipe";

@Component({
  selector: "app-transaction-dashboard",
  standalone: true,
  imports: [CommonModule, FormsModule, BelgianCurrencyPipe],
  templateUrl: "./transaction-dashboard.component.html",
  // styleUrls: ["./transaction-dashboard.component.scss"],
})
export class TransactionDashboardComponent implements OnInit {
  stats: TransactionStatistics | null = null;
  loading = false;

  filter: StatisticsFilter = {};

  constructor(private transactionService: TransactionService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.transactionService.getStatistics(this.filter).subscribe({
      next: (s) => {
        this.stats = s;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  get maxCategoryTotal(): number {
    if (!this.stats?.byCategory.length) return 0;
    return Math.max(...this.stats.byCategory.map((c) => c.categoryTotal));
  }

  barWidth(amount: number): string {
    const max = this.maxCategoryTotal;
    return max === 0 ? "0%" : ((amount / max) * 100).toFixed(1) + "%";
  }
}
