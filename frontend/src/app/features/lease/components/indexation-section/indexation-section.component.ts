// features/lease/indexation-section/indexation-section.component.ts
import { CommonModule } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { LeaseService } from "../../../../core/services/lease.service";
import { Lease, RecordIndexationRequest } from "../../../../models/lease.model";

@Component({
  selector: "app-indexation-section",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./indexation-section.component.html",
  styleUrls: ["./indexation-section.component.scss"],
})
export class IndexationSectionComponent implements OnChanges {
  @Input() lease!: Lease;
  @Output() leaseUpdated = new EventEmitter<void>();

  showForm = false;
  isSaving = false;
  formError = "";
  req: Partial<RecordIndexationRequest> = {};

  totalChange = 0;
  totalChangePct = 0;

  ngOnChanges(): void {
    this.computeTotals();
  }

  private computeTotals(): void {
    if (!this.lease?.indexations?.length) {
      this.totalChange = 0;
      this.totalChangePct = 0;
      return;
    }
    const first =
      this.lease.indexations[this.lease.indexations.length - 1].oldRent;
    const last = this.lease.indexations[0].appliedRent;
    this.totalChange = last - first;
    this.totalChangePct = first ? (this.totalChange / first) * 100 : 0;
  }

  save(): void {
    this.formError = "";
    if (
      !this.req.applicationDate ||
      !this.req.newIndexValue ||
      !this.req.newIndexMonth ||
      !this.req.appliedRent
    ) {
      this.formError = "Please fill in all required fields.";
      return;
    }
    this.isSaving = true;
    this.leaseService
      .recordIndexation(this.lease.id, this.req as RecordIndexationRequest)
      .subscribe({
        next: () => {
          this.showForm = false;
          this.req = {};
          this.isSaving = false;
          this.leaseUpdated.emit();
        },
        error: (err) => {
          this.formError = err.error?.message || "Failed to save indexation.";
          this.isSaving = false;
        },
      });
  }

  constructor(private leaseService: LeaseService) {}
}
