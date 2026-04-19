// features/transaction/components/transaction-detail/transaction-detail.component.ts — UC016 Phase 4
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ActiveEstateService } from '../../../../core/services/active-estate.service';
import { TransactionService } from '../../../../core/services/transaction.service';
import {
  DIRECTION_LABELS,
  FinancialTransaction,
  STATUS_LABELS,
} from '../../../../models/transaction.model';

@Component({
  selector: 'app-transaction-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './transaction-detail.component.html',
  styleUrls: ['./transaction-detail.component.scss'],
})
export class TransactionDetailComponent implements OnInit {
  transaction?: FinancialTransaction;
  loading = false;

  readonly DIRECTION_LABELS = DIRECTION_LABELS;
  readonly STATUS_LABELS = STATUS_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading = true;
    this.transactionService.getById(id).subscribe({
      next: (tx) => {
        this.transaction = tx;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private get estateId(): string | null {
    return this.activeEstateService.activeEstateId();
  }

  edit(): void {
    if (this.estateId) {
      this.router.navigate(['/estates', this.estateId, 'transactions', this.transaction!.id, 'edit']);
    } else {
      this.router.navigate(['/transactions', this.transaction!.id, 'edit']);
    }
  }

  delete(): void {
    if (!confirm('Delete this transaction?')) return;
    this.transactionService.delete(this.transaction!.id).subscribe(() => {
      if (this.estateId) {
        this.router.navigate(['/estates', this.estateId, 'transactions']);
      } else {
        this.router.navigate(['/transactions']);
      }
    });
  }

  back(): void {
    if (this.estateId) {
      this.router.navigate(['/estates', this.estateId, 'transactions']);
    } else {
      this.router.navigate(['/transactions']);
    }
  }
}
