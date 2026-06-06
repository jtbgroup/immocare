import { CommonModule } from "@angular/common";
import { Component, input } from "@angular/core";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { finalize, Subject, takeUntil } from "rxjs";
import { TagCategoryService } from "../../../../core/services/tag-category.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import {
  SaveTagCategoryRequest,
  SaveTagSubcategoryRequest,
  TagCategory,
  TagSubcategory,
} from "../../../../models/transaction.model";

import { DIRECTION_OPTIONS } from "../../helpers/estate-config.helpers";

@Component({
  selector: "app-estate-config-categories",
  standalone: true,
  imports: [CommonModule, TranslocoModule, ReactiveFormsModule],
  templateUrl: "./estate-config-categories.component.html",
  styleUrl: "./estate-config-categories.component.scss",
})
export class EstateConfigCategoriesComponent {
  canEdit = input<boolean>(false);
  readonly DIRECTION_OPTIONS = DIRECTION_OPTIONS;

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

  private destroy$ = new Subject<void>();

  constructor(
    private tagCategoryService: TagCategoryService,
    private tagSubcategoryService: TagSubcategoryService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  // ── Categories  ─────────────────────────────────────────

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
      description: ["", Validators.required],
    });
    this.showCategoryForm = true;
    this.categoriesError = null;
  }

  openEditCategoryForm(cat: TagCategory): void {
    this.editingCategory = cat;
    this.categoryForm = this.fb.group({
      name: [cat.name, Validators.required],
      description: [(cat as any).description ?? "", Validators.required],
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
}
