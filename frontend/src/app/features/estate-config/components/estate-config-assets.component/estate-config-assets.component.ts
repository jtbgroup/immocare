import { CommonModule } from "@angular/common";
import {
  Component,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { forkJoin, Subject, takeUntil } from "rxjs";
import { EstateConfigService } from "../../../../core/services/estate-config.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import { EstateConfigDTO } from "../../../../models/estate-config.model";
import { TagSubcategory } from "../../../../models/transaction.model";
import {
  ASSETS_KEYS,
  CONFIG_LABELS_I18N_KEYS,
  toFormKey,
} from "../../helpers/estate-config.helpers";

@Component({
  selector: "app-estate-config-assets",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  templateUrl: "./estate-config-assets.component.html",
})
export class EstateSettingsAssetsComponent
  implements OnInit, OnDestroy, OnChanges
{
  private configService = inject(EstateConfigService);
  private tagSubcategoryService = inject(TagSubcategoryService);

  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) configs: EstateConfigDTO[] = [];
  @Input() canEdit = false;

  mappingsLoading = false;
  subcategories: TagSubcategory[] = [];
  saving = false;

  readonly ASSETS_KEYS = ASSETS_KEYS;
  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly toFormKey = toFormKey;

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.loadSubcategories();
    this.sanitizeInitialValues();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.form && changes["canEdit"]) {
      this.toggleFormState();
    }
    // Si le formulaire ou les configurations changent, on applique le nettoyage des types
    if (changes["form"] || changes["configs"]) {
      this.sanitizeInitialValues();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Aligne les types de la DB avec les types attendus par le [ngValue] du template.
   * Transforme les chaînes "-1" ou vides en vrai nombre -1.
   */
  private sanitizeInitialValues(): void {
    if (!this.form) return;

    this.ASSETS_KEYS.forEach((key) => {
      const control = this.form.get(toFormKey(key));
      if (control) {
        const rawValue = control.value;

        // Si la valeur reçue est vide, nulle, ou vaut la chaîne "-1"
        if (
          rawValue === null ||
          rawValue === undefined ||
          String(rawValue).trim() === "" ||
          rawValue === "-1"
        ) {
          control.setValue(-1, { emitEvent: false }); // On force le nombre -1 (NONE)
        }
        // Si c'est un ID sous forme de chaîne de caractères (ex: "13")
        else if (typeof rawValue === "string") {
          const parsed = parseInt(rawValue, 10);
          if (!isNaN(parsed)) {
            control.setValue(parsed, { emitEvent: false }); // On convertit en vrai number
          }
        }
      }
    });
  }

  private toggleFormState(): void {
    this.ASSETS_KEYS.forEach((key) => {
      const control = this.form.get(toFormKey(key));
      if (control) {
        if (this.canEdit) {
          control.enable({ emitEvent: false });
        } else {
          control.disable({ emitEvent: false });
        }
      }
    });
  }

  loadSubcategories(): void {
    this.tagSubcategoryService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (subs) => (this.subcategories = subs),
      });
  }

  subcategoryName(catId: any): string {
    if (catId == null || catId === "" || catId === -1) return "--";
    const parsedId = parseInt(catId.toString(), 10);
    if (isNaN(parsedId)) return "--";

    const sub = this.subcategories.find((c) => c.id === parsedId);
    return sub ? `${sub.categoryName} / ${sub.name}` : "--";
  }

  save(): void {
    // 1. On isole uniquement les contrôles de CET onglet
    const currentTabControls = Array.from(ASSETS_KEYS).map((key) =>
      this.form.get(toFormKey(key)),
    );

    // 2. Validation tolérante : on ne bloque que si un champ pose problème,
    // en ignorant volontairement les erreurs de validation si le champ est sur NONE (-1)
    const isCurrentTabInvalid = currentTabControls.some((control) => {
      if (!control) return false;

      // Si le contrôle est invalide mais qu'il est configuré sur NONE (-1), on l'accepte
      if (control.invalid && (control.value === -1 || control.value === "")) {
        return false;
      }
      return control.invalid;
    });

    if (isCurrentTabInvalid || !this.canEdit) {
      currentTabControls.forEach((control) => control?.markAsTouched());
      return;
    }

    this.saving = true;

    // 4. Préparation et nettoyage des données pour l'API
    const updates = Array.from(ASSETS_KEYS).map((key) => {
      const value = this.form.get(toFormKey(key))?.value;

      // Si la valeur est un ID valide (différent de -1 et non nul), on l'envoie en string.
      // Sinon, on envoie explicitement la string "-1" attendue par votre DB pour le NONE.
      const cleanValue =
        value !== null &&
        value !== undefined &&
        value !== -1 &&
        String(value).trim() !== ""
          ? String(value).trim()
          : "-1";

      return this.configService.updateOne(key, {
        configValue: cleanValue,
      });
    });

    // 5. Envoi des requêtes via forkJoin
    if (updates.length > 0) {
      forkJoin(updates).subscribe({
        next: () => {
          this.saving = false;
        },
        error: (err) => {
          this.saving = false;
          console.error(
            "Erreur lors de la sauvegarde générale des assets :",
            err,
          );
        },
      });
    } else {
      this.saving = false;
    }
  }
}
