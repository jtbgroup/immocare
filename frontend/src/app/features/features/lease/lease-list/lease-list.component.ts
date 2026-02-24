// features/lease/lease-list/lease-list.component.ts
import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LeaseService } from '../../../core/services/lease.service';
import { LeaseSummary, LEASE_TYPE_LABELS } from '../../../models/lease.model';

@Component({
  selector: 'app-lease-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
<div class="card shadow-sm">
  <div class="card-header fw-semibold">Lease History</div>
  <div *ngIf="isLoading" class="card-body text-center py-3"><div class="spinner-border spinner-border-sm"></div></div>
  <div *ngIf="!isLoading && leases.length === 0" class="card-body text-muted small">No leases yet.</div>
  <div *ngIf="!isLoading && leases.length > 0" class="table-responsive">
    <table class="table table-hover mb-0">
      <thead class="table-light">
        <tr><th>Status</th><th>Type</th><th>Start</th><th>End</th><th>Tenant(s)</th><th>Rent</th><th></th></tr>
      </thead>
      <tbody>
        <tr *ngFor="let l of leases">
          <td><span class="badge" [ngClass]="statusClass(l.status)">{{ l.status }}</span></td>
          <td class="small">{{ LEASE_TYPE_LABELS[l.leaseType] }}</td>
          <td class="small">{{ l.startDate | date:'dd/MM/yyyy' }}</td>
          <td class="small">{{ l.endDate | date:'dd/MM/yyyy' }}</td>
          <td class="small">{{ l.tenantNames?.join(', ') || '—' }}</td>
          <td class="small">{{ l.monthlyRent | currency:'EUR':'symbol':'1.2-2' }}</td>
          <td><a [routerLink]="['/leases', l.id]" class="btn btn-sm btn-outline-secondary">View</a></td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
  `
})
export class LeaseListComponent implements OnInit {
  @Input() unitId!: number;
  leases: LeaseSummary[] = [];
  isLoading = false;
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  constructor(private leaseService: LeaseService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.leaseService.getByUnit(this.unitId).subscribe({
      next: leases => { this.leases = leases; this.isLoading = false; },
      error: () => { this.isLoading = false; }
    });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = { ACTIVE: 'bg-success', DRAFT: 'bg-secondary', FINISHED: 'bg-info', CANCELLED: 'bg-danger' };
    return map[status] || 'bg-secondary';
  }
}
