import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { BankAccountService } from "../../../../core/services/bank-account.service";
import { TagCategoryService } from "../../../../core/services/tag-category.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import {
  BankAccount,
  FinancialTransaction,
  TagCategory,
  TagSubcategory,
} from "../../../../models/transaction.model";
import { AssetLinkEditorComponent } from "../asset-link-editor/asset-link-editor.component";

@Component({
  selector: "app-transaction-form",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AssetLinkEditorComponent],
  templateUrl: "./transaction-form.component.html",
  // styleUrls: ["./transaction-form.component.scss"],
})
export class TransactionFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  transactionId?: number;
  existing?: FinancialTransaction;
  loading = false;
  error: string | null = null;

  categories: TagCategory[] = [];
  subcategories: TagSubcategory[] = [];
  bankAccounts: BankAccount[] = [];
  assetLinks: any[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private tagCategoryService: TagCategoryService,
    private tagSubcategoryService: TagSubcategoryService,
    private bankAccountService: BankAccountService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadRefData();

    const id = this.route.snapshot.paramMap.get("id");
    if (id) {
      this.isEdit = true;
      this.transactionId = +id;
      this.loadExisting(+id);
    }

    this.form.get("categoryId")?.valueChanges.subscribe((catId) => {
      if (catId) {
        const dir = this.form.get("direction")?.value;
        this.tagSubcategoryService
          .getAll(catId, dir || undefined)
          .subscribe((s) => (this.subcategories = s));
      } else {
        this.subcategories = [];
        this.form.patchValue({ subcategoryId: null });
      }
    });
  }

  buildForm(): void {
    const today = new Date().toISOString().split("T")[0];
    this.form = this.fb.group({
      direction: ["INCOME", Validators.required],
      transactionDate: [today, Validators.required],
      valueDate: [null],
      accountingMonth: [today.substring(0, 7) + "-01", Validators.required],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      description: [null],
      counterpartyName: [null],
      counterpartyAccount: [null, Validators.maxLength(50)],
      bankAccountId: [null],
      categoryId: [null],
      subcategoryId: [null],
      leaseId: [null],
      housingUnitId: [null],
      buildingId: [null],
    });
  }

  loadRefData(): void {
    this.tagCategoryService.getAll().subscribe((c) => (this.categories = c));
    this.bankAccountService
      .getAll(true)
      .subscribe((b) => (this.bankAccounts = b));
  }

  loadExisting(id: number): void {
    this.loading = true;
    this.transactionService.getById(id).subscribe({
      next: (tx) => {
        this.existing = tx;
        this.assetLinks = tx.assetLinks || [];
        if (tx.categoryId) {
          this.tagSubcategoryService
            .getAll(tx.categoryId)
            .subscribe((s) => (this.subcategories = s));
        }
        this.form.patchValue({
          direction: tx.direction,
          transactionDate: tx.transactionDate,
          valueDate: tx.valueDate,
          accountingMonth: tx.accountingMonth,
          amount: tx.amount,
          description: tx.description,
          counterpartyName: tx.counterpartyName,
          counterpartyAccount: tx.counterpartyAccount,
          bankAccountId: tx.bankAccountId,
          categoryId: tx.categoryId,
          subcategoryId: tx.subcategoryId,
          leaseId: tx.leaseId,
          housingUnitId: tx.housingUnitId,
          buildingId: tx.buildingId,
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.value;
    const req = {
      ...value,
      assetLinks: this.assetLinks,
    };

    const obs = this.isEdit
      ? this.transactionService.update(this.transactionId!, req)
      : this.transactionService.create(req);

    obs.subscribe({
      next: (tx) => this.router.navigate(["/transactions", tx.id]),
      error: (err) => (this.error = err?.error?.message || "An error occurred"),
    });
  }

  cancel(): void {
    if (this.form.dirty && !confirm("Discard unsaved changes?")) return;
    this.router.navigate(["/transactions"]);
  }

  get isIncomeDirection(): boolean {
    return this.form.get("direction")?.value === "INCOME";
  }
}
