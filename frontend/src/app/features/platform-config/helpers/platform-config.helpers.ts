// src/app/models/estate-settings.constants.ts
import { FormGroup } from "@angular/forms";
import { forkJoin, Observable, of } from "rxjs";
import { PlatformConfigService } from "../../../core/services/platform-config.service";
import { CONFIG_KEYS } from "../../../models/platform-config.model";

export const SECTION_KEYS = {
  SECTION_GENERAL: "GENERAL",
  // SECTION_IMPORTS: "IMPORTS",
};

export const SECTIONS_ICONS: Partial<Record<string, string>> = {
  [SECTION_KEYS.SECTION_GENERAL]: "⚙️",
  // [SECTION_KEYS.SECTION_IMPORTS]: "📥",
};

export const SECTIONS = new Set<string>(Object.values(SECTION_KEYS));

// export const IMPORTS_KEYS = new Set<string>([
//   // CONFIG_KEYS.IMPORT_ON_DUPLICATE,
//   CONFIG_KEYS.IMPORTS_CSV_SUGGESTION_CONFIDENCE,
//   CONFIG_KEYS.IMPORTS_CSV_DELIMITER,
//   CONFIG_KEYS.IMPORTS_CSV_DATE_FORMAT,
//   CONFIG_KEYS.IMPORTS_CSV_SKIP_HEADER_ROWS,
//   CONFIG_KEYS.IMPORTS_CSV_COL_DATE,
//   CONFIG_KEYS.IMPORTS_CSV_COL_AMOUNT,
//   CONFIG_KEYS.IMPORTS_CSV_COL_DESCRIPTION,
//   CONFIG_KEYS.IMPORTS_CSV_COL_COUNTERPARTY,
//   CONFIG_KEYS.IMPORTS_CSV_COL_EXTERNAL_REF,
//   CONFIG_KEYS.IMPORTS_CSV_COL_BANK_ACCOUNT,
//   CONFIG_KEYS.IMPORTS_CSV_COL_VALUE_DATE,
// ]);

export const CONFIG_LABELS_I18N_KEYS: Partial<Record<string, string>> = {
  [CONFIG_KEYS.GENERAL_APP_NAME]: "SETTING.KEY_LABEL.APP_NAME",
  [CONFIG_KEYS.GENERAL_DEFAULT_COUNTRY]: "SETTING.KEY_LABEL.DEFAULT_COUNTRY",

  // [CONFIG_KEYS.IMPORTS_CSV_SKIP_HEADER_ROWS]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_SKIP_HEADER_ROWS",
  // [CONFIG_KEYS.IMPORTS_CSV_SUGGESTION_CONFIDENCE]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_SUGGESTION_CONFIDENCE",
  // [CONFIG_KEYS.IMPORTS_CSV_DELIMITER]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_DELIMITER",
  // [CONFIG_KEYS.IMPORTS_CSV_DATE_FORMAT]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_DATE_FORMAT",

  // [CONFIG_KEYS.IMPORTS_CSV_COL_DATE]: "SETTING.KEY_LABEL.IMPORTS_CSV_COL_DATE",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_AMOUNT]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_COL_AMOUNT",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_BANK_ACCOUNT]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_COL_BANK_ACCOUNT",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_COUNTERPARTY]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_COL_COUNTERPARTY",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_DESCRIPTION]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_COL_DESCRIPTION",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_VALUE_DATE]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_COL_VALUE_DATE",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_EXTERNAL_REF]:
  //   "SETTING.KEY_LABEL.IMPORTS_CSV_COL_EXTERNAL_REF",
};

export const KEY_VALUETYPE: Partial<
  Record<string, "string" | "number" | "boolean">
> = {
  // [CONFIG_KEYS.IMPORTS_CSV_SUGGESTION_CONFIDENCE]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_SKIP_HEADER_ROWS]: "boolean",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_DATE]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_AMOUNT]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_DESCRIPTION]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_COUNTERPARTY]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_EXTERNAL_REF]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_BANK_ACCOUNT]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_COL_VALUE_DATE]: "number",
  // [CONFIG_KEYS.IMPORTS_CSV_DELIMITER]: "string",
  // [CONFIG_KEYS.IMPORTS_CSV_DATE_FORMAT]: "string",
};

// unit for the duration keyframes. d=days, m=months
export const CONFIG_UNITS_I18N_KEYS: Partial<Record<string, string>> = {
  // [CONFIG_KEYS.CONTACTS_ALERT_PEB_EXPIRY_WARNING_MONTHS]: "GLOBAL.MONTHS",
};

// export const KEY_MIN_VALUES: Partial<Record<string, number>> = {
//   [CONFIG_KEYS.IMPORTS_CSV_SUGGESTION_CONFIDENCE]: 1,
//   [CONFIG_KEYS.IMPORTS_CSV_COL_VALUE_DATE]: -1,
//   [CONFIG_KEYS.IMPORTS_CSV_COL_BANK_ACCOUNT]: 1,
//   [CONFIG_KEYS.IMPORTS_CSV_COL_EXTERNAL_REF]: 1,
//   [CONFIG_KEYS.IMPORTS_CSV_COL_COUNTERPARTY]: 1,
//   [CONFIG_KEYS.IMPORTS_CSV_COL_DESCRIPTION]: 1,
//   [CONFIG_KEYS.IMPORTS_CSV_COL_AMOUNT]: 1,
//   [CONFIG_KEYS.IMPORTS_CSV_COL_DATE]: 1,
// };

export const DIRECTION_OPTIONS = [
  { value: "EXPENSE", label: "💸 Dépense" },
  { value: "INCOME", label: "💰 Recette" },
  { value: "NEUTRAL", label: "↔️ Neutre" },
];

export function toFormKey(key: string): string {
  return key.replace(/\./g, "__");
}

export function getFieldError(key: string, form: FormGroup): string | null {
  const ctrl = form?.get(toFormKey(key));
  if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
  if (ctrl.hasError("required")) return "Ce champ est obligatoire.";
  return "Valeur invalide.";
}

export function getMinIfExists(key: string): number | undefined {
  // let min = KEY_MIN_VALUES[key];
  // if (min !== undefined) {
  //   return min;
  // }

  // if (getKeyValueType(key) === "number") {
  //   return 0;
  // }

  return undefined;
}

export function getKeyI18NValue(key: string): string | "" {
  return CONFIG_LABELS_I18N_KEYS[key] ?? "";
}

export function getKeyI18NUnit(key: string): string | "" {
  return CONFIG_UNITS_I18N_KEYS[key] ?? "";
}
export function getKeyValueType(key: string): "string" | "number" | "boolean" {
  return KEY_VALUETYPE[key] ?? "string";
}

export function createConfigSaveRequest(
  form: FormGroup,
  configKeys: Set<string> | string[],
  configService: PlatformConfigService,
  canEdit: boolean,
): Observable<any> {
  console.log("//////////////////////////////");
  const keysArray = Array.from(configKeys);
  // 1. Récupération des contrôles de l'onglet
  const currentTabControls = keysArray.map((key) => form.get(toFormKey(key)));

  console.log("Saving configs for keys:", keysArray);

  // 2. Validation générique synchronisée avec KEY_VALUETYPE
  const isCurrentTabInvalid = currentTabControls.some((control, i) => {
    if (!control) return false;
    if (!control.invalid) return false; // Si le contrôle est valide, tout va bien

    const originalKey = keysArray[i];
    const definedType = KEY_VALUETYPE[originalKey];

    // Exception pour le -1 autorisé sur les champs de type 'number'
    if (
      definedType === "number" &&
      control.value === -1 &&
      getMinIfExists(originalKey) === -1
    ) {
      return false;
    }

    // Exception pour le champ vide si tes règles l'autorisent encore
    if (control.value === "") {
      return false;
    }

    return control.invalid;
  });

  console.log("Is current tab invalid?", isCurrentTabInvalid);

  if (isCurrentTabInvalid || !canEdit) {
    currentTabControls.forEach((control) => control?.markAsTouched());
    return of(null);
  }

  // 3. Préparation automatique des requêtes API (Reconversion en string pour la DB)
  const updates = keysArray.map((key) => {
    console.log(`Processing config key: ${key}`);
    const value = form.get(toFormKey(key))?.value;
    console.log(`Saving config ${key} with value:`, value);

    const definedType = KEY_VALUETYPE[key];
    let cleanValue: string;

    if (definedType === "boolean") {
      // Si c'est un boolean, on retransforme true/false en "1"/"0" (ou "true"/"false" selon ta DB)
      cleanValue = value ? "1" : "0";
    } else {
      // Pour les 'number' et 'string'
      cleanValue =
        value !== null &&
        value !== undefined &&
        value !== -1 &&
        String(value).trim() !== ""
          ? String(value).trim()
          : "-1";
    }

    return configService.updateOne(key, { configValue: cleanValue });
  });

  return updates.length > 0 ? forkJoin(updates) : of(null);
}
