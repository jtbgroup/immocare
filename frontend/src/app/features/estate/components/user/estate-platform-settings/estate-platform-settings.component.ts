// features/settings/components/estate-platform-settings/estate-platform-settings.component.ts
// UC004_ESTATE_PLACEHOLDER Phase 5 — Per-estate platform settings page
// Route: /estates/:estateId/admin/platform-settings
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
import { ActiveEstateService } from "../../../../../core/services/active-estate.service";
import { DateFormatService } from "../../../../../core/services/date-format.service";
import { PlatformConfigService } from "../../../../../core/services/estate-config.service";
import { TagSubcategoryService } from "../../../../../core/services/tag-subcategory.service";
import {
  AddBoilerServiceValidityRuleRequest,
  AssetTypeMappingDTO,
  BoilerServiceValidityRuleDTO,
  CONFIG_KEYS,
  CONFIG_LABELS,
  DATE_FORMAT_PRESETS,
  EstateConfigDTO,
  ON_DUPLICATE_OPTIONS,
} from "../../../../../models/estate-config.model";
import { TagSubcategory } from "../../../../../models/transaction.model";

type Section = "general" | "alerts" | "import" | "boiler" | "assets";

const INTEGER_KEYS = new Set<string>([
  CONFIG_KEYS.PEB_EXPIRY_WARNING_DAYS,
  CONFIG_KEYS.BOILER_SERVICE_WARNING_DAYS,
  CONFIG_KEYS.LEASE_END_NOTICE_WARNING_DAYS,
  CONFIG_KEYS.INDEXATION_NOTICE_DAYS,
  CONFIG_KEYS.IMPORT_SUGGESTION_CONFIDENCE,
  CONFIG_KEYS.BOILER_ALERT_THRESHOLD_MONTHS,
  CONFIG_KEYS.CSV_SKIP_HEADER_ROWS,
  CONFIG_KEYS.CSV_COL_DATE,
  CONFIG_KEYS.CSV_COL_AMOUNT,
  CONFIG_KEYS.CSV_COL_DESCRIPTION,
  CONFIG_KEYS.CSV_COL_COUNTERPARTY,
  CONFIG_KEYS.CSV_COL_EXTERNAL_REF,
  CONFIG_KEYS.CSV_COL_BANK_ACCOUNT,
  CONFIG_KEYS.CSV_COL_VALUE_DATE,
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
  CONFIG_KEYS.CSV_DELIMITER,
  CONFIG_KEYS.CSV_DATE_FORMAT,
  CONFIG_KEYS.CSV_SKIP_HEADER_ROWS,
  CONFIG_KEYS.CSV_COL_DATE,
  CONFIG_KEYS.CSV_COL_AMOUNT,
  CONFIG_KEYS.CSV_COL_DESCRIPTION,
  CONFIG_KEYS.CSV_COL_COUNTERPARTY,
  CONFIG_KEYS.CSV_COL_EXTERNAL_REF,
  CONFIG_KEYS.CSV_COL_BANK_ACCOUNT,
  CONFIG_KEYS.CSV_COL_VALUE_DATE,
]);

const BOILER_KEYS = new Set<string>([
  CONFIG_KEYS.BOILER_ALERT_THRESHOLD_MONTHS,
]);

const ASSET_KEYS = new Set<string>([
  CONFIG_KEYS.ASSET_MAPPING_BOILER,
  CONFIG_KEYS.ASSET_MAPPING_FIRE_EXTINGUISHER,
  CONFIG_KEYS.ASSET_MAPPING_METER,
]);

@Component({
  selector: "app-estate-platform-settings",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: "./estate-platform-settings.component.html",
  styleUrls: ["./estate-platform-settings.component.scss"],
})
export class EstatePlatformSettingsComponent implements OnInit, OnDestroy {
  // ── Settings ──────────────────────────────────────────────────────────────
  configs: EstateConfigDTO[] = [];
  loading = false;
  saving = false;
  error: string | null = null;
  successMessage: string | null = null;
  form!: FormGroup;
  activeSection: Section = "general";

  // ── Boiler validity rules ─────────────────────────────────────────────────
  validityRules: BoilerServiceValidityRuleDTO[] = [];
  rulesLoading = false;
  showAddRuleForm = false;
  ruleForm!: FormGroup;
  ruleSaving = false;
  ruleError: string | null = null;

  // ── Asset type mappings ───────────────────────────────────────────────────
  assetMappings: AssetTypeMappingDTO[] = [];
  mappingsLoading = false;
  subcategories: TagSubcategory[] = [];
  mappingError: string | null = null;
  mappingSuccess: string | null = null;

  readonly CONFIG_LABELS = CONFIG_LABELS;
  readonly DATE_FORMAT_PRESETS = DATE_FORMAT_PRESETS;
  readonly ON_DUPLICATE_OPTIONS = ON_DUPLICATE_OPTIONS;
  readonly DATE_FORMAT_KEY = CONFIG_KEYS.APP_DATE_FORMAT;
  readonly ON_DUPLICATE_KEY = CONFIG_KEYS.IMPORT_ON_DUPLICATE;

  readonly sections: { id: Section; label: string; icon: string }[] = [
    { id: "general", label: "Général", icon: "🌐" },
    { id: "alerts", label: "Alertes", icon: "🔔" },
    { id: "import", label: "Import CSV", icon: "📥" },
    { id: "boiler", label: "Chaudières", icon: "🔥" },
    { id: "assets", label: "Liens actifs", icon: "🔗" },
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private configService: PlatformConfigService,
    private dateFormatService: DateFormatService,
    private subcategoryService: TagSubcategoryService,
    private activeEstateService: ActiveEstateService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
  ) {}

  get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  get canEdit(): boolean {
    return this.activeEstateService.canEdit();
  }

  ngOnInit(): void {
    const s = this.route.snapshot.queryParamMap.get("section") as Section;
    if (s && this.sections.find((sec) => sec.id === s)) this.activeSection = s;

    this.buildRuleForm();
    this.load();
    this.loadSubcategories();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setSection(section: Section): void {
    this.activeSection = section;
    this.router.navigate([], { queryParams: { section }, replaceUrl: true });

    if (section === "boiler") this.loadValidityRules();
    if (section === "assets") this.loadAssetMappings();
  }

  // ── Settings CRUD ────────────────────────────────────────────────────────

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
      if (INTEGER_KEYS.has(c.configKey)) {
        validators.push(Validators.pattern(/^-?[0-9]+$/));
      }
      group[c.configKey] = [c.configValue, validators];
    }
    this.form = this.fb.group(group);

    // Disable form for VIEWERs
    if (!this.canEdit) {
      this.form.disable();
    }
  }

  save(): void {
    if (this.form.invalid || !this.canEdit) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = null;
    this.successMessage = null;

    const updates = Object.entries(this.form.value).map(([key, value]) =>
      this.configService.updateOne(key, { configValue: String(value).trim() }),
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

  // ── Boiler validity rules ────────────────────────────────────────────────

  loadValidityRules(): void {
    this.rulesLoading = true;
    this.configService
      .getValidityRules()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rules) => {
          this.validityRules = rules;
          this.rulesLoading = false;
        },
        error: () => {
          this.rulesLoading = false;
        },
      });
  }

  buildRuleForm(): void {
    this.ruleForm = this.fb.group({
      validFrom: [null, Validators.required],
      validityDurationMonths: [24, [Validators.required, Validators.min(1)]],
      description: [""],
    });
  }

  openAddRuleForm(): void {
    this.showAddRuleForm = true;
    this.ruleError = null;
    this.ruleForm.reset({ validityDurationMonths: 24 });
  }

  cancelAddRule(): void {
    this.showAddRuleForm = false;
    this.ruleError = null;
  }

  saveRule(): void {
    if (this.ruleForm.invalid) {
      this.ruleForm.markAllAsTouched();
      return;
    }
    this.ruleSaving = true;
    this.ruleError = null;

    const req: AddBoilerServiceValidityRuleRequest = {
      validFrom: this.ruleForm.value.validFrom,
      validityDurationMonths: this.ruleForm.value.validityDurationMonths,
      description: this.ruleForm.value.description || null,
    };

    this.configService
      .addValidityRule(req)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.ruleSaving = false)),
      )
      .subscribe({
        next: () => {
          this.showAddRuleForm = false;
          this.loadValidityRules();
        },
        error: (err) => {
          this.ruleError =
            err?.error?.message ?? "Erreur lors de la sauvegarde.";
        },
      });
  }

  // ── Asset type mappings ───────────────────────────────────────────────────

  loadSubcategories(): void {
    this.subcategoryService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (subs) => (this.subcategories = subs),
      });
  }

  loadAssetMappings(): void {
    this.mappingsLoading = true;
    this.configService
      .getAssetTypeMappings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (mappings) => {
          this.assetMappings = mappings;
          this.mappingsLoading = false;
        },
        error: () => {
          this.mappingsLoading = false;
        },
      });
  }

  updateMapping(assetType: string, subcategoryId: number | null): void {
    if (!this.canEdit) return;
    this.mappingError = null;
    this.mappingSuccess = null;

    this.configService
      .updateAssetTypeMapping(assetType, { subcategoryId })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.mappingSuccess = "Mapping mis à jour.";
          setTimeout(() => (this.mappingSuccess = null), 2000);
          this.loadAssetMappings();
        },
        error: (err) => {
          this.mappingError =
            err?.error?.message ?? "Erreur lors de la mise à jour.";
        },
      });
  }

  assetTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      BOILER: "🔥 Chaudière",
      FIRE_EXTINGUISHER: "🧯 Extincteur",
      METER: "⚡ Compteur",
    };
    return labels[type] ?? type;
  }

  // ── Form helpers ────────────────────────────────────────────────────────

  configsFor(keys: Set<string>): EstateConfigDTO[] {
    return this.configs.filter((c) => keys.has(c.configKey));
  }

  get generalConfigs(): EstateConfigDTO[] {
    return this.configsFor(GENERAL_KEYS);
  }
  get alertConfigs(): EstateConfigDTO[] {
    return this.configsFor(ALERT_KEYS);
  }
  get importConfigs(): EstateConfigDTO[] {
    return this.configsFor(IMPORT_KEYS);
  }
  get boilerConfigs(): EstateConfigDTO[] {
    return this.configsFor(BOILER_KEYS);
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
    const selected = (event.target as HTMLSelectElement).value;
    if (selected !== "__custom__") this.form.get(key)?.setValue(selected);
  }

  fieldError(key: string): string | null {
    const ctrl = this.form?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Ce champ est obligatoire.";
    if (ctrl.hasError("pattern")) return "Valeur invalide.";
    return "Valeur invalide.";
  }

  ruleFieldError(key: string): string | null {
    const ctrl = this.ruleForm?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Champ requis.";
    if (ctrl.hasError("min")) return "Doit être ≥ 1.";
    return "Valeur invalide.";
  }
}
