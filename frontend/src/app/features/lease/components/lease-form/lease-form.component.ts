// features/lease/lease-form/lease-form.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";

import { LeaseService } from "../../../../core/services/lease.service";
import {
  AddTenantRequest,
  DEFAULT_NOTICE_MONTHS,
  LEASE_DURATION_MONTHS,
  LEASE_TYPE_LABELS,
  LeaseStatus,
  LeaseType,
  TenantRole,
} from "../../../../models/lease.model";
import { PersonPickerComponent } from "../../../../shared/components/person-picker/person-picker.component";

@Component({
  selector: "app-lease-form",
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    FormsModule,
    PersonPickerComponent,
  ],
  templateUrl: "./lease-form.component.html",
  styleUrls: ["./lease-form.component.scss"],
})
export class LeaseFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  leaseId?: number;
  unitId?: number;
  isLoading = false;
  isSaving = false;
  isTransitioning = false;
  errorMessage = "";
  statusError = "";
  computedEndDate = "";

  currentStatus?: LeaseStatus;
  selectedStatus: LeaseStatus | null = null;

  readonly LEASE_TYPES = Object.keys(LEASE_TYPE_LABELS) as LeaseType[];
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  pendingTenants: Array<{ person: any; role: TenantRole }> = [];

  // Transitions disponibles selon le statut courant
  get availableStatuses(): { value: LeaseStatus; label: string }[] {
    const map: Record<string, { value: LeaseStatus; label: string }[]> = {
      DRAFT:  [{ value: "ACTIVE", label: "Activate" }, { value: "CANCELLED", label: "Cancel lease" }],
      ACTIVE: [{ value: "FINISHED", label: "Mark as finished" }, { value: "CANCELLED", label: "Cancel lease" }],
    };
    return this.currentStatus ? (map[this.currentStatus] ?? []) : [];
  }

  // "Activate" masqué si startDate manquante
  get filteredAvailableStatuses(): { value: LeaseStatus; label: string }[] {
    const hasStart = !!this.form?.get("startDate")?.value;
    return this.availableStatuses.filter(s => s.value !== "ACTIVE" || hasStart);
  }

  get anniversaryMonthLabel(): string {
    const startDate = this.form?.get("startDate")?.value;
    if (!startDate) return "";
    const month = new Date(startDate + "T00:00:00").getMonth() + 1;
    return new Date(2000, month - 1, 1).toLocaleString("en", { month: "long" });
  }

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private leaseService: LeaseService,
  ) {}

  ngOnInit(): void {
    this.unitId = +(this.route.snapshot.paramMap.get("unitId") || 0);
    const id = this.route.snapshot.paramMap.get("id");
    if (id) {
      this.isEditMode = true;
      this.leaseId = +id;
    }
    this.buildForm();
    if (this.isEditMode) this.loadLease(this.leaseId!);
  }

  buildForm(): void {
    this.form = this.fb.group({
      signatureDate: ["", Validators.required],
      startDate: ["", Validators.required],
      leaseType: ["MAIN_RESIDENCE_9Y", Validators.required],
      durationMonths: [108, [Validators.required, Validators.min(1)]],
      noticePeriodMonths: [3, [Validators.required, Validators.min(1)]],
      monthlyRent: ["", [Validators.required, Validators.min(0.01)]],
      monthlyCharges: [0],
      chargesType: ["FORFAIT"],
      chargesDescription: [""],
      indexationNoticeDays: [30],
      baseIndexValue: [null],
      baseIndexMonth: [null],
      registrationSpf: [""],
      registrationInventorySpf: [""],
      registrationRegion: [""],
      depositAmount: [null],
      depositType: [null],
      depositReference: [""],
      tenantInsuranceConfirmed: [false],
      tenantInsuranceReference: [""],
      tenantInsuranceExpiry: [null],
    });

    this.form.get("startDate")!.valueChanges.subscribe(() => this.computeEndDate());
    this.form.get("durationMonths")!.valueChanges.subscribe(() => this.computeEndDate());
    this.form.get("leaseType")!.valueChanges.subscribe((type) => {
      if (type) {
        this.form.patchValue(
          {
            noticePeriodMonths: DEFAULT_NOTICE_MONTHS[type as LeaseType] || 3,
            durationMonths: LEASE_DURATION_MONTHS[type as LeaseType] || 108,
          },
          { emitEvent: false },
        );
      }
    });
  }

  computeEndDate(): void {
    const startDate = this.form.get("startDate")!.value;
    const duration = this.form.get("durationMonths")!.value;
    if (startDate && duration > 0) {
      const d = new Date(startDate + "T00:00:00");
      d.setMonth(d.getMonth() + duration);
      this.computedEndDate = d.toISOString().split("T")[0];
    } else {
      this.computedEndDate = "";
    }
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: (lease) => {
        this.currentStatus = lease.status;
        this.form.patchValue({
          signatureDate: lease.signatureDate,
          startDate: lease.startDate,
          leaseType: lease.leaseType,
          durationMonths: lease.durationMonths,
          noticePeriodMonths: lease.noticePeriodMonths,
          indexationNoticeDays: lease.indexationNoticeDays,
          monthlyRent: lease.monthlyRent,
          monthlyCharges: lease.monthlyCharges,
          chargesType: lease.chargesType,
          chargesDescription: lease.chargesDescription,
          baseIndexValue: lease.baseIndexValue,
          baseIndexMonth: lease.baseIndexMonth,
          registrationSpf: lease.registrationSpf,
          registrationInventorySpf: lease.registrationInventorySpf,
          registrationRegion: lease.registrationRegion,
          depositAmount: lease.depositAmount,
          depositType: lease.depositType,
          depositReference: lease.depositReference,
          tenantInsuranceConfirmed: lease.tenantInsuranceConfirmed,
          tenantInsuranceReference: lease.tenantInsuranceReference,
          tenantInsuranceExpiry: lease.tenantInsuranceExpiry,
        });
        this.pendingTenants = (lease.tenants || []).map((t) => ({
          person: { id: t.personId, lastName: t.lastName, firstName: t.firstName },
          role: t.role,
        }));
        this.unitId = lease.housingUnitId;
        this.computeEndDate();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Lease not found.";
        this.isLoading = false;
      },
    });
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  get showInsuranceDetails(): boolean {
    return this.form.get("tenantInsuranceConfirmed")!.value === true;
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  hasPrimaryTenant(): boolean {
    return this.pendingTenants.some((t) => t.role === "PRIMARY");
  }

  addTenantToForm(person: any): void {
    if (!person) return;
    if (this.pendingTenants.some((t) => t.person.id === person.id)) return;
    const role: TenantRole = this.pendingTenants.length === 0 ? "PRIMARY" : "CO_TENANT";
    this.pendingTenants.push({ person, role });
  }

  removeTenantFromForm(index: number): void {
    this.pendingTenants.splice(index, 1);
  }

  updateTenantRole(index: number, event: Event): void {
    this.pendingTenants[index].role = (event.target as HTMLSelectElement).value as TenantRole;
  }

  // ─── Actions ──────────────────────────────────────────────────────────────

  saveAsDraft(): void {
    this.submit(false);
  }

  saveAndActivate(): void {
    if (!this.form.get("startDate")?.value) {
      this.form.get("startDate")!.markAsTouched();
      this.errorMessage = "Start date is required to activate a lease.";
      return;
    }
    this.submit(true);
  }

  saveChanges(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    if (this.selectedStatus === "ACTIVE" && !this.form.get("startDate")?.value) {
      this.statusError = "Start date is required to activate a lease.";
      return;
    }

    this.isSaving = true;
    this.errorMessage = "";
    this.statusError = "";

    this.leaseService.update(this.leaseId!, { ...this.form.value }).subscribe({
      next: (lease) => {
        this.isSaving = false;
        if (this.selectedStatus) {
          this.applyStatusTransition(lease.id, this.selectedStatus);
        } else {
          this.router.navigate(["/leases", lease.id]);
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.message || "Error saving lease.";
        this.isSaving = false;
      },
    });
  }

  private applyStatusTransition(id: number, target: LeaseStatus): void {
    this.isTransitioning = true;
    this.leaseService.changeStatus(id, { targetStatus: target }).subscribe({
      next: (updated) => {
        this.isTransitioning = false;
        this.router.navigate(["/leases", updated.id]);
      },
      error: (err) => {
        this.statusError = err.error?.message || "Status change failed.";
        this.isTransitioning = false;
        this.router.navigate(["/leases", id]);
      },
    });
  }

  private submit(activate: boolean): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    if (!this.hasPrimaryTenant()) {
      this.errorMessage = "At least one PRIMARY tenant is required.";
      return;
    }

    this.isSaving = true;
    this.errorMessage = "";

    const tenants: AddTenantRequest[] = this.pendingTenants.map((t) => ({
      personId: t.person.id,
      role: t.role,
    }));

    this.leaseService
      .create({ ...this.form.value, housingUnitId: this.unitId!, tenants }, activate)
      .subscribe({
        next: (lease) => this.router.navigate(["/leases", lease.id]),
        error: (err) => {
          this.errorMessage = err.error?.message || "Error creating lease.";
          this.isSaving = false;
        },
      });
  }

  cancel(): void {
    if (this.isEditMode) {
      this.router.navigate(["/leases", this.leaseId]);
    } else {
      this.router.navigate(["/housing-units", this.unitId]);
    }
  }
}
