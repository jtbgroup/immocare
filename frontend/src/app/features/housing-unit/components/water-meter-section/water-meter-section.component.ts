import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  AssignMeterRequest,
  RemoveMeterRequest,
  REPLACEMENT_REASON_LABELS,
  ReplaceMeterRequest,
  WaterMeterHistory,
} from '../../../../models/water-meter.model';
import { WaterMeterService } from '../../../../core/services/water-meter.service';

type MeterFormMode = 'assign' | 'replace' | 'remove' | null;

/**
 * Water Meter section component — integrates into HousingUnitDetailsComponent.
 * UC006 — US026, US027, US028, US029, US030.
 *
 * Usage:
 *   <app-water-meter-section [unitId]="unit.id"></app-water-meter-section>
 */
@Component({
  selector: 'app-water-meter-section',
  templateUrl: './water-meter-section.component.html',
  styleUrls: ['./water-meter-section.component.scss'],
})
export class WaterMeterSectionComponent implements OnChanges, OnDestroy {

  @Input() unitId!: number;

  activeMeter: WaterMeterHistory | null = null;
  history: WaterMeterHistory[] = [];
  showHistory = false;
  formMode: MeterFormMode = null;
  loading = false;
  saving = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  /** US027: same meter number warning */
  sameNumberWarning = false;

  meterForm!: FormGroup;
  removeForm!: FormGroup;
  readonly replacementReasonLabels = REPLACEMENT_REASON_LABELS;
  readonly replacementReasons = Object.keys(REPLACEMENT_REASON_LABELS);

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private meterService: WaterMeterService
  ) {
    this.buildForms();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['unitId'] && this.unitId) {
      this.loadActiveMeter();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // -------------------------------------------------------------------------
  // Load
  // -------------------------------------------------------------------------

  loadActiveMeter(): void {
    this.loading = true;
    this.meterService.getActiveMeter(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: meter => {
          this.activeMeter = meter;
          this.loading = false;
        },
        error: () => {
          this.activeMeter = null;
          this.loading = false;
        },
      });
  }

  loadHistory(): void {
    this.meterService.getMeterHistory(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: history => {
          this.history = history;
          this.showHistory = true;
        },
        error: () => this.showError('Failed to load meter history.'),
      });
  }

  toggleHistory(): void {
    if (this.showHistory) {
      this.showHistory = false;
    } else {
      this.loadHistory();
    }
  }

  // -------------------------------------------------------------------------
  // Form modes
  // -------------------------------------------------------------------------

  openAssignForm(): void {
    this.meterForm.reset({ installationDate: this.today() });
    this.formMode = 'assign';
    this.clearMessages();
  }

  openReplaceForm(): void {
    this.sameNumberWarning = false;
    this.meterForm.reset({ newInstallationDate: this.today() });
    this.formMode = 'replace';
    this.clearMessages();
  }

  openRemoveForm(): void {
    this.removeForm.reset({ removalDate: this.today() });
    this.formMode = 'remove';
    this.clearMessages();
  }

  cancel(): void {
    this.formMode = null;
    this.sameNumberWarning = false;
    this.clearMessages();
  }

  // -------------------------------------------------------------------------
  // Submit handlers
  // -------------------------------------------------------------------------

  /** US026 */
  submitAssign(): void {
    if (this.meterForm.invalid) return;
    const req: AssignMeterRequest = {
      meterNumber: this.meterForm.value.meterNumber.trim(),
      meterLocation: this.meterForm.value.meterLocation?.trim() || null,
      installationDate: this.meterForm.value.installationDate,
    };
    this.saving = true;
    this.meterService.assignMeter(this.unitId, req)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: meter => {
          this.activeMeter = meter;
          this.formMode = null;
          this.showHistory = false;
          this.saving = false;
          this.showSuccess('Water meter assigned successfully.');
        },
        error: err => {
          this.saving = false;
          this.showError(err?.error?.message || 'Failed to assign meter.');
        },
      });
  }

  /** US027 — check same-number warning before submitting */
  checkAndSubmitReplace(): void {
    if (this.meterForm.invalid) return;
    const newNumber = this.meterForm.value.newMeterNumber?.trim();
    if (
      !this.sameNumberWarning
      && this.activeMeter
      && newNumber === this.activeMeter.meterNumber
    ) {
      this.sameNumberWarning = true;
      return; // TS-UC006-07: show warning, let user confirm
    }
    this.submitReplace();
  }

  private submitReplace(): void {
    const req: ReplaceMeterRequest = {
      newMeterNumber: this.meterForm.value.newMeterNumber.trim(),
      newMeterLocation: this.meterForm.value.newMeterLocation?.trim() || null,
      newInstallationDate: this.meterForm.value.newInstallationDate,
      reason: this.meterForm.value.reason || undefined,
    };
    this.saving = true;
    this.meterService.replaceMeter(this.unitId, req)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: meter => {
          this.activeMeter = meter;
          this.formMode = null;
          this.showHistory = false;
          this.sameNumberWarning = false;
          this.saving = false;
          this.showSuccess('Water meter replaced successfully.');
        },
        error: err => {
          this.saving = false;
          this.showError(err?.error?.message || 'Failed to replace meter.');
        },
      });
  }

  /** US029 */
  submitRemove(): void {
    if (this.removeForm.invalid) return;
    const req: RemoveMeterRequest = {
      removalDate: this.removeForm.value.removalDate,
    };
    this.saving = true;
    this.meterService.removeMeter(this.unitId, req)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.activeMeter = null;
          this.formMode = null;
          this.showHistory = false;
          this.saving = false;
          this.showSuccess('Water meter removed successfully.');
        },
        error: err => {
          this.saving = false;
          this.showError(err?.error?.message || 'Failed to remove meter.');
        },
      });
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /** US030 — meter age warning */
  isMeterOld(meter: WaterMeterHistory): boolean {
    return meter.durationMonths > 120; // > 10 years
  }

  private buildForms(): void {
    this.meterForm = this.fb.group({
      // assign fields
      meterNumber: ['', [Validators.required, Validators.maxLength(50),
        Validators.pattern('^[A-Za-z0-9\\-_]+$')]],
      meterLocation: ['', Validators.maxLength(100)],
      installationDate: ['', Validators.required],
      // replace-only fields
      newMeterNumber: ['', [Validators.maxLength(50),
        Validators.pattern('^[A-Za-z0-9\\-_]+$')]],
      newMeterLocation: ['', Validators.maxLength(100)],
      newInstallationDate: [''],
      reason: [null],
    });

    this.removeForm = this.fb.group({
      removalDate: ['', Validators.required],
    });
  }

  get meterNumberCtrl(): AbstractControl { return this.meterForm.get('meterNumber')!; }
  get installationDateCtrl(): AbstractControl { return this.meterForm.get('installationDate')!; }
  get newMeterNumberCtrl(): AbstractControl { return this.meterForm.get('newMeterNumber')!; }
  get newInstallationDateCtrl(): AbstractControl { return this.meterForm.get('newInstallationDate')!; }
  get removalDateCtrl(): AbstractControl { return this.removeForm.get('removalDate')!; }

  private today(): string {
    return new Date().toISOString().split('T')[0];
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    this.errorMessage = null;
    setTimeout(() => this.successMessage = null, 4000);
  }

  private showError(msg: string): void {
    this.errorMessage = msg;
    this.successMessage = null;
  }

  private clearMessages(): void {
    this.errorMessage = null;
    this.successMessage = null;
  }
}
