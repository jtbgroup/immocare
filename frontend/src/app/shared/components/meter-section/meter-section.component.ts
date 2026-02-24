import {
  Component,
  Input,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { MeterService } from '../../../core/services/meter.service';
import {
  MeterDTO,
  MeterOwnerType,
  MeterType,
  AddMeterRequest,
  ReplaceMeterRequest,
  RemoveMeterRequest,
  ALL_METER_TYPES,
  ALL_REPLACEMENT_REASONS,
  METER_TYPE_LABELS,
  METER_TYPE_ICONS,
  REPLACEMENT_REASON_LABELS,
  meterDurationMonths,
} from '../../../models/meter.model';

type PanelState = 'idle' | 'add' | 'replace' | 'remove';

interface TypeBlock {
  type: MeterType;
  label: string;
  icon: string;
  meters: MeterDTO[];
}

@Component({
  selector: 'app-meter-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './meter-section.component.html',
  styleUrls: ['./meter-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeterSectionComponent implements OnInit {

  @Input() ownerType!: MeterOwnerType;
  @Input() ownerId!: number;

  // ─── State ──────────────────────────────────────────────────────────────
  blocks: TypeBlock[] = [];
  history: MeterDTO[] = [];
  showHistory = false;
  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  panelState: Record<MeterType, PanelState> = {
    WATER: 'idle', GAS: 'idle', ELECTRICITY: 'idle',
  };

  activeMeter: MeterDTO | null = null;
  activeMeterType: MeterType | null = null;

  // ─── Forms ──────────────────────────────────────────────────────────────
  addForm!: FormGroup;
  replaceForm!: FormGroup;
  removeForm!: FormGroup;

  // ─── Exposed constants for template ─────────────────────────────────────
  readonly ALL_METER_TYPES           = ALL_METER_TYPES;
  readonly ALL_REPLACEMENT_REASONS   = ALL_REPLACEMENT_REASONS;
  readonly METER_TYPE_LABELS         = METER_TYPE_LABELS;
  readonly METER_TYPE_ICONS          = METER_TYPE_ICONS;
  readonly REPLACEMENT_REASON_LABELS = REPLACEMENT_REASON_LABELS;
  readonly meterDurationMonths       = meterDurationMonths;

  constructor(
    private readonly meterService: MeterService,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadActiveMeters();
  }

  // ─── Initialization ──────────────────────────────────────────────────────

  private initForms(): void {
    this.addForm = this.fb.group({
      type:               ['', Validators.required],
      meterNumber:        ['', [Validators.required, Validators.maxLength(50)]],
      label:              ['', Validators.maxLength(100)],
      eanCode:            [''],
      installationNumber: [''],
      customerNumber:     [''],
      startDate:          [this.today(), Validators.required],
    });

    this.replaceForm = this.fb.group({
      newMeterNumber:        ['', [Validators.required, Validators.maxLength(50)]],
      newLabel:              ['', Validators.maxLength(100)],
      newEanCode:            [''],
      newInstallationNumber: [''],
      newCustomerNumber:     [''],
      newStartDate:          [this.today(), Validators.required],
      reason:                [null],
    });

    this.removeForm = this.fb.group({
      endDate: [this.today(), Validators.required],
    });
  }

  // ─── Data loading ────────────────────────────────────────────────────────

  private loadActiveMeters(): void {
    this.loading = true;
    const obs$ = this.ownerType === 'HOUSING_UNIT'
      ? this.meterService.getUnitActiveMeters(this.ownerId)
      : this.meterService.getBuildingActiveMeters(this.ownerId);

    obs$.subscribe({
      next: (meters) => {
        this.buildBlocks(meters);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Failed to load meters.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private buildBlocks(meters: MeterDTO[]): void {
    this.blocks = ALL_METER_TYPES.map((type) => ({
      type,
      label: METER_TYPE_LABELS[type],
      icon:  METER_TYPE_ICONS[type],
      meters: meters.filter((m) => m.type === type),
    }));
  }

  loadHistory(): void {
    const obs$ = this.ownerType === 'HOUSING_UNIT'
      ? this.meterService.getUnitMeterHistory(this.ownerId)
      : this.meterService.getBuildingMeterHistory(this.ownerId);

    obs$.subscribe({
      next: (h) => {
        this.history = h;
        this.showHistory = true;
        this.cdr.markForCheck();
      },
    });
  }

  toggleHistory(): void {
    if (this.showHistory) {
      this.showHistory = false;
      this.cdr.markForCheck();
    } else {
      this.loadHistory();
    }
  }

  get hasAnyMeter(): boolean {
    return this.blocks.some((b) => b.meters.length > 0);
  }

  // ─── Add Meter ───────────────────────────────────────────────────────────

  openAddForm(type: MeterType): void {
    this.closeAllPanels();
    this.panelState[type] = 'add';
    this.activeMeterType = type;
    this.addForm.reset({ type, startDate: this.today() });
    this.addForm.patchValue({ type });
  }

  isAddOpen(type: MeterType): boolean {
    return this.panelState[type] === 'add';
  }

  get addType(): MeterType | null {
    return this.addForm.value.type as MeterType ?? null;
  }

  needsEan(type: MeterType | null): boolean {
    return type === 'GAS' || type === 'ELECTRICITY';
  }

  needsInstallation(type: MeterType | null): boolean {
    return type === 'WATER';
  }

  needsCustomerNumber(type: MeterType | null): boolean {
    return type === 'WATER' && this.ownerType === 'BUILDING';
  }

  submitAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.value;
    const req: AddMeterRequest = {
      type:               v.type,
      meterNumber:        v.meterNumber,
      label:              v.label || null,
      eanCode:            v.eanCode || null,
      installationNumber: v.installationNumber || null,
      customerNumber:     v.customerNumber || null,
      startDate:          v.startDate,
    };

    const obs$ = this.ownerType === 'HOUSING_UNIT'
      ? this.meterService.addUnitMeter(this.ownerId, req)
      : this.meterService.addBuildingMeter(this.ownerId, req);

    obs$.subscribe({
      next: () => {
        this.showSuccess('Meter added successfully.');
        this.closeAllPanels();
        this.loadActiveMeters();
        if (this.showHistory) this.loadHistory();
      },
      error: (err) => this.showError(err),
    });
  }

  // ─── Replace Meter ───────────────────────────────────────────────────────

  openReplaceForm(meter: MeterDTO): void {
    this.closeAllPanels();
    this.activeMeter = meter;
    this.panelState[meter.type] = 'replace';
    this.replaceForm.reset({
      newStartDate: this.today(),
      newLabel: meter.label || '',   // pre-fill with current label
    });
  }

  isReplaceOpen(type: MeterType): boolean {
    return this.panelState[type] === 'replace';
  }

  get replaceType(): MeterType | null {
    return this.activeMeter?.type ?? null;
  }

  submitReplace(): void {
    if (!this.activeMeter || this.replaceForm.invalid) {
      this.replaceForm.markAllAsTouched();
      return;
    }
    const v = this.replaceForm.value;
    const req: ReplaceMeterRequest = {
      newMeterNumber:        v.newMeterNumber,
      newLabel:              v.newLabel || null,
      newEanCode:            v.newEanCode || null,
      newInstallationNumber: v.newInstallationNumber || null,
      newCustomerNumber:     v.newCustomerNumber || null,
      newStartDate:          v.newStartDate,
      reason:                v.reason || null,
    };

    const obs$ = this.ownerType === 'HOUSING_UNIT'
      ? this.meterService.replaceUnitMeter(this.ownerId, this.activeMeter.id, req)
      : this.meterService.replaceBuildingMeter(this.ownerId, this.activeMeter.id, req);

    obs$.subscribe({
      next: () => {
        this.showSuccess('Meter replaced successfully.');
        this.closeAllPanels();
        this.loadActiveMeters();
        // FIX: always reload history after replace and show it
        this.loadHistory();
      },
      error: (err) => this.showError(err),
    });
  }

  // ─── Remove Meter ────────────────────────────────────────────────────────

  openRemoveDialog(meter: MeterDTO): void {
    this.closeAllPanels();
    this.activeMeter = meter;
    this.panelState[meter.type] = 'remove';
    this.removeForm.reset({ endDate: this.today() });
  }

  isRemoveOpen(type: MeterType): boolean {
    return this.panelState[type] === 'remove';
  }

  submitRemove(): void {
    if (!this.activeMeter || this.removeForm.invalid) {
      this.removeForm.markAllAsTouched();
      return;
    }
    const req: RemoveMeterRequest = { endDate: this.removeForm.value.endDate };

    const obs$ = this.ownerType === 'HOUSING_UNIT'
      ? this.meterService.removeUnitMeter(this.ownerId, this.activeMeter.id, req)
      : this.meterService.removeBuildingMeter(this.ownerId, this.activeMeter.id, req);

    obs$.subscribe({
      next: () => {
        this.showSuccess('Meter removed successfully.');
        this.closeAllPanels();
        this.loadActiveMeters();
        if (this.showHistory) this.loadHistory();
      },
      error: (err) => this.showError(err),
    });
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  closeAllPanels(): void {
    this.panelState = { WATER: 'idle', GAS: 'idle', ELECTRICITY: 'idle' };
    this.activeMeter = null;
    this.activeMeterType = null;
    this.errorMessage = null;
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    this.errorMessage = null;
    setTimeout(() => { this.successMessage = null; this.cdr.markForCheck(); }, 3000);
    this.cdr.markForCheck();
  }

  private showError(err: any): void {
    this.errorMessage = err?.error?.message ?? 'An error occurred.';
    this.cdr.markForCheck();
  }

  fieldError(form: FormGroup, field: string): string {
    const ctrl = form.get(field);
    if (!ctrl || !ctrl.touched || !ctrl.errors) return '';
    if (ctrl.errors['required']) return 'This field is required.';
    if (ctrl.errors['maxlength']) return `Max ${ctrl.errors['maxlength'].requiredLength} characters.`;
    return '';
  }

  today(): string {
    return new Date().toISOString().split('T')[0];
  }
}
