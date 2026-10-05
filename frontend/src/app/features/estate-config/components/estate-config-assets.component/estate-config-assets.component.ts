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
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { Subject, takeUntil } from "rxjs";
import { EstateConfigService } from "../../../../core/services/estate-config.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import {
  CONFIG_KEYS,
  EstateConfigDTO,
} from "../../../../models/estate-config.model";
import { TagSubcategory } from "../../../../models/transaction.model";
import { ConfigRowComponent } from "../../../../shared/components/config-row.component/config-row.component";
import { ConfigSectionWrapperComponent } from "../../../../shared/components/config-section-wrapper.component";
import {
  CONFIG_LABELS_I18N_KEYS,
  createConfigSaveRequest,
  getKeyI18NUnit,
  getKeyI18NValue,
  getKeyValueType,
  getMinIfExists,
  toFormKey,
} from "../../helpers/estate-config.helpers";

@Component({
  selector: "app-estate-config-assets",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    ConfigSectionWrapperComponent,
    ConfigRowComponent,
  ],
  templateUrl: "./estate-config-assets.component.html",
  styleUrl: "./estate-config-assets.component.scss",
})
export class EstateConfigAssetsComponent
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

  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly toFormKey = toFormKey;
  readonly getMinIfExists = getMinIfExists;
  readonly getKeyI18NUnit = getKeyI18NUnit;
  readonly getKeyI18NValue = getKeyI18NValue;
  readonly getKeyValueType = getKeyValueType;

  readonly ASSETS_MAPPING_KEYS = new Set<string>([
    CONFIG_KEYS.CONTACTS_ALERT_PEB_EXPIRY_WARNING_MONTHS,
    CONFIG_KEYS.CONTACTS_ALERT_LEASE_END_NOTICE_DAYS,
    CONFIG_KEYS.CONTACTS_ALERT_LEASE_INDEXATION_DAYS,
  ]);

  readonly ASSETS_GENERAL_KEYS = new Set<string>([
    CONFIG_KEYS.IMPORTS_CSV_SUGGESTION_CONFIDENCE,
  ]);

  private readonly ALL_ASSETS_KEYS = new Set<string>([
    ...this.ASSETS_GENERAL_KEYS,
    ...this.ASSETS_GENERAL_KEYS,
  ]);

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

    this.ASSETS_MAPPING_KEYS.forEach((key) => {
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
    this.ASSETS_MAPPING_KEYS.forEach((key) => {
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

  saveTab = () =>
    createConfigSaveRequest(
      this.form,
      this.ALL_ASSETS_KEYS,
      this.configService,
      this.canEdit,
    );

  getControl(alertKey: string): FormControl {
    return this.form.get(this.toFormKey(alertKey)) as FormControl;
  }
}
