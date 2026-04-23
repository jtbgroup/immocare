// features/transaction/components/transaction-categories/transaction-categories.component.ts — UC004_ESTATE_PLACEHOLDER Phase 4
// No changes to component logic — TagCategoryService and TagSubcategoryService handle estate scoping.
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TagCategoryService } from '../../../../core/services/tag-category.service';
import { TagSubcategoryService } from '../../../../core/services/tag-subcategory.service';
import {
  SaveTagCategoryRequest,
  SaveTagSubcategoryRequest,
  SubcategoryDirection,
  TagCategory,
  TagSubcategory,
} from '../../../../models/transaction.model';

@Component({
  selector: 'app-transaction-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-categories.component.html',
  styleUrls: ['./transaction-categories.component.scss'],
})
export class TransactionCategoriesComponent implements OnInit {
  categories: TagCategory[] = [];
  subcategories: TagSubcategory[] = [];

  editingCategory?: TagCategory;
  newCategory: SaveTagCategoryRequest = { name: '' };
  showNewCategory = false;

  editingSubcategory?: TagSubcategory;
  newSubcategory: SaveTagSubcategoryRequest = {
    categoryId: 0,
    name: '',
    direction: 'EXPENSE',
  };
  showNewSubcategory = false;

  categoryError: string | null = null;
  subcategoryError: string | null = null;

  readonly directions: SubcategoryDirection[] = ['INCOME', 'EXPENSE', 'BOTH'];

  constructor(
    private tagCategoryService: TagCategoryService,
    private tagSubcategoryService: TagSubcategoryService,
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadSubcategories();
  }

  loadCategories(): void {
    this.tagCategoryService.getAll().subscribe((c) => (this.categories = c));
  }

  loadSubcategories(): void {
    this.tagSubcategoryService.getAll().subscribe((s) => (this.subcategories = s));
  }

  resetSubForm(): void {
    this.newSubcategory = { categoryId: 0, name: '', direction: 'EXPENSE' };
  }

  saveCategory(): void {
    this.categoryError = null;
    const obs = this.editingCategory
      ? this.tagCategoryService.update(this.editingCategory.id, this.newCategory)
      : this.tagCategoryService.create(this.newCategory);
    obs.subscribe({
      next: () => {
        this.editingCategory = undefined;
        this.showNewCategory = false;
        this.newCategory = { name: '' };
        this.loadCategories();
      },
      error: (err) => (this.categoryError = err?.error?.message || 'Error'),
    });
  }

  editCategory(cat: TagCategory): void {
    this.editingCategory = cat;
    this.newCategory = { name: cat.name, description: cat.description };
    this.showNewCategory = true;
  }

  deleteCategory(cat: TagCategory): void {
    if (!confirm(`Delete category "${cat.name}"?`)) return;
    this.tagCategoryService.delete(cat.id).subscribe({
      next: () => this.loadCategories(),
      error: (err) => (this.categoryError = err?.error?.message || 'Cannot delete'),
    });
  }

  saveSubcategory(): void {
    this.subcategoryError = null;
    const obs = this.editingSubcategory
      ? this.tagSubcategoryService.update(this.editingSubcategory.id, this.newSubcategory)
      : this.tagSubcategoryService.create(this.newSubcategory);
    obs.subscribe({
      next: () => {
        this.editingSubcategory = undefined;
        this.showNewSubcategory = false;
        this.resetSubForm();
        this.loadSubcategories();
      },
      error: (err) => (this.subcategoryError = err?.error?.message || 'Error'),
    });
  }

  editSubcategory(sub: TagSubcategory): void {
    this.editingSubcategory = sub;
    this.newSubcategory = {
      categoryId: sub.categoryId,
      name: sub.name,
      direction: sub.direction,
      description: sub.description,
    };
    this.showNewSubcategory = true;
  }

  deleteSubcategory(sub: TagSubcategory): void {
    if (!confirm(`Delete subcategory "${sub.name}"?`)) return;
    this.tagSubcategoryService.delete(sub.id).subscribe({
      next: () => this.loadSubcategories(),
      error: (err) => (this.subcategoryError = err?.error?.message || 'Cannot delete'),
    });
  }
}
