import { CommonModule } from "@angular/common";
import { Component, Input, inject } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { EstateConfigService } from "../../../../core/services/estate-config.service"; // Ajuste le chemin
import {
  CONFIG_KEYS,
  EstateConfigDTO,
} from "../../../../models/estate-config.model";
import {
  CONFIG_LABELS_I18N_KEYS,
  CONFIG_UNITS_I18N_KEYS,
  createConfigSaveRequest,
  getConfigDescription,
  getFieldError,
  getKeyI18NValue,
  getKeyValueType,
  getMinIfExists,
  toFormKey,
} from "../../helpers/estate-config.helpers";
import { ConfigRowComponent } from "../config-row.component/config-row.component";
import { EstateConfigSectionWrapperComponent } from "../estate-config-section-wrapper.component";

@Component({
  selector: "app-estate-config-imports",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    EstateConfigSectionWrapperComponent,
    ConfigRowComponent,
  ],
  templateUrl: "./estate-config-imports.component.html",
  styleUrl: "./estate-config-imports.component.scss",
})
export class EstateConfigImportsComponent {
  private configService = inject(EstateConfigService);

  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) configs: EstateConfigDTO[] = [];
  @Input() canEdit = false;

  saving = false;

  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly CONFIG_UNITS_I18N_KEYS = CONFIG_UNITS_I18N_KEYS;
  readonly toFormKey = toFormKey;
  readonly getKeyI18NValue = getKeyI18NValue;
  readonly getKeyValueType = getKeyValueType;
  readonly getMinIfExists = getMinIfExists;

  readonly IMPORTS_KEYS_COLS = new Set<string>([
    CONFIG_KEYS.IMPORTS_CSV_COL_DATE,
    CONFIG_KEYS.IMPORTS_CSV_COL_AMOUNT,
    CONFIG_KEYS.IMPORTS_CSV_COL_DESCRIPTION,
    CONFIG_KEYS.IMPORTS_CSV_COL_COUNTERPARTY,
    CONFIG_KEYS.IMPORTS_CSV_COL_EXTERNAL_REF,
    CONFIG_KEYS.IMPORTS_CSV_COL_BANK_ACCOUNT,
    CONFIG_KEYS.IMPORTS_CSV_COL_VALUE_DATE,
  ]);

  readonly IMPORTS_KEYS_FORMAT = new Set<string>([
    // CONFIG_KEYS.IMPORT_ON_DUPLICATE,
    CONFIG_KEYS.IMPORTS_CSV_DELIMITER,
    CONFIG_KEYS.IMPORTS_CSV_DATE_FORMAT,
    CONFIG_KEYS.IMPORTS_CSV_SKIP_HEADER_ROWS,
  ]);

  readonly IMPORTS_KEYS_ENGINE = new Set<string>([
    CONFIG_KEYS.IMPORTS_CSV_SUGGESTION_CONFIDENCE,
  ]);

  private readonly ALL_IMPORTS_KEYS = new Set<string>([
    ...this.IMPORTS_KEYS_COLS,
    ...this.IMPORTS_KEYS_FORMAT,
    ...this.IMPORTS_KEYS_ENGINE,
  ]);

  fieldError(key: string): string | null {
    return getFieldError(key, this.form);
  }

  getControl(key: string): FormControl {
    return this.form.get(this.toFormKey(key)) as FormControl;
  }

  configDescription(key: string): string | null {
    return getConfigDescription(key, this.configs);
  }

  saveTab = () =>
    createConfigSaveRequest(
      this.form,
      this.ALL_IMPORTS_KEYS,
      this.configService,
      this.canEdit,
    );
}
