// features/settings/components/settings/settings.component.ts — UC012
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

import { DateFormatService } from "../../../../core/services/date-format.service";
import { PlatformConfigService } from "../../../../core/services/platform-config.service";
import {
  CONFIG_KEYS,
  CONFIG_LABELS,
  DATE_FORMAT_PRESETS,
  PlatformConfigDTO,
} from "../../../../models/platform-config.model";

/** Keys that must be positive integers */
const INTEGER_KEYS = new Set<string>([
  CONFIG_KEYS.PEB_EXPIRY_WARNING_DAYS,
  CONFIG_KEYS.BOILER_SERVICE_WARNING_DAYS,
  CONFIG_KEYS.LEASE_END_NOTICE_WARNING_DAYS,
  CONFIG_KEYS.INDEXATION_NOTICE_DAYS,
]);

@Component({
  selector: "app-settings",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: "./settings.component.html",
  styleUrls: ["./settings.component.scss"],
})
export class SettingsComponent implements OnInit, OnDestroy {
  configs: PlatformConfigDTO[] = [];
  loading = false;
  saving = false;
  error: string | null = null;
  successMessage: string | null = null;
  form!: FormGroup;

  readonly CONFIG_LABELS = CONFIG_LABELS;
  readonly INTEGER_KEYS = INTEGER_KEYS;
  readonly DATE_FORMAT_PRESETS = DATE_FORMAT_PRESETS;
  readonly DATE_FORMAT_KEY = CONFIG_KEYS.APP_DATE_FORMAT;

  private destroy$ = new Subject<void>();

  constructor(
    private configService: PlatformConfigService,
    private dateFormatService: DateFormatService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.configService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (configs) => {
          this.configs = configs;
          this.buildForm(configs);
          this.loading = false;
        },
        error: () => {
          this.error = "Failed to load settings.";
          this.loading = false;
        },
      });
  }

  buildForm(configs: PlatformConfigDTO[]): void {
    const group: Record<string, unknown[]> = {};
    for (const c of configs) {
      const validators = [Validators.required];
      if (INTEGER_KEYS.has(c.configKey)) {
        validators.push(Validators.pattern(/^[1-9][0-9]*$/));
      }
      group[c.configKey] = [c.configValue, validators];
    }
    this.form = this.fb.group(group);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = null;
    this.successMessage = null;

    const entries = Object.entries(this.form.value).map(
      ([configKey, configValue]) => ({
        configKey,
        configValue: String(configValue).trim(),
      }),
    );

    this.configService
      .bulkUpdate({ entries })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.successMessage = "Settings saved successfully.";
          setTimeout(() => (this.successMessage = null), 3000);
          // Reload date format so AppDatePipe picks it up immediately
          this.dateFormatService.reload();
        },
        error: (err) => {
          this.saving = false;
          this.error = err?.error?.message ?? "Failed to save settings.";
        },
      });
  }

  labelFor(key: string): string {
    return CONFIG_LABELS[key] ?? key;
  }

  isIntegerKey(key: string): boolean {
    return INTEGER_KEYS.has(key);
  }

  isDateFormatKey(key: string): boolean {
    return key === CONFIG_KEYS.APP_DATE_FORMAT;
  }

  /** Returns the current form value if it matches a preset, otherwise '__custom__'. */
  presetOrCustom(key: string): string {
    const val = this.form?.get(key)?.value as string;
    return this.DATE_FORMAT_PRESETS.some((p) => p.value === val)
      ? val
      : "__custom__";
  }

  /** Called when the <select> changes. Sets the form control value directly for presets,
   *  or clears it to let the user type a custom value. */
  onDateFormatSelectChange(key: string, event: Event): void {
    const selected = (event.target as HTMLSelectElement).value;
    if (selected !== "__custom__") {
      this.form.get(key)?.setValue(selected);
    }
    // For '__custom__', keep the current value so the text input shows the previous entry
  }

  fieldError(key: string): string | null {
    const ctrl = this.form?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "This field is required.";
    if (ctrl.hasError("pattern")) return "Must be a positive integer.";
    return "Invalid value.";
  }
}
