import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { BankAccountService } from "../../../../core/services/bank-account.service";
import { ImportParserService } from "../../../../core/services/import-parser.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import {
  BankAccount,
  ImportBatchResult,
  ImportParser,
} from "../../../../models/transaction.model";

@Component({
  selector: "app-transaction-import",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./transaction-import.component.html",
  styleUrls: ["./transaction-import.component.scss"],
})
export class TransactionImportComponent implements OnInit {
  // ── Reference data ──────────────────────────────────────────────────────────
  parsers: ImportParser[] = [];
  bankAccounts: BankAccount[] = [];

  // ── Form state ──────────────────────────────────────────────────────────────
  selectedParserCode: string = "";
  selectedBankAccountId: number | null = null;
  selectedFile: File | null = null;
  isDragging = false;

  // ── Import state ─────────────────────────────────────────────────────────────
  importing = false;
  result: ImportBatchResult | null = null;
  error: string | null = null;

  constructor(
    private transactionService: TransactionService,
    private importParserService: ImportParserService,
    private bankAccountService: BankAccountService,
    private router: Router,
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

  get canImport(): boolean {
    return !!this.selectedFile && !!this.selectedParserCode;
  }

  // ── File selection ───────────────────────────────────────────────────────────

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(): void {
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files?.[0];
    if (file) this.selectFile(file);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.selectFile(file);
  }

  selectFile(file: File): void {
    this.error = null;
    this.result = null;

    // Validate extension against selected parser
    if (this.selectedParser) {
      const ext = file.name.split(".").pop()?.toLowerCase();
      const expected = this.selectedParser.format.toLowerCase();
      if (ext !== expected) {
        this.error = `Ce parseur attend un fichier ${this.selectedParser.format}. Fichier sélectionné: .${ext}`;
        return;
      }
    }

    this.selectedFile = file;
  }

  clearFile(): void {
    this.selectedFile = null;
    this.result = null;
    this.error = null;
  }

  // ── Import ───────────────────────────────────────────────────────────────────

  import(): void {
    if (!this.canImport) return;
    this.importing = true;
    this.error = null;
    this.result = null;

    this.transactionService
      .importFile(this.selectedFile!, this.selectedParserCode, this.selectedBankAccountId)
      .subscribe({
        next: (r) => {
          this.result = r;
          this.importing = false;
        },
        error: (err) => {
          this.error = err?.error?.errors?.[0]?.errorMessage || err?.error?.message || "Import échoué";
          this.importing = false;
        },
      });
  }

  reviewBatch(): void {
    if (this.result?.batchId) {
      this.router.navigate(["/transactions/import", this.result.batchId]);
    }
  }
}
