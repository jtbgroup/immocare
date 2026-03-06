import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { Router } from "@angular/router";
import { TransactionService } from "../../../../core/services/transaction.service";
import { ImportBatchResult } from "../../../../models/transaction.model";

@Component({
  selector: "app-transaction-import",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./transaction-import.component.html",
  // styleUrls: ["./transaction-import.component.scss"],
})
export class TransactionImportComponent {
  isDragging = false;
  selectedFile: File | null = null;
  importing = false;
  result: ImportBatchResult | null = null;
  error: string | null = null;

  constructor(
    private transactionService: TransactionService,
    private router: Router,
  ) {}

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
    if (!file.name.endsWith(".csv")) {
      this.error = "Please select a CSV file.";
      return;
    }
    this.selectedFile = file;
    this.result = null;
    this.error = null;
  }

  import(): void {
    if (!this.selectedFile) return;
    this.importing = true;
    this.error = null;
    this.transactionService.importCsv(this.selectedFile).subscribe({
      next: (r) => {
        this.result = r;
        this.importing = false;
      },
      error: (err) => {
        this.error = err?.error?.message || "Import failed";
        this.importing = false;
      },
    });
  }

  reviewBatch(): void {
    if (this.result) {
      this.router.navigate(["/transactions/import", this.result.batchId]);
    }
  }
}
