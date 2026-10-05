// features/estate/components/user/estate-settings/estate-settings.component.ts
// UC004_ESTATE_PLACEHOLDER Phase 5 — Per-estate settings page
// Route: /estates/:estateId/settings
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslocoModule, TranslocoService } from "@ngneat/transloco";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../core/services/active-estate.service";
import { EstateConfigService } from "../../core/services/estate-config.service";
import {
  DATE_FORMAT_PRESETS,
  EstateConfigDTO,
} from "../../models/estate-config.model";
import { EstateConfigAlertsComponent } from "./components/estate-config-alerts.component/estate-config-alerts.component";
import { EstateConfigAssetsComponent } from "./components/estate-config-assets.component/estate-config-assets.component";
import { EstateConfigBankAccountsComponent } from "./components/estate-config-bank-accounts.component/estate-config-bank-accounts.component";
import { EstateConfigBoilersComponent } from "./components/estate-config-boilers.component/estate-config-boilers.component";
import { EstateConfigCategoriesComponent } from "./components/estate-config-categories.component/estate-config-categories.component";
import {
  CONFIG_LABELS_I18N_KEYS,
  CONFIG_UNITS_I18N_KEYS,
  getMinIfExists,
  KEY_VALUETYPE,
  SECTION_KEYS,
  SECTIONS,
  SECTIONS_ICONS,
} from "./helpers/estate-config.helpers";

@Component({
  selector: "app-estate-settings",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    EstateConfigBoilersComponent,
    EstateConfigAssetsComponent,
    EstateConfigAlertsComponent,
    EstateConfigBankAccountsComponent,
    EstateConfigCategoriesComponent,
  ],
  templateUrl: "./estate-config.component.html",
  styleUrls: ["./estate-config.component.scss"],
})
export class EstateSettingsComponent implements OnInit, OnDestroy {
  // ── Settings ──────────────────────────────────────────────────────────────
  configs: EstateConfigDTO[] = [];
  loading = false;
  saving = false;
  error: string | null = null;
  successMessage: string | null = null;
  form!: FormGroup;
  activeSection: string = Object.values(SECTION_KEYS)[0];

  readonly CONFIG_UNITS_I18N_KEYS = CONFIG_UNITS_I18N_KEYS;
  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly DATE_FORMAT_PRESETS = DATE_FORMAT_PRESETS;
  readonly SECTIONS = SECTIONS;
  readonly SECTION_KEYS = SECTION_KEYS;

  private destroy$ = new Subject<void>();

  constructor(
    private configService: EstateConfigService,
    private activeEstateService: ActiveEstateService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    public translocoService: TranslocoService,
  ) {}

  get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  get canEdit(): boolean {
    return this.activeEstateService.canEdit();
  }

  ngOnInit(): void {
    const s = this.route.snapshot.queryParamMap.get("section") as string;
    if (s && Object.values(SECTIONS).includes(s)) this.activeSection = s;

    // this.buildRuleForm();
    this.load();
    // this.loadSubcategories();

    // if (this.activeSection === "categories") this.loadCategories();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setSection(section: string): void {
    this.activeSection = section;
    this.router.navigate([], { queryParams: { section }, replaceUrl: true });
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

  // Dots are not allowed in FormGroup keys — encode/decode with __
  toFormKey(key: string): string {
    return key.replace(/\./g, "__");
  }
  private fromFormKey(key: string): string {
    return key.replace(/__/g, ".");
  }

  buildForm(configs: EstateConfigDTO[]): void {
    const group: Record<string, unknown[]> = {};

    for (const c of configs) {
      const validators = [];
      let initialValue: any = c.configValue;

      // 1. On récupère le type défini dans ton dictionnaire KEY_VALUETYPE
      const definedType = KEY_VALUETYPE[c.configKey];

      // 2. Traitement selon le type détecté
      if (definedType === "boolean") {
        // NORMALISATION : Si la DB renvoie "-1", "0", null, ou "false", la checkbox doit être décochée (false)
        if (
          initialValue === "-1" ||
          initialValue === -1 ||
          initialValue === "0" ||
          initialValue === 0 ||
          !initialValue ||
          initialValue === "false"
        ) {
          initialValue = false;
        } else {
          initialValue = true;
        }

        // Pas de Validators.required pour les booléens, sinon 'false' bloque le formulaire
      } else {
        // Pour les types 'string' et 'number', le champ est obligatoire
        validators.push(Validators.required);

        if (definedType === "number") {
          // Validation du format entier (positif ou négatif)
          validators.push(Validators.pattern(/^-?[0-9]+$/));

          // Sécurité : On force le type JavaScript à être un vrai nombre (ex: "-1" devient -1)
          if (typeof initialValue === "string") {
            initialValue = parseInt(initialValue, 10);
          }

          // On applique le minimum de 1 UNIQUEMENT si la clé n'est pas dans la liste des minimums
          validators.push(Validators.min(getMinIfExists(c.configKey) ?? 1));
        }
      }

      // 3. Ajout du contrôle configuré au groupe
      group[this.toFormKey(c.configKey)] = [initialValue, validators];
    }

    this.form = this.fb.group(group);
    if (!this.canEdit) this.form.disable();
  }

  // ── Form helpers ────────────────────────────────────────────────────────

  getSectionIcon(id: string): string {
    const section = SECTIONS_ICONS[id];
    return section ? section : "";
  }

  keyExists(key: string): boolean {
    const activeLang = this.translocoService.getActiveLang();
    const translations = this.translocoService.getTranslation(activeLang);

    if (!translations) return false;

    return translations.hasOwnProperty(key);
  }
}
