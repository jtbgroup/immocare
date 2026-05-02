// features/transaction/components/transaction-form/transaction-form.component.ts — UC004_ESTATE_PLACEHOLDER Phase 4
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
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
import { AssetLinkEditorComponent } from "../_partials/asset-link-editor/asset-link-editor.component";

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
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadRefData();

    const id = this.route.snapshot.paramMap.get("id");
    if (id) {
      this.isEdit = true;
      this.transactionId = +id;
      this.loadExisting(+id, () => this.setupCascadeListeners());
    } else {
      this.setupCascadeListeners();
    }
  }

  private get estateId(): string | null {
    return this.activeEstateService.activeEstateId();
  }

  private setupCascadeListeners(): void {
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
        if (tx.categoryId) {
          this.tagSubcategoryService
            .getAll(tx.categoryId)
            .subscribe((s) => (this.subcategories = s));
        }
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
          onComplete?.();
        };
        const loadLeases = (unitId: number, then: () => void) => {
          this.leaseService.getByUnit(unitId).subscribe((leases) => {
            this.leases = leases;
            then();
          });
        };
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
          loadLeases(tx.housingUnitId, patchAndFinish);
        } else if (tx.leaseId) {
          this.leaseService.getById(tx.leaseId).subscribe({
            next: (lease) => {
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

  onAssetSubcategoryPreFill(subcategoryId: number): void {
    if (!this.form.get("subcategoryId")?.value) {
      this.form.patchValue({ subcategoryId }, { emitEvent: false });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const req = { ...this.form.value, assetLinks: this.assetLinks };
    const obs = this.isEdit
      ? this.transactionService.update(this.transactionId!, req)
      : this.transactionService.create(req);

    obs.subscribe({
      next: (tx) => {
        if (this.estateId) {
          this.router.navigate([
            "/estates",
            this.estateId,
            "transactions",
            tx.id,
          ]);
        } else {
          this.router.navigate(["/transactions", tx.id]);
        }
      },
      error: (err) => (this.error = err?.error?.message || "An error occurred"),
    });
  }

  cancel(): void {
    if (this.form.dirty && !confirm("Discard unsaved changes?")) return;
    if (this.estateId) {
      this.router.navigate(["/estates", this.estateId, "transactions"]);
    } else {
      this.router.navigate(["/transactions"]);
    }
  }

  get isIncomeDirection(): boolean {
    return this.form.get("direction")?.value === "INCOME";
  }

  unitLabel(unit: HousingUnit): string {
    return `Unit ${unit.unitNumber} — Floor ${unit.floor}`;
  }

  leaseLabel(lease: LeaseSummary): string {
    const tenants = lease.tenantNames?.join(", ") || "No tenants";
    return `#${lease.id} — ${tenants} (${lease.status})`;
  }
}
