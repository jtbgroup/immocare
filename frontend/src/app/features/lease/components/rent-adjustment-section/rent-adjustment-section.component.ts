// features/lease/components/rent-adjustment-section/rent-adjustment-section.component.ts
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LeaseService } from '../../../../core/services/lease.service';
import { AdjustRentRequest, Lease, RentField } from '../../../../models/lease.model';

@Component({
  selector: 'app-rent-adjustment-section',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rent-adjustment-section.component.html',
  styleUrls: ['./rent-adjustment-section.component.scss'],
})
export class RentAdjustmentSectionComponent implements OnInit {
  @Input() lease!: Lease;
  @Output() leaseUpdated = new EventEmitter<void>();

  showForm = false;
  isSaving = false;
  formError = '';

  req: Partial<AdjustRentRequest> = { field: 'RENT' };

  /** Pre-fill new value when user picks a field */
  get currentValue(): number {
    return this.req.field === 'RENT' ? this.lease.monthlyRent : this.lease.monthlyCharges;
  }

  ngOnInit(): void {
    this.resetForm();
  }

  openForm(): void {
    this.resetForm();
    this.showForm = true;
    this.formError = '';
  }

  onFieldChange(): void {
    // Pre-fill newValue with the current value of the selected field
    this.req.newValue = this.currentValue;
  }

  save(): void {
    this.formError = '';
    if (!this.req.field || this.req.newValue == null || !this.req.reason || !this.req.effectiveDate) {
      this.formError = 'All fields are required.';
      return;
    }
    if (this.req.newValue <= 0) {
      this.formError = 'Amount must be greater than 0.';
      return;
    }
    this.isSaving = true;
    this.leaseService.adjustRent(this.lease.id, this.req as AdjustRentRequest).subscribe({
      next: () => {
        this.showForm = false;
        this.isSaving = false;
        this.leaseUpdated.emit();
      },
      error: (err) => {
        this.formError = err.error?.message || 'Failed to save adjustment.';
        this.isSaving = false;
      },
    });
  }

  private resetForm(): void {
    this.req = { field: 'RENT', newValue: this.lease?.monthlyRent };
  }

  constructor(private leaseService: LeaseService) {}
}
