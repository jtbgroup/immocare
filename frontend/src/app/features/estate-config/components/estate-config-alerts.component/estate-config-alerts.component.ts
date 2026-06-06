import { CommonModule } from "@angular/common";
import { Component, inject, Input } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { EstateConfigService } from "../../../../core/services/estate-config.service";
import { CONFIG_KEYS } from "../../../../models/estate-config.model";
import {
  createConfigSaveRequest,
  getKeyI18NUnit,
  getKeyI18NValue,
  getKeyValueType,
  getMinIfExists,
  toFormKey,
} from "../../helpers/estate-config.helpers";
import { ConfigRowComponent } from "../config-row.component/config-row.component";
import { EstateConfigSectionWrapperComponent } from "../estate-config-section-wrapper.component";

@Component({
  selector: "app-estate-config-alerts",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    ConfigRowComponent,
    EstateConfigSectionWrapperComponent,
  ],
  templateUrl: "./estate-config-alerts.component.html",
  styleUrl: "./estate-config-alerts.component.scss",
})
export class EstateConfigAlertsComponent {
  private configService = inject(EstateConfigService);

  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) configs: any[] = [];
  @Input() canEdit = false;

  // readonly CONFIG_KEYS = CONFIG_KEYS;
  readonly toFormKey = toFormKey;
  readonly getKeyI18NValue = getKeyI18NValue;
  readonly getKeyI18NUnit = getKeyI18NUnit;
  readonly getKeyValueType = getKeyValueType;
  readonly getMinIfExists = getMinIfExists;

  readonly ALERT_KEYS_CONTRACTS = new Set<string>([
    CONFIG_KEYS.CONTACTS_ALERT_PEB_EXPIRY_WARNING_MONTHS,
    CONFIG_KEYS.CONTACTS_ALERT_LEASE_END_NOTICE_DAYS,
    CONFIG_KEYS.CONTACTS_ALERT_LEASE_INDEXATION_DAYS,
  ]);

  readonly ALERT_KEYS_BOILER = new Set<string>([
    CONFIG_KEYS.BOILER_ALERT_THRESHOLD_MONTHS,
  ]);

  private readonly ALL_ALERT_KEYS = new Set<string>([
    ...this.ALERT_KEYS_CONTRACTS,
    ...this.ALERT_KEYS_BOILER,
  ]);

  getControl(alertKey: string): FormControl {
    return this.form.get(this.toFormKey(alertKey)) as FormControl;
  }

  saveTab = () =>
    createConfigSaveRequest(
      this.form,
      this.ALL_ALERT_KEYS,
      this.configService,
      this.canEdit,
    );
  // /**
  //  * Cette méthode prépare la requête de sauvegarde de l'onglet actuel.
  //  * Elle est passée sous forme d'Observable au wrapper qui se chargera de la souscription et du statut "saving".
  //  */
  // saveTab = (): Observable<any> => {
  //   const ALL_ALERT_KEYS = new Set<string>([
  //     ...this.ALERT_KEYS_CONTRACTS,
  //     ...this.ALERT_KEYS_BOILER,
  //   ]);

  //   const currentTabControls = Array.from(ALL_ALERT_KEYS).map((key) =>
  //     this.form.get(toFormKey(key)),
  //   );

  //   // Validation
  //   const isCurrentTabInvalid = currentTabControls.some((control) => {
  //     if (!control) return false;
  //     if (control.invalid && (control.value === -1 || control.value === "")) {
  //       return false;
  //     }
  //     return control.invalid;
  //   });

  //   if (isCurrentTabInvalid || !this.canEdit) {
  //     currentTabControls.forEach((control) => control?.markAsTouched());
  //     // On retourne un flux vide si le formulaire de l'onglet est invalide
  //     return of(null);
  //   }

  //   // Préparation des requêtes API
  //   const updates = Array.from(ALL_ALERT_KEYS).map((key) => {
  //     const value = this.form.get(toFormKey(key))?.value;
  //     const cleanValue =
  //       value !== null &&
  //       value !== undefined &&
  //       value !== -1 &&
  //       String(value).trim() !== ""
  //         ? String(value).trim()
  //         : "-1";

  //     return this.configService.updateOne(key, { configValue: cleanValue });
  //   });

  //   return updates.length > 0 ? forkJoin(updates) : of(null);
  // };
}
