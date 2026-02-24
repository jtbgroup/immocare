// features/lease/indexation-section/indexation-section.component.ts
import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LeaseService } from '../../../core/services/lease.service';
import { Lease, RecordIndexationRequest } from '../../../models/lease.model';

@Component({
  selector: 'app-indexation-section',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="card shadow-sm mb-3">
  <div class="card-header d-flex justify-content-between align-items-center fw-semibold">
    <span>Indexation</span>
    <button *ngIf="lease.status === 'ACTIVE'" class="btn btn-primary btn-sm"
      (click)="showForm = !showForm">
      {{ showForm ? 'Cancel' : 'Record Indexation' }}
    </button>
  </div>
  <div class="card-body">

    <!-- Base info -->
    <div class="d-flex gap-4 text-muted small mb-3" *ngIf="lease.baseIndexValue">
      <span>Base index: <strong>{{ lease.baseIndexValue }}</strong> ({{ lease.baseIndexMonth | date:'MMM yyyy' }})</span>
      <span *ngIf="lease.indexationAnniversaryMonth">Anniversary: <strong>month {{ lease.indexationAnniversaryMonth }}</strong></span>
    </div>

    <!-- Record form -->
    <div *ngIf="showForm" class="border rounded p-3 mb-3 bg-light">
      <div *ngIf="formError" class="alert alert-danger py-2 small">{{ formError }}</div>
      <div class="row g-2">
        <div class="col-md-3">
          <label class="form-label small">Application Date *</label>
          <input type="date" class="form-control form-control-sm" [(ngModel)]="req.applicationDate" />
        </div>
        <div class="col-md-2">
          <label class="form-label small">Old Rent</label>
          <input type="number" class="form-control form-control-sm" [value]="lease.monthlyRent" readonly />
        </div>
        <div class="col-md-2">
          <label class="form-label small">New Index *</label>
          <input type="number" class="form-control form-control-sm" [(ngModel)]="req.newIndexValue" step="0.0001" />
        </div>
        <div class="col-md-2">
          <label class="form-label small">Index Month *</label>
          <input type="month" class="form-control form-control-sm" [(ngModel)]="req.newIndexMonth" />
        </div>
        <div class="col-md-3">
          <label class="form-label small">Applied Rent (€) *</label>
          <input type="number" class="form-control form-control-sm" [(ngModel)]="req.appliedRent" step="0.01" />
        </div>
        <div class="col-md-3">
          <label class="form-label small">Notification Date</label>
          <input type="date" class="form-control form-control-sm" [(ngModel)]="req.notificationSentDate" />
        </div>
        <div class="col-md-9">
          <label class="form-label small">Notes</label>
          <input type="text" class="form-control form-control-sm" [(ngModel)]="req.notes" />
        </div>
      </div>
      <div class="mt-2 d-flex gap-2">
        <button class="btn btn-primary btn-sm" (click)="save()" [disabled]="isSaving">
          <span *ngIf="isSaving" class="spinner-border spinner-border-sm me-1"></span>Save
        </button>
        <button class="btn btn-secondary btn-sm" (click)="showForm = false">Cancel</button>
      </div>
    </div>

    <!-- History table -->
    <div *ngIf="lease.indexations && lease.indexations.length > 0">
      <table class="table table-sm table-bordered mb-2">
        <thead class="table-light">
          <tr>
            <th>Application Date</th><th>Old Rent</th><th>Index Value</th>
            <th>Applied Rent</th><th>Notes</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let idx of lease.indexations">
            <td>{{ idx.applicationDate | date:'dd/MM/yyyy' }}</td>
            <td>{{ idx.oldRent | currency:'EUR':'symbol':'1.2-2' }}</td>
            <td>{{ idx.newIndexValue }}</td>
            <td><strong>{{ idx.appliedRent | currency:'EUR':'symbol':'1.2-2' }}</strong></td>
            <td>{{ idx.notes || '—' }}</td>
          </tr>
        </tbody>
      </table>
      <small class="text-muted" *ngIf="lease.indexations.length > 0">
        Total change since start: {{ totalChange | currency:'EUR':'symbol':'1.2-2' }}
        ({{ totalChangePct | number:'1.1-1' }}%)
      </small>
    </div>
    <p *ngIf="!lease.indexations || lease.indexations.length === 0" class="text-muted small">No indexations recorded yet.</p>
  </div>
</div>
  `
})
export class IndexationSectionComponent implements OnChanges {
  @Input() lease!: Lease;
  @Output() leaseUpdated = new EventEmitter<void>();

  showForm = false;
  isSaving = false;
  formError = '';

  req: RecordIndexationRequest = {
    applicationDate: new Date().toISOString().split('T')[0],
    newIndexValue: 0,
    newIndexMonth: '',
    appliedRent: 0
  };

  constructor(private leaseService: LeaseService) {}

  ngOnChanges(): void {
    if (this.lease) this.req.appliedRent = this.lease.monthlyRent;
  }

  get totalChange(): number {
    if (!this.lease.indexations?.length) return 0;
    const last = this.lease.indexations[0]; // DESC order
    const first = this.lease.indexations[this.lease.indexations.length - 1];
    return last.appliedRent - first.oldRent;
  }

  get totalChangePct(): number {
    if (!this.lease.indexations?.length) return 0;
    const first = this.lease.indexations[this.lease.indexations.length - 1];
    return first.oldRent > 0 ? (this.totalChange / first.oldRent) * 100 : 0;
  }

  save(): void {
    this.formError = '';
    if (!this.req.applicationDate || !this.req.newIndexValue || !this.req.appliedRent || !this.req.newIndexMonth) {
      this.formError = 'Please fill all required fields.'; return;
    }
    this.isSaving = true;
    const payload = { ...this.req, newIndexMonth: this.req.newIndexMonth + '-01' };
    this.leaseService.recordIndexation(this.lease.id, payload).subscribe({
      next: () => {
        this.showForm = false;
        this.isSaving = false;
        this.leaseUpdated.emit();
      },
      error: err => {
        this.formError = err.error?.message || 'Error saving indexation.';
        this.isSaving = false;
      }
    });
  }
}
