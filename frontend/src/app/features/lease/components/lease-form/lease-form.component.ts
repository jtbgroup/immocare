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
  DEFAULT_NOTICE_MONTHS,
  LEASE_DURATION_MONTHS,
  LEASE_TYPE_LABELS,
  LEASE_TYPES,
  LeaseType,
  TenantRole,
} from "../../../../models/lease.model";

@Component({
  selector: "app-lease-form",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: "./lease-form.component.html",
  styleUrls: ["./lease-form.component.scss"],
})
export class LeaseFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  leaseId?: number;
  unitId?: number;
  isLoading = false;
  errorMessage = "";

  readonly LEASE_TYPES = LEASE_TYPES;
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;

  /** Flag to prevent circular updates between endDate ↔ durationMonths */
  private _updatingDates = false;

  pendingTenants: {
    person: { id: number; lastName: string; firstName: string };
    role: string;
  }[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private leaseService: LeaseService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.setupBidirectionalDates();

    const idParam = this.route.snapshot.paramMap.get("id");
    if (idParam) {
      this.isEditMode = true;
      this.leaseId = +idParam;
      this.loadLease(this.leaseId);
    } else {
      this.unitId =
        +(this.route.snapshot.queryParamMap.get("unitId") ?? 0) || undefined;
    }
  }

  private buildForm(): void {
    this.form = this.fb.group({
      signatureDate: [null, Validators.required],
      startDate: [null, Validators.required],
      endDate: [null, Validators.required],
      leaseType: [LEASE_TYPES[3], Validators.required],
      durationMonths: [108, [Validators.required, Validators.min(1)]],
      noticePeriodMonths: [3, [Validators.required, Validators.min(1)]],
      monthlyRent: [null, [Validators.required, Validators.min(0.01)]],
      monthlyCharges: [0],
      chargesType: ["FORFAIT"],
      chargesDescription: [null],
      registrationSpf: [null],
      registrationRegion: [null],
      registrationInventorySpf: [null],
      registrationInventoryRegion: [null],
      depositAmount: [null],
      depositType: [null],
      depositReference: [null],
      tenantInsuranceConfirmed: [false],
      tenantInsuranceReference: [null],
      tenantInsuranceExpiry: [null],
    });

    // When leaseType changes, update defaults for duration and notice
    this.form.get("leaseType")!.valueChanges.subscribe((type) => {
      if (type) {
        this.form.patchValue(
          {
            noticePeriodMonths: DEFAULT_NOTICE_MONTHS[type as LeaseType] || 3,
            durationMonths: LEASE_DURATION_MONTHS[type as LeaseType] || 108,
          },
          { emitEvent: true },
        ); // emitEvent true so endDate gets recomputed
      }
    });
  }

  /**
   * Wire up the bidirectional link between startDate, durationMonths, and endDate.
   * - startDate or durationMonths changed → recompute endDate
   * - endDate changed manually → recompute durationMonths
   */
  private setupBidirectionalDates(): void {
    const startCtrl = this.form.get("startDate")!;
    const durationCtrl = this.form.get("durationMonths")!;
    const endCtrl = this.form.get("endDate")!;

    // startDate or durationMonths → endDate
    const recomputeEnd = () => {
      if (this._updatingDates) return;
      const start = startCtrl.value as string | null;
      const duration = durationCtrl.value as number | null;
      if (start && duration && duration > 0) {
        this._updatingDates = true;
        const d = new Date(start);
        d.setMonth(d.getMonth() + duration);
        endCtrl.setValue(d.toISOString().split("T")[0], { emitEvent: false });
        this._updatingDates = false;
      }
    };

    // endDate → durationMonths (in whole months, rounded)
    const recomputeDuration = () => {
      if (this._updatingDates) return;
      const start = startCtrl.value as string | null;
      const end = endCtrl.value as string | null;
      if (start && end) {
        const s = new Date(start);
        const e = new Date(end);
        if (e > s) {
          this._updatingDates = true;
          const months =
            (e.getFullYear() - s.getFullYear()) * 12 +
            (e.getMonth() - s.getMonth());
          durationCtrl.setValue(months, { emitEvent: false });
          this._updatingDates = false;
        }
      }
    };

    startCtrl.valueChanges.subscribe(recomputeEnd);
    durationCtrl.valueChanges.subscribe(recomputeEnd);
    endCtrl.valueChanges.subscribe(recomputeDuration);
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: (lease) => {
        this.form.patchValue({
          signatureDate: lease.signatureDate,
          startDate: lease.startDate,
          endDate: lease.endDate,
          leaseType: lease.leaseType,
          durationMonths: lease.durationMonths,
          noticePeriodMonths: lease.noticePeriodMonths,
          monthlyRent: lease.monthlyRent,
          monthlyCharges: lease.monthlyCharges,
          chargesType: lease.chargesType,
          chargesDescription: lease.chargesDescription,
          registrationSpf: lease.registrationSpf,
          registrationRegion: lease.registrationRegion,
          registrationInventorySpf: lease.registrationInventorySpf,
          registrationInventoryRegion: lease.registrationInventoryRegion,
          depositAmount: lease.depositAmount,
          depositType: lease.depositType,
          depositReference: lease.depositReference,
          tenantInsuranceConfirmed: lease.tenantInsuranceConfirmed,
          tenantInsuranceReference: lease.tenantInsuranceReference,
          tenantInsuranceExpiry: lease.tenantInsuranceExpiry,
        });
        this.pendingTenants = (lease.tenants || []).map((t) => ({
          person: {
            id: t.personId,
            lastName: t.lastName,
            firstName: t.firstName,
          },
          role: t.role,
        }));
        this.unitId = lease.housingUnitId;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Lease not found.";
        this.isLoading = false;
      },
    });
  }

  get showInsuranceDetails(): boolean {
    return !!this.form.get("tenantInsuranceConfirmed")!.value;
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const base = {
      signatureDate: v.signatureDate,
      startDate: v.startDate,
      endDate: v.endDate,
      leaseType: v.leaseType,
      durationMonths: v.durationMonths,
      noticePeriodMonths: v.noticePeriodMonths,
      monthlyRent: v.monthlyRent,
      monthlyCharges: v.monthlyCharges ?? 0,
      chargesType: v.chargesType,
      chargesDescription: v.chargesDescription || null,
      registrationSpf: v.registrationSpf || null,
      registrationRegion: v.registrationRegion || null,
      registrationInventorySpf: v.registrationInventorySpf || null,
      registrationInventoryRegion: v.registrationInventoryRegion || null,
      depositAmount: v.depositAmount || null,
      depositType: v.depositType || null,
      depositReference: v.depositReference || null,
      tenantInsuranceConfirmed: v.tenantInsuranceConfirmed,
      tenantInsuranceReference: v.tenantInsuranceReference || null,
      tenantInsuranceExpiry: v.tenantInsuranceExpiry || null,
    };

    if (this.isEditMode && this.leaseId) {
      this.leaseService.update(this.leaseId, base).subscribe({
        next: () => this.router.navigate(["/leases", this.leaseId]),
        error: (err) => {
          this.errorMessage = err.error?.message || "Save failed.";
        },
      });
    } else {
      const createReq = {
        ...base,
        housingUnitId: this.unitId!,
        tenants: this.pendingTenants.map((t) => ({
          personId: t.person.id,
          role: t.role as TenantRole,
        })),
      };
      this.leaseService.create(createReq).subscribe({
        next: (lease) => this.router.navigate(["/leases", lease.id]),
        error: (err) => {
          this.errorMessage = err.error?.message || "Save failed.";
        },
      });
    }
  }

  // Tenant management helpers (unchanged)
  removePendingTenant(index: number): void {
    this.pendingTenants.splice(index, 1);
  }
}
