// features/lease/lease-form/lease-form.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { LeaseService } from '../../../../core/services/lease.service';
import {
  AddTenantRequest, DEFAULT_NOTICE_MONTHS, LEASE_DURATION_MONTHS,
  LEASE_TYPE_LABELS, LeaseType, TenantRole,
} from '../../../../models/lease.model';
import { PersonPickerComponent } from '../../../../shared/components/person-picker/person-picker.component';

@Component({
  selector: 'app-lease-form',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, PersonPickerComponent],
  templateUrl: './lease-form.component.html',
  styleUrls: ['./lease-form.component.scss'],
})
export class LeaseFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  leaseId?: number;
  unitId?: number;
  isLoading = false;
  isSaving = false;
  errorMessage = '';
  computedEndDate = '';

  readonly LEASE_TYPES = Object.keys(LEASE_TYPE_LABELS) as LeaseType[];
  readonly LEASE_TYPE_LABELS = LEASE_TYPE_LABELS;
  pendingTenants: Array<{ person: any; role: TenantRole }> = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private leaseService: LeaseService,
  ) {}

  ngOnInit(): void {
    this.unitId = +(this.route.snapshot.paramMap.get('unitId') || 0);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) { this.isEditMode = true; this.leaseId = +id; }
    this.buildForm();
    if (this.isEditMode) this.loadLease(this.leaseId!);
  }

  buildForm(): void {
    this.form = this.fb.group({
      signatureDate:          ['', Validators.required],
      startDate:              ['', Validators.required],
      leaseType:              ['MAIN_RESIDENCE_9Y', Validators.required],
      durationMonths:         [108, [Validators.required, Validators.min(1)]],
      noticePeriodMonths:     [3,   [Validators.required, Validators.min(1)]],
      monthlyRent:            ['', [Validators.required, Validators.min(0.01)]],
      monthlyCharges:         [0],
      chargesType:            ['FORFAIT'],
      chargesDescription:     [''],
      registrationSpf:        [''],
      registrationRegion:     [''],
      depositAmount:          [null],
      depositType:            [null],
      depositReference:       [''],
      tenantInsuranceConfirmed: [false],
      tenantInsuranceReference: [''],
      tenantInsuranceExpiry:    [null],
    });

    this.form.get('startDate')!.valueChanges.subscribe(() => this.computeEndDate());
    this.form.get('durationMonths')!.valueChanges.subscribe(() => this.computeEndDate());
    this.form.get('leaseType')!.valueChanges.subscribe((type) => {
      if (type) {
        this.form.patchValue({
          noticePeriodMonths: DEFAULT_NOTICE_MONTHS[type as LeaseType] || 3,
          durationMonths:     LEASE_DURATION_MONTHS[type as LeaseType] || 108,
        }, { emitEvent: false });
      }
    });
  }

  computeEndDate(): void {
    const start    = this.form.get('startDate')!.value;
    const duration = this.form.get('durationMonths')!.value;
    if (start && duration > 0) {
      const d = new Date(start);
      d.setMonth(d.getMonth() + duration);
      this.computedEndDate = d.toISOString().split('T')[0];
    }
  }

  loadLease(id: number): void {
    this.isLoading = true;
    this.leaseService.getById(id).subscribe({
      next: (lease) => {
        this.form.patchValue({
          signatureDate:            lease.signatureDate,
          startDate:                lease.startDate,
          leaseType:                lease.leaseType,
          durationMonths:           lease.durationMonths,
          noticePeriodMonths:       lease.noticePeriodMonths,
          monthlyRent:              lease.monthlyRent,
          monthlyCharges:           lease.monthlyCharges,
          chargesType:              lease.chargesType,
          chargesDescription:       lease.chargesDescription,
          registrationSpf:          lease.registrationSpf,
          registrationRegion:       lease.registrationRegion,
          depositAmount:            lease.depositAmount,
          depositType:              lease.depositType,
          depositReference:         lease.depositReference,
          tenantInsuranceConfirmed: lease.tenantInsuranceConfirmed,
          tenantInsuranceReference: lease.tenantInsuranceReference,
          tenantInsuranceExpiry:    lease.tenantInsuranceExpiry,
        });
        this.pendingTenants = (lease.tenants || []).map(t => ({
          person: { id: t.personId, lastName: t.lastName, firstName: t.firstName },
          role: t.role,
        }));
        this.unitId = lease.housingUnitId;
        this.computeEndDate();
        this.isLoading = false;
      },
      error: () => { this.errorMessage = 'Lease not found.'; this.isLoading = false; },
    });
  }

  get showInsuranceDetails(): boolean { return !!this.form.get('tenantInsuranceConfirmed')!.value; }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  addTenant(person: any, role: TenantRole = 'PRIMARY'): void {
    if (!this.pendingTenants.find(t => t.person.id === person.id))
      this.pendingTenants.push({ person, role });
  }
  removeTenant(personId: number): void {
    this.pendingTenants = this.pendingTenants.filter(t => t.person.id !== personId);
  }
  setRole(personId: number, role: TenantRole): void {
    const t = this.pendingTenants.find(t => t.person.id === personId);
    if (t) t.role = role;
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    this.isSaving = true;
    this.errorMessage = '';

    if (this.isEditMode) {
      this.leaseService.update(this.leaseId!, { ...v }).subscribe({
        next: () => this.router.navigate(['/leases', this.leaseId]),
        error: (err) => { this.errorMessage = err.error?.message || 'Update failed.'; this.isSaving = false; },
      });
    } else {
      const tenants: AddTenantRequest[] = this.pendingTenants.map(t => ({ personId: t.person.id, role: t.role }));
      this.leaseService.create({ ...v, housingUnitId: this.unitId!, tenants }).subscribe({
        next: (l) => this.router.navigate(['/leases', l.id]),
        error: (err) => { this.errorMessage = err.error?.message || 'Create failed.'; this.isSaving = false; },
      });
    }
  }
}
