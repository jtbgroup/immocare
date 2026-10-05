import { CommonModule } from "@angular/common";
import { Component, inject, Input } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { PlatformConfigService } from "../../../../core/services/platform-config.service";
import { CONFIG_KEYS } from "../../../../models/platform-config.model";
import { ConfigRowComponent } from "../../../../shared/components/config-row.component/config-row.component";
import { ConfigSectionWrapperComponent } from "../../../../shared/components/config-section-wrapper.component";
import {
  createConfigSaveRequest,
  getKeyI18NUnit,
  getKeyI18NValue,
  getKeyValueType,
  getMinIfExists,
  toFormKey,
} from "../../helpers/platform-config.helpers";

@Component({
  selector: "app-platform-config-general",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    ConfigRowComponent,
    ConfigSectionWrapperComponent,
  ],
  templateUrl: "./platform-config-general.component.html",
  styleUrl: "./platform-config-general.component.scss",
})
export class PlatformConfigAGeneralComponent {
  private configService = inject(PlatformConfigService);

  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) configs: any[] = [];
  @Input() canEdit = false;

  readonly toFormKey = toFormKey;
  readonly getKeyI18NValue = getKeyI18NValue;
  readonly getKeyI18NUnit = getKeyI18NUnit;
  readonly getKeyValueType = getKeyValueType;
  readonly getMinIfExists = getMinIfExists;

  readonly GENERAL_KEYS = new Set<string>([
    CONFIG_KEYS.GENERAL_APP_NAME,
    CONFIG_KEYS.GENERAL_DEFAULT_COUNTRY,
  ]);

  getControl(key: string): FormControl {
    return this.form.get(this.toFormKey(key)) as FormControl;
  }

  saveTab = () =>
    createConfigSaveRequest(
      this.form,
      this.GENERAL_KEYS,
      this.configService,
      this.canEdit,
    );
}
