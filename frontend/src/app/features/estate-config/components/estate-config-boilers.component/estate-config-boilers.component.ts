import { CommonModule } from "@angular/common";
import { Component, inject, Input } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { finalize, forkJoin, Subject, takeUntil } from "rxjs";
import { EstateConfigService } from "../../../../core/services/estate-config.service"; // Ajuste le chemin
import {
  AddBoilerServiceValidityRuleRequest,
  BoilerServiceValidityRuleDTO,
  EstateConfigDTO,
} from "../../../../models/estate-config.model";
import {
  BOILER_KEYS,
  CONFIG_LABELS_I18N_KEYS,
  getFieldError,
  toFormKey,
} from "../../helpers/estate-config.helpers";

@Component({
  selector: "app-estate-config-boilers",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  templateUrl: "./estate-config-boilers.component.html",
})
export class EstateSettingsBoilersComponent {
  validityRules: BoilerServiceValidityRuleDTO[] = [];
  rulesLoading = false;
  showAddRuleForm = false;
  ruleForm!: FormGroup;
  ruleSaving = false;
  ruleError: string | null = null;

  private configService = inject(EstateConfigService);
  private destroy$ = new Subject<void>();

  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) configs: EstateConfigDTO[] = [];
  @Input() canEdit = false;

  saving = false;

  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly BOILER_KEYS = BOILER_KEYS;
  readonly toFormKey = toFormKey;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.buildRuleForm();
    this.loadValidityRules();
  }

  fieldError(key: string): string | null {
    return getFieldError(key, this.form);
  }

  save(): void {
    // 1. On isole uniquement les contrôles de CET onglet via le Set de constantes
    const currentTabControls = Array.from(BOILER_KEYS).map((key) =>
      this.form.get(toFormKey(key)),
    );

    // 2. On vérifie si l'un d'entre eux est invalide
    const isCurrentTabInvalid = currentTabControls.some(
      (control) => control?.invalid,
    );

    // 3. Validation ciblée : on ne bloque que si un champ de CET onglet pose problème
    if (isCurrentTabInvalid || !this.canEdit) {
      currentTabControls.forEach((control) => control?.markAsTouched());
      return;
    }

    this.saving = true;

    // 4. Préparation des requêtes unitaires directement basées sur les clés du Set
    const updates = Array.from(BOILER_KEYS).map((key) => {
      const value = this.form.get(toFormKey(key))?.value;
      return this.configService.updateOne(key, {
        configValue:
          value !== null && value !== undefined ? String(value).trim() : "",
      });
    });

    if (updates.length > 0) {
      forkJoin(updates).subscribe({
        next: () => {
          this.saving = false;
        },
        error: (err) => {
          this.saving = false;
          console.error("Erreur lors de la sauvegarde générale :", err);
        },
      });
    } else {
      this.saving = false;
    }
  }

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

  ruleFieldError(key: string): string | null {
    const ctrl = this.ruleForm?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Champ requis.";
    if (ctrl.hasError("min")) return "Doit être ≥ 1.";
    return "Valeur invalide.";
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
}
