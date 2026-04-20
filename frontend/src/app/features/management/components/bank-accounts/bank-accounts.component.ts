// features/management/components/bank-accounts/bank-accounts.component.ts — UC016 Phase 6
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActiveEstateService } from '../../../../core/services/active-estate.service';
import { BankAccountService } from '../../../../core/services/bank-account.service';
import {
  BankAccount,
  BankAccountType,
  SaveBankAccountRequest,
} from '../../../../models/transaction.model';

@Component({
  selector: 'app-bank-accounts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bank-accounts.component.html',
  styleUrls: ['./bank-accounts.component.scss'],
})
export class BankAccountsComponent implements OnInit {
  accounts: BankAccount[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  showForm = false;
  editingAccount?: BankAccount;

  formData: SaveBankAccountRequest = {
    label: '',
    accountNumber: '',
    type: 'CURRENT',
    isActive: true,
  };

  readonly accountTypes: BankAccountType[] = ['CURRENT', 'SAVINGS'];

  constructor(
    private bankAccountService: BankAccountService,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.bankAccountService.getAll().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load bank accounts.';
        this.loading = false;
      },
    });
  }

  resetForm(): void {
    this.formData = { label: '', accountNumber: '', type: 'CURRENT', isActive: true };
  }

  edit(ba: BankAccount): void {
    this.editingAccount = ba;
    this.formData = {
      label: ba.label,
      accountNumber: ba.accountNumber,
      type: ba.type,
      isActive: ba.isActive,
    };
    this.showForm = false;
  }

  save(): void {
    this.error = null;
    const obs = this.editingAccount
      ? this.bankAccountService.update(this.editingAccount.id, this.formData)
      : this.bankAccountService.create(this.formData);

    obs.subscribe({
      next: () => {
        this.editingAccount = undefined;
        this.showForm = false;
        this.resetForm();
        this.successMessage = 'Bank account saved.';
        setTimeout(() => (this.successMessage = null), 3000);
        this.load();
      },
      error: (err) => (this.error = err?.error?.message || 'Failed to save.'),
    });
  }
}
