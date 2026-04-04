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
import { BuildingService } from "../../../../core/services/building.service";
import { HousingUnitService } from "../../../../core/services/housing-unit.service";
import { LeaseService } from "../../../../core/services/lease.service";
import { TagCategoryService } from "../../../../core/services/tag-category.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import { TransactionService } from "../../../../core/services/transaction.service";
import { Building } from "../../../../models/building.model";
import { HousingUnit } from "../../../../models/housing-unit.model";
import { LeaseSummary } from "../../../../models/lease.model";
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
  styleUrls: ["./transaction-form.component.scss"],
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

  // Linked dropdowns
  buildings: Building[] = [];
  units: HousingUnit[] = [];
  leases: LeaseSummary[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private tagCategoryService: TagCategoryService,
    private tagSubcategoryService: TagSubcategoryService,
    private bankAccountService: BankAccountService,
    private buildingService: BuildingService,
    private housingUnitService: HousingUnitService,
    private leaseService: LeaseService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadRefData();

    const id = this.route.snapshot.paramMap.get("id");
    if (id) {
      this.isEdit = true;
      this.transactionId = +id;
      // Load existing data first, then wire up the cascade listeners
      this.loadExisting(+id, () => this.setupCascadeListeners());
    } else {
      // New transaction: wire up listeners immediately
      this.setupCascadeListeners();
    }
  }

  /**
   * Wire up the cascade listeners for user interactions.
   * Must be called AFTER the form is patched in edit mode,
   * so that patchValue doesn't trigger resets.
   */
  private setupCascadeListeners(): void {
    // When category changes, reload subcategories
    this.form.get("categoryId")?.valueChanges.subscribe((catId) => {
      if (catId) {
        const dir = this.form.get("direction")?.value;
        this.tagSubcategoryService
          .getAll(catId, dir || undefined)
          .subscribe((s) => (this.subcategories = s));
      } else {
        this.subcategories = [];
        this.form.patchValue({ subcategoryId: null }, { emitEvent: false });
      }
    });

    // When building changes, reload units and reset unit/lease
    this.form.get("buildingId")?.valueChanges.subscribe((buildingId) => {
      this.units = [];
      this.leases = [];
      this.form.patchValue(
        { housingUnitId: null, leaseId: null },
        { emitEvent: false },
      );

      if (buildingId) {
        this.housingUnitService
          .getUnitsByBuilding(+buildingId)
          .subscribe((units) => (this.units = units));
      }
    });

    // When unit changes, reload leases and reset lease
    this.form.get("housingUnitId")?.valueChanges.subscribe((unitId) => {
      this.leases = [];
      this.form.patchValue({ leaseId: null }, { emitEvent: false });

      if (unitId) {
        this.leaseService
          .getByUnit(+unitId)
          .subscribe((leases) => (this.leases = leases));
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
    // Load all buildings for the dropdown
    this.buildingService
      .getAllBuildings(0, 200, "name,asc")
      .subscribe((page) => (this.buildings = page.content));
  }

  loadExisting(id: number, onComplete?: () => void): void {
    this.loading = true;
    this.transactionService.getById(id).subscribe({
      next: (tx) => {
        this.existing = tx;
        this.assetLinks = tx.assetLinks || [];

        // Load subcategories if needed
        if (tx.categoryId) {
          this.tagSubcategoryService
            .getAll(tx.categoryId)
            .subscribe((s) => (this.subcategories = s));
        }

        // Pre-load dependent lists, then patch form with emitEvent:false
        // to avoid cascade listeners firing and clearing values.
        const patchAndFinish = () => {
          this.form.patchValue(
            {
              direction: tx.direction,
              transactionDate: tx.transactionDate,
              valueDate: tx.valueDate,
              accountingMonth: tx.accountingMonth,
              amount: tx.amount,
              description: tx.description,
              counterpartyAccount: tx.counterpartyAccount,
              bankAccountId: tx.bankAccountId,
              categoryId: tx.categoryId,
              subcategoryId: tx.subcategoryId,
              buildingId: tx.buildingId,
              housingUnitId: tx.housingUnitId,
              leaseId: tx.leaseId,
            },
            { emitEvent: false },
          );
          this.loading = false;
          // Wire up cascade listeners only after the form is fully patched
          onComplete?.();
        };

        // Helper: load leases for a unit then call continuation
        const loadLeases = (unitId: number, then: () => void) => {
          this.leaseService.getByUnit(unitId).subscribe((leases) => {
            this.leases = leases;
            then();
          });
        };

        // Helper: load units for a building, optionally leases for a unit
        const loadUnitsAndLeases = (
          buildingId: number,
          unitId: number | undefined,
          then: () => void,
        ) => {
          this.housingUnitService
            .getUnitsByBuilding(buildingId)
            .subscribe((units) => {
              this.units = units;
              if (unitId) {
                loadLeases(unitId, then);
              } else {
                then();
              }
            });
        };

        if (tx.buildingId && tx.housingUnitId) {
          loadUnitsAndLeases(tx.buildingId, tx.housingUnitId, patchAndFinish);
        } else if (tx.buildingId) {
          loadUnitsAndLeases(tx.buildingId, undefined, patchAndFinish);
        } else if (tx.housingUnitId) {
          // Unit known but no building
          loadLeases(tx.housingUnitId, patchAndFinish);
        } else if (tx.leaseId) {
          // Only leaseId set (imported transaction): fetch lease to get its unit/building
          this.leaseService.getById(tx.leaseId).subscribe({
            next: (lease) => {
              // Enrich tx with building/unit from the lease so patchValue fills them
              (tx as any).buildingId = lease.buildingId;
              (tx as any).housingUnitId = lease.housingUnitId;
              loadUnitsAndLeases(
                lease.buildingId,
                lease.housingUnitId,
                patchAndFinish,
              );
            },
            error: () => patchAndFinish(),
          });
        } else {
          patchAndFinish();
        }
      },
      error: () => {
        this.loading = false;
        onComplete?.();
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const req = { ...this.form.value, assetLinks: this.assetLinks };
    console.log("SUBMIT payload:", JSON.stringify(req, null, 2)); // ← ajouter
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

  /** Label for a unit in the dropdown */
  unitLabel(unit: HousingUnit): string {
    return `Unit ${unit.unitNumber} — Floor ${unit.floor}`;
  }

  /** Label for a lease in the dropdown */
  leaseLabel(lease: LeaseSummary): string {
    const tenants = lease.tenantNames?.join(", ") || "No tenants";
    return `#${lease.id} — ${tenants} (${lease.status})`;
  }
}
