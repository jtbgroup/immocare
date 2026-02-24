// features/lease/lease-form/lease-form.component.ts
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
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
  LeaseType,
} from "../../../../models/lease.model";
import { PersonPickerComponent } from "../../../../shared/components/person-picker/person-picker.component";

@Component({
  selector: "app-lease-form",
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    PersonPickerComponent,
  ],
  templateUrl: "./lease-form.component.html",
  // styleUrls: ['./lease-form.component.scss']
})
export class LeaseFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  leaseId?: number;
  unitId?: number;
  isLoading = false;
  isSaving = false;
  errorMessage = "";
  computedEndDate = "";

  readonly LEASE_TYPES = Object.keys(LEASE_TYPE_LABELS) as LeaseType[];
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  // Tenant management in form
  pendingTenants: Array<{ person: any; role: string }> = [];

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
      // Section 1 — General
      signatureDate: ["", Validators.required],
      startDate: ["", Validators.required],
      leaseType: ["MAIN_RESIDENCE_9Y", Validators.required],
      durationMonths: [108, [Validators.required, Validators.min(1)]],
      noticePeriodMonths: [3, [Validators.required, Validators.min(1)]],
      indexationNoticeDays: [30, Validators.required],
      indexationAnniversaryMonth: [null],
      // Section 2 — Financial
      monthlyRent: ["", [Validators.required, Validators.min(0.01)]],
      monthlyCharges: [0],
      chargesType: ["FORFAIT"],
      chargesDescription: [""],
      // Section 3 — Indexation
      baseIndexValue: [null],
      baseIndexMonth: [null],
      // Section 4 — Registration
      registrationSpf: [""],
      registrationRegion: [""],
      // Section 5 — Deposit
      depositAmount: [null],
      depositType: [null],
      depositReference: [""],
      // Section 6 — Insurance
      tenantInsuranceConfirmed: [false],
      tenantInsuranceReference: [""],
      tenantInsuranceExpiry: [null],
    });

    // Auto-compute end date when start or duration changes
    this.form
      .get("startDate")!
      .valueChanges.subscribe(() => this.computeEndDate());
    this.form
      .get("durationMonths")!
      .valueChanges.subscribe(() => this.computeEndDate());

    // Auto-fill notice period when lease type changes
    this.form.get("leaseType")!.valueChanges.subscribe((type) => {
      if (type) {
        this.form.patchValue({
          noticePeriodMonths: DEFAULT_NOTICE_MONTHS[type as LeaseType] || 3,
          durationMonths: LEASE_DURATION_MONTHS[type as LeaseType] || 108,
        });
      }
    });
  }

  computeEndDate(): void {
    const startDate = this.form.get("startDate")!.value;
    const duration = this.form.get("durationMonths")!.value;
    if (startDate && duration > 0) {
      const d = new Date(startDate);
      d.setMonth(d.getMonth() + duration);
      this.computedEndDate = d.toISOString().split("T")[0];
    }
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: (lease) => {
        this.form.patchValue({
          signatureDate: lease.signatureDate,
          startDate: lease.startDate,
          leaseType: lease.leaseType,
          durationMonths: lease.durationMonths,
          noticePeriodMonths: lease.noticePeriodMonths,
          indexationNoticeDays: lease.indexationNoticeDays,
          indexationAnniversaryMonth: lease.indexationAnniversaryMonth,
          monthlyRent: lease.monthlyRent,
          monthlyCharges: lease.monthlyCharges,
          chargesType: lease.chargesType,
          chargesDescription: lease.chargesDescription,
          baseIndexValue: lease.baseIndexValue,
          baseIndexMonth: lease.baseIndexMonth,
          registrationSpf: lease.registrationSpf,
          registrationRegion: lease.registrationRegion,
          depositAmount: lease.depositAmount,
          depositType: lease.depositType,
          depositReference: lease.depositReference,
          tenantInsuranceConfirmed: lease.tenantInsuranceConfirmed,
          tenantInsuranceReference: lease.tenantInsuranceReference,
          tenantInsuranceExpiry: lease.tenantInsuranceExpiry,
        });
        this.pendingTenants = lease.tenants.map((t) => ({
          person: {
            id: t.personId,
            lastName: t.lastName,
            firstName: t.firstName,
          },
          role: t.role,
        }));
        this.computeEndDate();
        this.unitId = lease.housingUnitId;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Lease not found.";
        this.isLoading = false;
      },
    });
  }

  addTenantToForm(person: any): void {
    if (!person) return;
    if (!this.pendingTenants.find((t) => t.person.id === person.id)) {
      this.pendingTenants.push({ person, role: "PRIMARY" });
    }
  }

  removeTenantFromForm(index: number): void {
    this.pendingTenants.splice(index, 1);
  }

  hasPrimaryTenant(): boolean {
    return this.pendingTenants.some((t) => t.role === "PRIMARY");
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  saveAsDraft(): void {
    this.submit(false);
  }
  saveAndActivate(): void {
    this.submit(true);
  }

  submit(activate: boolean): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.hasPrimaryTenant()) {
      this.errorMessage = "At least one PRIMARY tenant is required.";
      return;
    }

    this.isSaving = true;
    this.errorMessage = "";

    const value = this.form.value;
    const tenants: AddTenantRequest[] = this.pendingTenants.map((t) => ({
      personId: t.person.id,
      role: t.role,
    }));

    if (this.isEditMode) {
      this.leaseService.update(this.leaseId!, { ...value }).subscribe({
        next: (lease) => this.router.navigate(["/leases", lease.id]),
        error: (err) => {
          this.errorMessage = err.error?.message || "Error saving lease.";
          this.isSaving = false;
        },
      });
    } else {
      this.leaseService
        .create({ ...value, housingUnitId: this.unitId!, tenants }, activate)
        .subscribe({
          next: (lease) => this.router.navigate(["/leases", lease.id]),
          error: (err) => {
            this.errorMessage = err.error?.message || "Error creating lease.";
            this.isSaving = false;
          },
        });
    }
  }

  cancel(): void {
    if (this.isEditMode) this.router.navigate(["/leases", this.leaseId]);
    else this.router.navigate(["/housing-units", this.unitId]);
  }

  get showInsuranceDetails(): boolean {
    return !!this.form.get("tenantInsuranceConfirmed")?.value;
  }
}
