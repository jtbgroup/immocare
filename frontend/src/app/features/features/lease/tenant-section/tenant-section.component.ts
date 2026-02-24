// features/lease/tenant-section/tenant-section.component.ts
import { CommonModule } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { LeaseService } from "../../../../core/services/lease.service";
import { Lease, LeaseTenant } from "../../../../models/lease.model";
import { PersonPickerComponent } from "../../../../shared/components/person-picker/person-picker.component";

@Component({
  selector: "app-tenant-section",
  standalone: true,
  imports: [CommonModule, FormsModule, PersonPickerComponent],
  template: `
    <div class="card shadow-sm mb-3">
      <div
        class="card-header d-flex justify-content-between align-items-center fw-semibold"
      >
        <span>Tenants</span>
        <button
          *ngIf="lease.status === 'DRAFT' || lease.status === 'ACTIVE'"
          class="btn btn-primary btn-sm"
          (click)="showPicker = !showPicker"
        >
          {{ showPicker ? "Cancel" : "+ Add Tenant" }}
        </button>
      </div>
      <div class="card-body">
        <div *ngIf="errorMessage" class="alert alert-danger py-2 small">
          {{ errorMessage }}
        </div>

        <!-- Tenant list -->
        <ul class="list-group list-group-flush mb-3">
          <li
            *ngFor="let t of lease.tenants"
            class="list-group-item d-flex align-items-center gap-3"
          >
            <div class="flex-grow-1">
              <strong>{{ t.lastName }} {{ t.firstName }}</strong>
              <span class="badge ms-2" [ngClass]="roleClass(t.role)">{{
                t.role
              }}</span>
              <span *ngIf="t.email" class="ms-2 small text-muted"
                >📧 {{ t.email }}</span
              >
              <span *ngIf="t.gsm" class="ms-2 small text-muted"
                >📱 {{ t.gsm }}</span
              >
            </div>
            <button
              *ngIf="canRemove(t)"
              class="btn btn-sm btn-outline-danger"
              (click)="remove(t.personId)"
              [disabled]="isRemoving"
            >
              ✕
            </button>
          </li>
          <li
            *ngIf="lease.tenants.length === 0"
            class="list-group-item text-muted small"
          >
            No tenants.
          </li>
        </ul>

        <!-- Add picker -->
        <div *ngIf="showPicker" class="border rounded p-3 bg-light">
          <app-person-picker
            label="Select Person"
            [required]="true"
            (personSelected)="onPersonSelected($event)"
          ></app-person-picker>
          <div class="mt-2">
            <label class="form-label small">Role</label>
            <select
              class="form-select form-select-sm w-auto"
              [(ngModel)]="newRole"
            >
              <option value="PRIMARY">Primary</option>
              <option value="CO_TENANT">Co-Tenant</option>
              <option value="GUARANTOR">Guarantor</option>
            </select>
          </div>
          <button
            *ngIf="selectedPerson"
            class="btn btn-primary btn-sm mt-2"
            (click)="confirmAdd()"
            [disabled]="isAdding"
          >
            <span
              *ngIf="isAdding"
              class="spinner-border spinner-border-sm me-1"
            ></span
            >Add Tenant
          </button>
        </div>
      </div>
    </div>
  `,
})
export class TenantSectionComponent {
  @Input() lease!: Lease;
  @Output() leaseUpdated = new EventEmitter<void>();

  showPicker = false;
  newRole = "PRIMARY";
  selectedPerson: any = null;
  errorMessage = "";
  isAdding = false;
  isRemoving = false;

  constructor(private leaseService: LeaseService) {}

  onPersonSelected(person: any): void {
    this.selectedPerson = person;
  }

  confirmAdd(): void {
    if (!this.selectedPerson) return;
    this.isAdding = true;
    this.errorMessage = "";
    this.leaseService
      .addTenant(this.lease.id, {
        personId: this.selectedPerson.id,
        role: this.newRole as any,
      })
      .subscribe({
        next: () => {
          this.showPicker = false;
          this.selectedPerson = null;
          this.isAdding = false;
          this.leaseUpdated.emit();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || "Failed to add tenant.";
          this.isAdding = false;
        },
      });
  }

  remove(personId: number): void {
    this.isRemoving = true;
    this.errorMessage = "";
    this.leaseService.removeTenant(this.lease.id, personId).subscribe({
      next: () => {
        this.isRemoving = false;
        this.leaseUpdated.emit();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || "Failed to remove tenant.";
        this.isRemoving = false;
      },
    });
  }

  canRemove(t: LeaseTenant): boolean {
    return this.lease.status === "DRAFT" || this.lease.status === "ACTIVE";
  }

  roleClass(role: string): string {
    return role === "PRIMARY"
      ? "bg-primary"
      : role === "CO_TENANT"
        ? "bg-secondary"
        : "bg-warning text-dark";
  }
}
