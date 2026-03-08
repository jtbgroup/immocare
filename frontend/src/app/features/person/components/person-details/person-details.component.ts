// features/person/person-details/person-details.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { PersonBankAccountService } from '../../../../core/services/person-bank-account.service';
import { PersonService } from '../../../../core/services/person.service';
import { Person, PersonBankAccount, SavePersonBankAccountRequest } from '../../../../models/person.model';
import { AppDatePipe } from '../../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-person-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AppDatePipe],
  templateUrl: './person-details.component.html',
  styleUrls: ['./person-details.component.scss'],
})
export class PersonDetailsComponent implements OnInit {

  // ── Person ────────────────────────────────────────────────────────────────
  person?: Person;
  isLoading    = false;
  errorMessage = '';

  // ── Delete ────────────────────────────────────────────────────────────────
  showDeleteConfirm = false;
  isDeleting        = false;
  deleteError       = '';
  deleteErrorDetails?: {
    ownedBuildings: string[];
    ownedUnits:     string[];
    activeLeases:   string[];
  };

  // ── IBAN management ───────────────────────────────────────────────────────
  showIbanForm  = false;
  editingIban?: PersonBankAccount;
  ibanError     = '';
  ibanSuccess   = '';

  ibanForm: SavePersonBankAccountRequest = { iban: '', label: '', primary: false };

  constructor(
    private route:                    ActivatedRoute,
    private router:                   Router,
    private personService:            PersonService,
    private personBankAccountService: PersonBankAccountService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadPerson(+id);
  }

  // ── Person loading ────────────────────────────────────────────────────────

  loadPerson(id: number): void {
    this.isLoading = true;
    this.personService.getById(id).subscribe({
      next: (p) => {
        this.person    = p;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Person not found.';
        this.isLoading    = false;
      },
    });
  }

  // ── Delete ────────────────────────────────────────────────────────────────

  confirmDelete(): void {
    this.showDeleteConfirm  = true;
    this.deleteError        = '';
    this.deleteErrorDetails = undefined;
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
  }

  doDelete(): void {
    if (!this.person) return;
    this.isDeleting = true;
    this.personService.delete(this.person.id).subscribe({
      next: () => this.router.navigate(['/persons']),
      error: (err) => {
        this.isDeleting = false;
        if (err.status === 409) {
          this.deleteError        = err.error?.message || 'This person cannot be deleted.';
          this.deleteErrorDetails = {
            ownedBuildings: err.error?.ownedBuildings || [],
            ownedUnits:     err.error?.ownedUnits     || [],
            activeLeases:   err.error?.activeLeases   || [],
          };
        } else {
          this.deleteError = 'An error occurred. Please try again.';
        }
        this.showDeleteConfirm = false;
      },
    });
  }

  // ── IBAN management ───────────────────────────────────────────────────────

  resetIbanForm(): void {
    this.ibanForm    = { iban: '', label: '', primary: false };
    this.ibanError   = '';
    this.ibanSuccess = '';
  }

  editIban(ba: PersonBankAccount): void {
    this.editingIban  = ba;
    this.ibanForm     = { iban: ba.iban, label: ba.label ?? '', primary: ba.primary };
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
        this.loadPerson(personId);
      },
      error: (err) => {
        this.ibanError = err?.error?.message || 'Failed to save IBAN.';
      },
    });
  }

  deleteIban(id: number): void {
    if (!this.person) return;
    if (!confirm('Delete this IBAN?')) return;
    const personId = this.person.id;
    this.personBankAccountService.delete(personId, id).subscribe({
      next:  () => this.loadPerson(personId),
      error: (err) => (this.ibanError = err?.error?.message || 'Failed to delete IBAN.'),
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE:    'bg-success',
      DRAFT:     'bg-secondary',
      FINISHED:  'bg-info',
      CANCELLED: 'bg-danger',
    };
    return map[status] || 'bg-secondary';
  }
}
