import { CommonModule } from "@angular/common";
import { Component, inject, Input } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { EstateConfigService } from "../../../../core/services/estate-config.service";
import { CONFIG_KEYS } from "../../../../models/estate-config.model";
import { ConfigRowComponent } from "../../../../shared/components/config-row.component/config-row.component";
import { ConfigSectionWrapperComponent } from "../../../../shared/components/config-section-wrapper.component";
import {
  createConfigSaveRequest,
  getKeyI18NUnit,
  getKeyI18NValue,
  getKeyValueType,
  getMinIfExists,
  toFormKey,
} from "../../helpers/estate-config.helpers";

@Component({
  selector: "app-estate-config-alerts",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    ConfigRowComponent,
    ConfigSectionWrapperComponent,
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
}
