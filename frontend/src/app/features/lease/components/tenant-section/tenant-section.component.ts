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
  templateUrl: "./tenant-section.component.html",
  styleUrls: ["./tenant-section.component.scss"],
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
