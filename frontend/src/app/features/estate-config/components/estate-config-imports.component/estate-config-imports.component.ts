import { CommonModule } from "@angular/common";
import { Component, Input, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { forkJoin } from "rxjs";
import { EstateConfigService } from "../../../../core/services/estate-config.service"; // Ajuste le chemin
import { EstateConfigDTO } from "../../../../models/estate-config.model";
import {
  CONFIG_LABELS_I18N_KEYS,
  CONFIG_UNITS_I18N_KEYS,
  IMPORTS_KEYS,
  getConfigDescription,
  getFieldError,
  isIntegerKey,
  toFormKey,
} from "../../helpers/estate-config.helpers";

@Component({
  selector: "app-estate-config-imports",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  templateUrl: "./estate-config-imports.component.html",
})
export class EstateSettingsImportsComponent {
  private configService = inject(EstateConfigService);

  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) configs: EstateConfigDTO[] = [];
  @Input() canEdit = false;

  saving = false;

  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly CONFIG_UNITS_I18N_KEYS = CONFIG_UNITS_I18N_KEYS;
  readonly IMPORTS_KEYS = IMPORTS_KEYS;
  readonly toFormKey = toFormKey;
  readonly isIntegerKey = isIntegerKey;

  fieldError(key: string): string | null {
    return getFieldError(key, this.form);
  }

  configDescription(key: string): string | null {
    return getConfigDescription(key, this.configs);
  }

  save(): void {
    // 1. On isole uniquement les contrôles de CET onglet
    const currentTabControls = Array.from(IMPORTS_KEYS).map((key) =>
      this.form.get(toFormKey(key)),
    );

    // 2. On vérifie si l'un d'entre eux est invalide
    const isCurrentTabInvalid = currentTabControls.some(
      (control) => control?.invalid,
    );

    if (isCurrentTabInvalid || !this.canEdit) {
      currentTabControls.forEach((control) => control?.markAsTouched());
      return;
    }

    // 3. Si cet onglet est propre, on sauvegarde !
    this.saving = true;

    const updates = Array.from(IMPORTS_KEYS).map((key) => {
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
}
