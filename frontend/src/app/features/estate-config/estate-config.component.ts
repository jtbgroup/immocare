// features/estate/components/user/estate-settings/estate-settings.component.ts
// UC004_ESTATE_PLACEHOLDER Phase 5 — Per-estate settings page
// Route: /estates/:estateId/settings
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslocoModule, TranslocoService } from "@ngneat/transloco";
import { Subject, forkJoin } from "rxjs";
import { finalize, takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../core/services/active-estate.service";
import { DateFormatService } from "../../core/services/date-format.service";
import { EstateConfigService } from "../../core/services/estate-config.service";
import { TagCategoryService } from "../../core/services/tag-category.service";
import { TagSubcategoryService } from "../../core/services/tag-subcategory.service";
import {
  DATE_FORMAT_PRESETS,
  EstateConfigDTO,
} from "../../models/estate-config.model";
import {
  SaveTagCategoryRequest,
  SaveTagSubcategoryRequest,
  TagCategory,
  TagSubcategory,
} from "../../models/transaction.model";
import { EstateSettingsAssetsComponent } from "./components/estate-config-assets.component/estate-config-assets.component";
import { EstateSettingsBoilersComponent } from "./components/estate-config-boilers.component/estate-config-boilers.component";
import { EstateSettingsContractsComponent } from "./components/estate-config-contracts.component/estate-config-contracts.component";
import { EstateSettingsImportsComponent } from "./components/estate-config-imports.component/estate-config-imports.component";
import {
  CONFIG_LABELS_I18N_KEYS,
  CONFIG_UNITS_I18N_KEYS,
  INTEGER_KEYS,
} from "./helpers/estate-config.helpers";

type Section = "contracts" | "imports" | "boilers" | "assets" | "categories";

@Component({
  selector: "app-estate-settings",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    EstateSettingsContractsComponent,
    EstateSettingsImportsComponent,
    EstateSettingsBoilersComponent,
    EstateSettingsAssetsComponent,
  ],
  templateUrl: "./estate-config.component.html",
  styleUrls: ["./estate-config.component.scss"],
})
export class EstateSettingsComponent implements OnInit, OnDestroy {
  // ── Settings ──────────────────────────────────────────────────────────────
  configs: EstateConfigDTO[] = [];
  loading = false;
  saving = false;
  error: string | null = null;
  successMessage: string | null = null;
  form!: FormGroup;
  activeSection: Section = "contracts";

  // ── Categories & subcategories ────────────────────────────────────────────
  categories: TagCategory[] = [];
  categoriesLoading = false;
  categoriesError: string | null = null;
  categoriesSuccess: string | null = null;

  // Category form (create / edit)
  showCategoryForm = false;
  editingCategory: TagCategory | null = null;
  categoryForm!: FormGroup;
  categorySaving = false;

  // Subcategory form (create / edit)
  expandedCategoryId: number | null = null;
  categorySubcategories: Record<number, TagSubcategory[]> = {};
  subcategoriesLoading: Record<number, boolean> = {};
  showSubcategoryForm: Record<number, boolean> = {};
  editingSubcategory: Record<number, TagSubcategory | null> = {};
  subcategoryForms: Record<number, FormGroup> = {};
  subcategorySaving: Record<number, boolean> = {};
  subcategoryError: Record<number, string | null> = {};

  readonly CONFIG_UNITS_I18N_KEYS = CONFIG_UNITS_I18N_KEYS;
  readonly CONFIG_LABELS_I18N_KEYS = CONFIG_LABELS_I18N_KEYS;
  readonly DATE_FORMAT_PRESETS = DATE_FORMAT_PRESETS;

  readonly DIRECTION_OPTIONS = [
    { value: "EXPENSE", label: "💸 Dépense" },
    { value: "INCOME", label: "💰 Recette" },
    { value: "NEUTRAL", label: "↔️ Neutre" },
  ];

  readonly sectionsI18N: {
    id: Section;
    icon: string;
  }[] = [
    {
      id: "contracts",
      icon: "🔔",
    },
    {
      id: "imports",
      icon: "📥",
    },
    {
      id: "boilers",
      icon: "🔥",
    },
    {
      id: "assets",
      icon: "🔗",
    },
    {
      id: "categories",
      icon: "🏷️",
    },
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private configService: EstateConfigService,
    private dateFormatService: DateFormatService,
    private tagSubcategoryService: TagSubcategoryService,
    private tagCategoryService: TagCategoryService,
    private activeEstateService: ActiveEstateService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    public translocoService: TranslocoService,
  ) {}

  get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  get canEdit(): boolean {
    return this.activeEstateService.canEdit();
  }

  ngOnInit(): void {
    const s = this.route.snapshot.queryParamMap.get("section") as Section;
    if (s && this.sectionsI18N.find((sec) => sec.id === s))
      this.activeSection = s;

    // this.buildRuleForm();
    this.load();
    // this.loadSubcategories();

    if (this.activeSection === "categories") this.loadCategories();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setSection(section: Section): void {
    this.activeSection = section;
    this.router.navigate([], { queryParams: { section }, replaceUrl: true });

    // if (section === "boilers") this.loadValidityRules();
    // if (section === "assets") this.loadAssetMappings();
    if (section === "categories") this.loadCategories();
  }

  // ── Settings CRUD ────────────────────────────────────────────────────────

  load(): void {
    this.loading = true;
    this.configService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (configs) => {
          this.configs = configs;
          this.buildForm(configs);
          this.loading = false;
        },
        error: () => {
          this.error = "Impossible de charger les paramètres.";
          this.loading = false;
        },
      });
  }

  // Dots are not allowed in FormGroup keys — encode/decode with __
  toFormKey(key: string): string {
    return key.replace(/\./g, "__");
  }
  private fromFormKey(key: string): string {
    return key.replace(/__/g, ".");
  }

  buildForm(configs: EstateConfigDTO[]): void {
    const group: Record<string, unknown[]> = {};
    for (const c of configs) {
      const validators = [Validators.required];
      if (INTEGER_KEYS.has(c.configKey)) {
        validators.push(Validators.pattern(/^-?[0-9]+$/));
      }
      group[this.toFormKey(c.configKey)] = [c.configValue, validators];
    }
    this.form = this.fb.group(group);
    if (!this.canEdit) this.form.disable();
  }

  // TODO : delete this method
  save(): void {
    console.log("Formulaire valide ?", this.form.valid);
    console.log(
      "Valeurs brutes du formulaire avant envoi :",
      this.form.getRawValue(),
    );
    if (this.form.invalid || !this.canEdit) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = null;
    this.successMessage = null;

    const updates = Object.entries(this.form.value).map(([key, value]) =>
      this.configService.updateOne(this.fromFormKey(key), {
        configValue: String(value).trim(),
      }),
    );

    forkJoin(updates)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.saving = false)),
      )
      .subscribe({
        next: () => {
          this.successMessage = "Paramètres enregistrés.";
          setTimeout(() => (this.successMessage = null), 3000);
          this.dateFormatService.reload();
          this.load();
        },
        error: (err) => {
          this.error =
            err?.error?.message ?? "Erreur lors de l'enregistrement.";
        },
      });
  }

  // ── Boiler validity rules ────────────────────────────────────────────────

  // loadValidityRules(): void {
  //   this.rulesLoading = true;
  //   this.configService
  //     .getValidityRules()
  //     .pipe(takeUntil(this.destroy$))
  //     .subscribe({
  //       next: (rules) => {
  //         this.validityRules = rules;
  //         this.rulesLoading = false;
  //       },
  //       error: () => {
  //         this.rulesLoading = false;
  //       },
  //     });
  // }

  // buildRuleForm(): void {
  //   this.ruleForm = this.fb.group({
  //     validFrom: [null, Validators.required],
  //     validityDurationMonths: [24, [Validators.required, Validators.min(1)]],
  //     description: [""],
  //   });
  // }

  // openAddRuleForm(): void {
  //   this.showAddRuleForm = true;
  //   this.ruleError = null;
  //   this.ruleForm.reset({ validityDurationMonths: 24 });
  // }

  // cancelAddRule(): void {
  //   this.showAddRuleForm = false;
  //   this.ruleError = null;
  // }

  // saveRule(): void {
  //   if (this.ruleForm.invalid) {
  //     this.ruleForm.markAllAsTouched();
  //     return;
  //   }
  //   this.ruleSaving = true;
  //   this.ruleError = null;

  //   const req: AddBoilerServiceValidityRuleRequest = {
  //     validFrom: this.ruleForm.value.validFrom,
  //     validityDurationMonths: this.ruleForm.value.validityDurationMonths,
  //     description: this.ruleForm.value.description || null,
  //   };

  //   this.configService
  //     .addValidityRule(req)
  //     .pipe(
  //       takeUntil(this.destroy$),
  //       finalize(() => (this.ruleSaving = false)),
  //     )
  //     .subscribe({
  //       next: () => {
  //         this.showAddRuleForm = false;
  //         this.loadValidityRules();
  //       },
  //       error: (err) => {
  //         this.ruleError =
  //           err?.error?.message ?? "Erreur lors de la sauvegarde.";
  //       },
  //     });
  // }

  // ── Categories ───────────────────────────────────────────────────────────

  loadCategories(): void {
    this.categoriesLoading = true;
    this.categoriesError = null;
    this.tagCategoryService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cats) => {
          this.categories = cats;
          this.categoriesLoading = false;
        },
        error: () => {
          this.categoriesError = "Impossible de charger les catégories.";
          this.categoriesLoading = false;
        },
      });
  }

  openAddCategoryForm(): void {
    this.editingCategory = null;
    this.categoryForm = this.fb.group({
      name: ["", Validators.required],
      description: [""],
    });
    this.showCategoryForm = true;
    this.categoriesError = null;
  }

  openEditCategoryForm(cat: TagCategory): void {
    this.editingCategory = cat;
    this.categoryForm = this.fb.group({
      name: [cat.name, Validators.required],
      description: [(cat as any).description ?? ""],
    });
    this.showCategoryForm = true;
    this.categoriesError = null;
  }

  cancelCategoryForm(): void {
    this.showCategoryForm = false;
    this.editingCategory = null;
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }
    this.categorySaving = true;
    this.categoriesError = null;

    const req: SaveTagCategoryRequest = {
      name: this.categoryForm.value.name.trim(),
      description: this.categoryForm.value.description?.trim() || null,
    };

    const call$ = this.editingCategory
      ? this.tagCategoryService.update(this.editingCategory.id, req)
      : this.tagCategoryService.create(req);

    call$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.categorySaving = false)),
      )
      .subscribe({
        next: () => {
          this.showCategoryForm = false;
          this.editingCategory = null;
          this.categoriesSuccess = "Catégorie enregistrée.";
          setTimeout(() => (this.categoriesSuccess = null), 2500);
          this.loadCategories();
        },
        error: (err) => {
          this.categoriesError =
            err?.error?.message ?? "Erreur lors de l'enregistrement.";
        },
      });
  }

  deleteCategory(cat: TagCategory): void {
    if (!confirm(`Supprimer la catégorie "${cat.name}" ?`)) return;
    this.tagCategoryService
      .delete(cat.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.categoriesSuccess = "Catégorie supprimée.";
          setTimeout(() => (this.categoriesSuccess = null), 2500);
          if (this.expandedCategoryId === cat.id)
            this.expandedCategoryId = null;
          this.loadCategories();
        },
        error: (err) => {
          this.categoriesError =
            err?.error?.message ?? "Impossible de supprimer cette catégorie.";
        },
      });
  }

  // ── Subcategories (per category) ─────────────────────────────────────────

  toggleCategory(catId: number): void {
    if (this.expandedCategoryId === catId) {
      this.expandedCategoryId = null;
      return;
    }
    this.expandedCategoryId = catId;
    this.showSubcategoryForm[catId] = false;
    this.editingSubcategory[catId] = null;
    this.loadSubcategoriesForCategory(catId);
  }

  loadSubcategoriesForCategory(catId: number): void {
    this.subcategoriesLoading[catId] = true;
    this.tagSubcategoryService
      .getAll(catId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (subs) => {
          this.categorySubcategories[catId] = subs;
          this.subcategoriesLoading[catId] = false;
        },
        error: () => {
          this.subcategoriesLoading[catId] = false;
        },
      });
  }

  openAddSubcategoryForm(catId: number): void {
    this.editingSubcategory[catId] = null;
    this.subcategoryError[catId] = null;
    this.subcategoryForms[catId] = this.fb.group({
      name: ["", Validators.required],
      direction: ["EXPENSE", Validators.required],
      description: [""],
    });
    this.showSubcategoryForm[catId] = true;
  }

  openEditSubcategoryForm(catId: number, sub: TagSubcategory): void {
    this.editingSubcategory[catId] = sub;
    this.subcategoryError[catId] = null;
    this.subcategoryForms[catId] = this.fb.group({
      name: [sub.name, Validators.required],
      direction: [sub.direction, Validators.required],
      description: [(sub as any).description ?? ""],
    });
    this.showSubcategoryForm[catId] = true;
  }

  cancelSubcategoryForm(catId: number): void {
    this.showSubcategoryForm[catId] = false;
    this.editingSubcategory[catId] = null;
    this.subcategoryError[catId] = null;
  }

  saveSubcategory(catId: number): void {
    const form = this.subcategoryForms[catId];
    if (!form || form.invalid) {
      form?.markAllAsTouched();
      return;
    }
    this.subcategorySaving[catId] = true;
    this.subcategoryError[catId] = null;

    const editing = this.editingSubcategory[catId];
    const req: SaveTagSubcategoryRequest = {
      categoryId: catId,
      name: form.value.name.trim(),
      direction: form.value.direction,
      description: form.value.description?.trim() || null,
    };

    const call$ = editing
      ? this.tagSubcategoryService.update(editing.id, req)
      : this.tagSubcategoryService.create(req);

    call$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.subcategorySaving[catId] = false)),
      )
      .subscribe({
        next: () => {
          this.showSubcategoryForm[catId] = false;
          this.editingSubcategory[catId] = null;
          this.loadSubcategoriesForCategory(catId);
        },
        error: (err) => {
          this.subcategoryError[catId] =
            err?.error?.message ?? "Erreur lors de l'enregistrement.";
        },
      });
  }

  deleteSubcategory(catId: number, sub: TagSubcategory): void {
    if (!confirm(`Supprimer la sous-catégorie "${sub.name}" ?`)) return;
    this.tagSubcategoryService
      .delete(sub.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadSubcategoriesForCategory(catId),
        error: (err) => {
          this.subcategoryError[catId] =
            err?.error?.message ?? "Impossible de supprimer.";
        },
      });
  }

  // ── Form helpers ────────────────────────────────────────────────────────

  configsFor(keys: Set<string>): EstateConfigDTO[] {
    return this.configs.filter((c) => keys.has(c.configKey));
  }

  getSectionIcon(id: string): string {
    const section = this.sectionsI18N.find((s) => s.id === id);
    return section ? section.icon : "";
  }

  isIntegerKey(key: string): boolean {
    return INTEGER_KEYS.has(key);
  }
  // isDateFormatKey(key: string): boolean {
  //   return key === CONFIG_KEYS.APP_DATE_FORMAT;
  // }
  // isSelectKey(key: string): boolean {
  //   return key === CONFIG_KEYS.IMPORT_ON_DUPLICATE;
  // }

  // presetOrCustom(key: string): string {
  //   const val = this.form?.get(this.toFormKey(key))?.value as string;
  //   return this.DATE_FORMAT_PRESETS.some((p) => p.value === val)
  //     ? val
  //     : "__custom__";
  // }

  // onDateFormatSelectChange(key: string, event: Event): void {
  //   const selected = (event.target as HTMLSelectElement).value;
  //   if (selected !== "__custom__")
  //     this.form.get(this.toFormKey(key))?.setValue(selected);
  // }

  fieldError(key: string): string | null {
    const ctrl = this.form?.get(this.toFormKey(key));
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Ce champ est obligatoire.";
    if (ctrl.hasError("pattern")) return "Valeur invalide.";
    return "Valeur invalide.";
  }

  // ruleFieldError(key: string): string | null {
  //   const ctrl = this.ruleForm?.get(key);
  //   if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
  //   if (ctrl.hasError("required")) return "Champ requis.";
  //   if (ctrl.hasError("min")) return "Doit être ≥ 1.";
  //   return "Valeur invalide.";
  // }

  categoryFieldError(key: string): string | null {
    const ctrl = this.categoryForm?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Ce champ est obligatoire.";
    return "Valeur invalide.";
  }

  subcategoryFieldError(catId: number, key: string): string | null {
    const ctrl = this.subcategoryForms[catId]?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Ce champ est obligatoire.";
    return "Valeur invalide.";
  }

  getSubcategoryControl(catId: number, key: string): FormControl {
    return (
      (this.subcategoryForms[catId]?.get(key) as FormControl) ??
      new FormControl()
    );
  }

  subcategoryCount(cat: TagCategory): number {
    return (cat as any).subcategoryCount ?? 0;
  }

  subDescription(sub: TagSubcategory): string {
    return (sub as any).description || "—";
  }

  subUsage(sub: TagSubcategory): number {
    return (sub as any).usage ?? 0;
  }

  directionLabel(direction: string): string {
    const map: Record<string, string> = {
      EXPENSE: "💸 Dépense",
      INCOME: "💰 Recette",
      NEUTRAL: "↔️ Neutre",
    };
    return map[direction] ?? direction;
  }

  directionBadgeClass(direction: string): string {
    const map: Record<string, string> = {
      EXPENSE: "badge-expense",
      INCOME: "badge-income",
      NEUTRAL: "badge-neutral",
    };
    return map[direction] ?? "";
  }

  keyExists(key: string): boolean {
    const activeLang = this.translocoService.getActiveLang();
    const translations = this.translocoService.getTranslation(activeLang);

    if (!translations) return false;

    return translations.hasOwnProperty(key);
  }
}
