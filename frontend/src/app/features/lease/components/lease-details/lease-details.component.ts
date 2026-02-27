// features/lease/lease-details/lease-details.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { LeaseService } from '../../../../core/services/lease.service';
import { Lease, LEASE_TYPE_LABELS } from '../../../../models/lease.model';
import { TenantSectionComponent } from '../tenant-section/tenant-section.component';
import { RentAdjustmentSectionComponent } from '../rent-adjustment-section/rent-adjustment-section.component';

@Component({
  selector: 'app-lease-details',
  standalone: true,
  imports: [CommonModule, RouterModule, TenantSectionComponent, RentAdjustmentSectionComponent],
  templateUrl: './lease-details.component.html',
  styleUrls: ['./lease-details.component.scss'],
})
export class LeaseDetailsComponent implements OnInit {
  lease?: Lease;
  isLoading = false;
  errorMessage = '';
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  constructor(private route: ActivatedRoute, private leaseService: LeaseService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadLease(+id);
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: (lease) => { this.lease = lease; this.isLoading = false; },
      error: () => { this.errorMessage = 'Lease not found.'; this.isLoading = false; },
    });
  }

  get statusClass(): string {
    const map: Record<string, string> = {
      ACTIVE: 'bg-success', DRAFT: 'bg-secondary',
      FINISHED: 'bg-info', CANCELLED: 'bg-danger',
    };
    return this.lease ? map[this.lease.status] || 'bg-secondary' : '';
  }

  canEdit(): boolean {
    return this.lease?.status === 'DRAFT' || this.lease?.status === 'ACTIVE';
  }
}
