// ── ADDITIONS for frontend/src/app/models/person.model.ts ────────────────────
// Add these interfaces to the existing file.

export interface PersonBankAccount {
  id: number;
  personId: number;
  iban: string;
  label?: string;
  primary: boolean;
  createdAt: string;
}

export interface SavePersonBankAccountRequest {
  iban: string;
  label?: string;
  primary: boolean;
}

// Also extend the existing Person interface with:
//   bankAccounts?: PersonBankAccount[];
