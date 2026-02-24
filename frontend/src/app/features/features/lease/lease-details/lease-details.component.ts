// features/lease/lease-details/lease-details.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { LeaseService } from '../../../core/services/lease.service';
import { Lease, LEASE_TYPE_LABELS } from '../../../models/lease.model';

@Component({
  selector: 'app-lease-details',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './lease-details.component.html',
  styleUrls: ['./lease-details.component.scss']
})
export class LeaseDetailsComponent implements OnInit {
  lease?: Lease;
  isLoading = false;
  errorMessage = '';
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  // Status action
  showStatusPanel = false;
  statusAction = '';
  statusError = '';
  isTransitioning = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private leaseService: LeaseService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadLease(+id);
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: lease => { this.lease = lease; this.isLoading = false; },
      error: () => { this.errorMessage = 'Lease not found.'; this.isLoading = false; }
    });
  }

  get statusClass(): string {
    const map: Record<string, string> = {
      ACTIVE: 'bg-success', DRAFT: 'bg-secondary', FINISHED: 'bg-info', CANCELLED: 'bg-danger'
    };
    return this.lease ? (map[this.lease.status] || 'bg-secondary') : '';
  }

  canEdit(): boolean { return this.lease?.status === 'DRAFT' || this.lease?.status === 'ACTIVE'; }
  canActivate(): boolean { return this.lease?.status === 'DRAFT'; }
  canFinish(): boolean { return this.lease?.status === 'ACTIVE'; }
  canCancel(): boolean { return this.lease?.status === 'DRAFT' || this.lease?.status === 'ACTIVE'; }

  doActivate(): void { this.transition('ACTIVE'); }
  doFinish():   void { this.transition('FINISHED'); }
  doCancel():   void { this.transition('CANCELLED'); }

  private transition(target: string): void {
    if (!this.lease) return;
    this.isTransitioning = true;
    this.leaseService.changeStatus(this.lease.id, { targetStatus: target as any }).subscribe({
      next: updated => { this.lease = updated; this.isTransitioning = false; },
      error: err => { this.statusError = err.error?.message || 'Transition failed.'; this.isTransitioning = false; }
    });
  }
}
