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
import { PlatformConfigService } from "../../../../core/services/platform-config.service";
import {
  CONFIG_KEYS,
  CONFIG_LABELS,
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

  private destroy$ = new Subject<void>();

  constructor(
    private configService: PlatformConfigService,
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

  fieldError(key: string): string | null {
    const ctrl = this.form?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "This field is required.";
    if (ctrl.hasError("pattern")) return "Must be a positive integer.";
    return "Invalid value.";
  }
}
