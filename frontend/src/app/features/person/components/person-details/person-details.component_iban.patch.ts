// ── PATCH for person-details.component.ts ────────────────────────────────────
//
// 1) Add import:
//    import { PersonBankAccountService } from '../../../../core/services/person-bank-account.service';
//    import { PersonBankAccount, SavePersonBankAccountRequest } from '../../../../models/person.model';
//
// 2) Add FormsModule to the standalone imports array.
//
// 3) Inject in constructor:
//    private personBankAccountService: PersonBankAccountService
//
// 4) Add these fields and methods to the component class:

  // ── IBAN management ───────────────────────────────────────────────────────

  showIbanForm = false;
  editingIban?: PersonBankAccount;
  ibanError    = '';
  ibanSuccess  = '';

  ibanForm: SavePersonBankAccountRequest = { iban: '', label: '', primary: false };

  resetIbanForm(): void {
    this.ibanForm = { iban: '', label: '', primary: false };
    this.ibanError   = '';
    this.ibanSuccess = '';
  }

  editIban(ba: PersonBankAccount): void {
    this.editingIban = ba;
    this.ibanForm    = { iban: ba.iban, label: ba.label ?? '', primary: ba.primary };
    this.showIbanForm = false;
    this.ibanError    = '';
    this.ibanSuccess  = '';
  }

  saveIban(): void {
    if (!this.person) return;
    this.ibanError = '';
    const personId = this.person.id;
    const obs = this.editingIban
      ? this.personBankAccountService.update(personId, this.editingIban.id, this.ibanForm)
      : this.personBankAccountService.create(personId, this.ibanForm);

    obs.subscribe({
      next: () => {
        this.showIbanForm = false;
        this.editingIban  = undefined;
        this.ibanSuccess  = 'IBAN saved.';
        setTimeout(() => (this.ibanSuccess = ''), 3000);
        this.loadPerson(personId);  // refresh to get updated bankAccounts list
      },
      error: (err) => {
        this.ibanError = err?.error?.message || 'Failed to save IBAN.';
      },
    });
  }

  deleteIban(id: number): void {
    if (!this.person) return;
    if (!confirm('Delete this IBAN?')) return;
    this.personBankAccountService.delete(this.person.id, id).subscribe({
      next: () => this.loadPerson(this.person!.id),
      error: (err) => (this.ibanError = err?.error?.message || 'Failed to delete IBAN.'),
    });
  }
