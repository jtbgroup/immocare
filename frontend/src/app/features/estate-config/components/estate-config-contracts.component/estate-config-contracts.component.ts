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
  CONTRACTS_KEYS,
  getFieldError,
  toFormKey,
} from "../../helpers/estate-config.helpers";

@Component({
  selector: "app-estate-config-contracts",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  templateUrl: "./estate-config-contracts.component.html",
})
export class EstateSettingsContractsComponent {
  private configService = inject(EstateConfigService);

  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) configs: EstateConfigDTO[] = [];
  @Input() canEdit = false;

  saving = false;

  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly CONFIG_UNITS_I18N_KEYS = CONFIG_UNITS_I18N_KEYS;
  readonly CONTRACTS_KEYS = CONTRACTS_KEYS;
  readonly toFormKey = toFormKey;

  fieldError(key: string): string | null {
    return getFieldError(key, this.form);
  }

  save(): void {
    // 1. On isole uniquement les contrôles de CET onglet via le Set de constantes
    const currentTabControls = Array.from(CONTRACTS_KEYS).map((key) =>
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
    const updates = Array.from(CONTRACTS_KEYS).map((key) => {
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
