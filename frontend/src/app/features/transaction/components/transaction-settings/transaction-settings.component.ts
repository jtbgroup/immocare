import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { BankAccountService } from "../../../../core/services/bank-account.service";
import { TagCategoryService } from "../../../../core/services/tag-category.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import {
  BankAccount,
  BankAccountType,
  SaveBankAccountRequest,
  SaveTagCategoryRequest,
  SaveTagSubcategoryRequest,
  SubcategoryDirection,
  TagCategory,
  TagSubcategory,
} from "../../../../models/transaction.model";

@Component({
  selector: "app-transaction-settings",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./transaction-settings.component.html",
  // styleUrls: ["./transaction-settings.component.scss"],
})
export class TransactionSettingsComponent implements OnInit {
  categories: TagCategory[] = [];
  subcategories: TagSubcategory[] = [];
  bankAccounts: BankAccount[] = [];

  editingCategory?: TagCategory;
  newCategory: SaveTagCategoryRequest = { name: "" };
  showNewCategory = false;

  editingSubcategory?: TagSubcategory;
  newSubcategory: SaveTagSubcategoryRequest = {
    categoryId: 0,
    name: "",
    direction: "EXPENSE",
  };
  showNewSubcategory = false;

  editingBankAccount?: BankAccount;
  newBankAccount: SaveBankAccountRequest = {
    label: "",
    accountNumber: "",
    type: "CURRENT",
    isActive: true,
  };
  showNewBankAccount = false;

  categoryError: string | null = null;
  subcategoryError: string | null = null;
  bankAccountError: string | null = null;

  readonly directions: SubcategoryDirection[] = ["INCOME", "EXPENSE", "BOTH"];
  readonly bankAccountTypes: BankAccountType[] = ["CURRENT", "SAVINGS"];

  constructor(
    private tagCategoryService: TagCategoryService,
    private tagSubcategoryService: TagSubcategoryService,
    private bankAccountService: BankAccountService,
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadSubcategories();
    this.loadBankAccounts();
  }

  loadCategories(): void {
    this.tagCategoryService.getAll().subscribe((c) => (this.categories = c));
  }

  loadSubcategories(): void {
    this.tagSubcategoryService
      .getAll()
      .subscribe((s) => (this.subcategories = s));
  }

  loadBankAccounts(): void {
    this.bankAccountService.getAll().subscribe((b) => (this.bankAccounts = b));
  }

  // Categories
  saveCategory(): void {
    this.categoryError = null;
    if (this.editingCategory) {
      this.tagCategoryService
        .update(this.editingCategory.id, this.newCategory)
        .subscribe({
          next: () => {
            this.editingCategory = undefined;
            this.loadCategories();
          },
          error: (err) => (this.categoryError = err?.error?.message || "Error"),
        });
    } else {
      this.tagCategoryService.create(this.newCategory).subscribe({
        next: () => {
          this.showNewCategory = false;
          this.newCategory = { name: "" };
          this.loadCategories();
        },
        error: (err) => (this.categoryError = err?.error?.message || "Error"),
      });
    }
  }

  editCategory(cat: TagCategory): void {
    this.editingCategory = cat;
    this.newCategory = { name: cat.name, description: cat.description };
  }

  deleteCategory(cat: TagCategory): void {
    if (!confirm('Delete category "' + cat.name + '"?')) return;
    this.tagCategoryService.delete(cat.id).subscribe({
      next: () => this.loadCategories(),
      error: (err) =>
        (this.categoryError = err?.error?.message || "Cannot delete"),
    });
  }

  // Subcategories
  saveSubcategory(): void {
    this.subcategoryError = null;
    if (this.editingSubcategory) {
      this.tagSubcategoryService
        .update(this.editingSubcategory.id, this.newSubcategory)
        .subscribe({
          next: () => {
            this.editingSubcategory = undefined;
            this.loadSubcategories();
          },
          error: (err) =>
            (this.subcategoryError = err?.error?.message || "Error"),
        });
    } else {
      this.tagSubcategoryService.create(this.newSubcategory).subscribe({
        next: () => {
          this.showNewSubcategory = false;
          this.newSubcategory = {
            categoryId: 0,
            name: "",
            direction: "EXPENSE",
          };
          this.loadSubcategories();
        },
        error: (err) =>
          (this.subcategoryError = err?.error?.message || "Error"),
      });
    }
  }

  editSubcategory(sub: TagSubcategory): void {
    this.editingSubcategory = sub;
    this.newSubcategory = {
      categoryId: sub.categoryId,
      name: sub.name,
      direction: sub.direction,
      description: sub.description,
    };
  }

  deleteSubcategory(sub: TagSubcategory): void {
    if (!confirm('Delete subcategory "' + sub.name + '"?')) return;
    this.tagSubcategoryService.delete(sub.id).subscribe({
      next: () => this.loadSubcategories(),
      error: (err) =>
        (this.subcategoryError = err?.error?.message || "Cannot delete"),
    });
  }

  // Bank accounts
  saveBankAccount(): void {
    this.bankAccountError = null;
    if (this.editingBankAccount) {
      this.bankAccountService
        .update(this.editingBankAccount.id, this.newBankAccount)
        .subscribe({
          next: () => {
            this.editingBankAccount = undefined;
            this.loadBankAccounts();
          },
          error: (err) =>
            (this.bankAccountError = err?.error?.message || "Error"),
        });
    } else {
      this.bankAccountService.create(this.newBankAccount).subscribe({
        next: () => {
          this.showNewBankAccount = false;
          this.newBankAccount = {
            label: "",
            accountNumber: "",
            type: "CURRENT",
            isActive: true,
          };
          this.loadBankAccounts();
        },
        error: (err) =>
          (this.bankAccountError = err?.error?.message || "Error"),
      });
    }
  }

  editBankAccount(ba: BankAccount): void {
    this.editingBankAccount = ba;
    this.newBankAccount = {
      label: ba.label,
      accountNumber: ba.accountNumber,
      type: ba.type,
      isActive: ba.isActive,
    };
  }
}
