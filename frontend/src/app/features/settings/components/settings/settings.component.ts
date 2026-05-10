// features/settings/components/settings/settings.component.ts — UC004_ESTATE_PLACEHOLDER Phase 6
// ActiveEstateService injected; save() blocked for VIEWERs.
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Subject, forkJoin } from "rxjs";
import { finalize, takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { DateFormatService } from "../../../../core/services/date-format.service";
import { PlatformConfigService as EstateConfigService } from "../../../../core/services/estate-config.service";
import {
  CONFIG_KEYS,
  CONFIG_LABELS,
  DATE_FORMAT_PRESETS,
  EstateConfigDTO,
  ON_DUPLICATE_OPTIONS,
} from "../../../../models/estate-config.model";

type Section = "general" | "alerts" | "import";

const INTEGER_KEYS = new Set<string>([
  CONFIG_KEYS.PEB_EXPIRY_WARNING_DAYS,
  CONFIG_KEYS.BOILER_SERVICE_WARNING_DAYS,
  CONFIG_KEYS.LEASE_END_NOTICE_WARNING_DAYS,
  CONFIG_KEYS.INDEXATION_NOTICE_DAYS,
  CONFIG_KEYS.IMPORT_SUGGESTION_CONFIDENCE,
]);

const GENERAL_KEYS = new Set<string>([
  CONFIG_KEYS.APP_NAME,
  CONFIG_KEYS.DEFAULT_COUNTRY,
  CONFIG_KEYS.APP_DATE_FORMAT,
]);

const ALERT_KEYS = new Set<string>([
  CONFIG_KEYS.PEB_EXPIRY_WARNING_DAYS,
  CONFIG_KEYS.BOILER_SERVICE_WARNING_DAYS,
  CONFIG_KEYS.LEASE_END_NOTICE_WARNING_DAYS,
  CONFIG_KEYS.INDEXATION_NOTICE_DAYS,
]);

const IMPORT_KEYS = new Set<string>([
  CONFIG_KEYS.IMPORT_ON_DUPLICATE,
  CONFIG_KEYS.IMPORT_SUGGESTION_CONFIDENCE,
]);

@Component({
  selector: "app-settings",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: "./settings.component.html",
  styleUrls: ["./settings.component.scss"],
})
export class SettingsComponent implements OnInit, OnDestroy {
  configs: EstateConfigDTO[] = [];
  loading = false;
  saving = false;
  error: string | null = null;
  successMessage: string | null = null;
  form!: FormGroup;
  activeSection: Section = "general";

  readonly CONFIG_LABELS = CONFIG_LABELS;
  readonly DATE_FORMAT_PRESETS = DATE_FORMAT_PRESETS;
  readonly ON_DUPLICATE_OPTIONS = ON_DUPLICATE_OPTIONS;
  readonly DATE_FORMAT_KEY = CONFIG_KEYS.APP_DATE_FORMAT;
  readonly ON_DUPLICATE_KEY = CONFIG_KEYS.IMPORT_ON_DUPLICATE;

  private destroy$ = new Subject<void>();

  constructor(
    private configService: EstateConfigService,
    private dateFormatService: DateFormatService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    const s = this.route.snapshot.queryParamMap.get("section") as Section;
    if (s && ["general", "alerts", "import"].includes(s))
      this.activeSection = s;
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setSection(section: Section): void {
    this.activeSection = section;
    this.router.navigate([], { queryParams: { section }, replaceUrl: true });
  }

  load(): void {
    this.loading = true;
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
          this.error = "Impossible de charger les paramètres.";
          this.loading = false;
        },
      });
  }

  buildForm(configs: EstateConfigDTO[]): void {
    const group: Record<string, unknown[]> = {};
    for (const c of configs) {
      const validators = [Validators.required];
      if (INTEGER_KEYS.has(c.configKey))
        validators.push(Validators.pattern(/^[1-9][0-9]*$/));
      group[c.configKey] = [c.configValue, validators];
    }
    this.form = this.fb.group(group);

    // Disable form for VIEWERs (BR-UC004_ESTATE_PLACEHOLDER-08)
    if (!this.activeEstateService.canEdit()) {
      this.form.disable();
    }
  }

  get generalConfigs(): EstateConfigDTO[] {
    return this.configs.filter((c) => GENERAL_KEYS.has(c.configKey));
  }
  get alertConfigs(): EstateConfigDTO[] {
    return this.configs.filter((c) => ALERT_KEYS.has(c.configKey));
  }
  get importConfigs(): EstateConfigDTO[] {
    return this.configs.filter((c) => IMPORT_KEYS.has(c.configKey));
  }

  save(): void {
    // Block save for VIEWERs (BR-UC004_ESTATE_PLACEHOLDER-08)
    if (!this.activeEstateService.canEdit()) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = null;
    this.successMessage = null;

    const updates = Object.entries(this.form.value).map(
      ([configKey, configValue]) =>
        this.configService.updateOne(configKey, {
          configValue: String(configValue).trim(),
        }),
    );

    forkJoin(updates)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.saving = false)),
      )
      .subscribe({
        next: () => {
          this.successMessage = "Paramètres enregistrés.";
          setTimeout(() => (this.successMessage = null), 3000);
          this.dateFormatService.reload();
          this.load();
        },
        error: (err) => {
          this.error =
            err?.error?.message ?? "Erreur lors de l'enregistrement.";
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
  isSelectKey(key: string): boolean {
    return key === CONFIG_KEYS.IMPORT_ON_DUPLICATE;
  }

  presetOrCustom(key: string): string {
    const val = this.form?.get(key)?.value as string;
    return this.DATE_FORMAT_PRESETS.some((p) => p.value === val)
      ? val
      : "__custom__";
  }

  onDateFormatSelectChange(key: string, event: Event): void {
    if (!this.activeEstateService.canEdit()) return;
    const selected = (event.target as HTMLSelectElement).value;
    if (selected !== "__custom__") this.form.get(key)?.setValue(selected);
  }

  fieldError(key: string): string | null {
    const ctrl = this.form?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Ce champ est obligatoire.";
    if (ctrl.hasError("pattern")) return "Doit être un entier positif.";
    return "Valeur invalide.";
  }
}
