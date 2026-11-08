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
import { TranslocoModule } from "@ngneat/transloco";
import { Observable } from "rxjs";
import { ActiveEstateService } from "../../../core/services/active-estate.service";
import { BankAccountService } from "../../../core/services/bank-account.service";
import { BuildingService } from "../../../core/services/building.service";
import { HousingUnitService } from "../../../core/services/housing-unit.service";
import { LeaseService } from "../../../core/services/lease.service";
import { PersonService } from "../../../core/services/person.service";
import { TagSubcategoryService } from "../../../core/services/tag-subcategory.service";
import { TransactionService } from "../../../core/services/transaction.service";
import { Building } from "../../../models/building.model";
import { HousingUnit } from "../../../models/housing-unit.model";
import { LeaseSummary, LeaseTenant } from "../../../models/lease.model";
import {
  BankAccount,
  CreateTransactionRequest,
  FinancialTransaction,
  ManualTransactionStatus,
  TagSubcategory,
  TransactionAssetLink,
  TransactionRequestFields,
  TransactionStatus,
} from "../../../models/transaction.model";
import { AssetLinkEditorComponent } from "../components/asset-link-editor/asset-link-editor.component";

@Component({
  selector: "app-transaction-form",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AssetLinkEditorComponent,
    TranslocoModule,
  ],
  templateUrl: "./transaction-form.component.html",
  styleUrls: ["./transaction-form.component.scss"],
})
export class TransactionFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  readonly manualTransactionStatuses: readonly ManualTransactionStatus[] = [
    "DRAFT",
    "CONFIRMED",
  ];
  transactionId?: number;
  existing?: FinancialTransaction;
  loading = false;
  error: string | null = null;

  subcategories: TagSubcategory[] = [];
  bankAccounts: BankAccount[] = [];
  assetLinks: TransactionAssetLink[] = [];
  subcategorySearch = "";
  leaseAccountError: string | null = null;

  buildings: Building[] = [];
  units: HousingUnit[] = [];
  leases: LeaseSummary[] = [];
  private leaseLookupVersion = 0;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private tagSubcategoryService: TagSubcategoryService,
    private bankAccountService: BankAccountService,
    private buildingService: BuildingService,
    private housingUnitService: HousingUnitService,
    private leaseService: LeaseService,
    private personService: PersonService,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get("id");
    this.isEdit = id !== null;
    this.buildForm();
    this.loadRefData();

    if (id) {
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
    this.form.get("subcategoryId")?.valueChanges.subscribe((subcategoryId) => {
      const subcategory = this.subcategories.find(
        (item) => item.id === subcategoryId,
      );
      this.form.patchValue(
        { categoryId: subcategory?.categoryId ?? null },
        { emitEvent: false },
      );
    });

    this.form.get("direction")?.valueChanges.subscribe((direction) => {
      const selected = this.subcategories.find(
        (subcategory) =>
          subcategory.id === this.form.get("subcategoryId")?.value,
      );
      if (
        selected &&
        selected.direction !== "BOTH" &&
        selected.direction !== direction
      ) {
        this.form.patchValue(
          { categoryId: null, subcategoryId: null },
          { emitEvent: false },
        );
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

    this.form.get("leaseId")?.valueChanges.subscribe((leaseId) => {
      this.fillCounterpartyAccountFromLease(leaseId);
    });
  }

  buildForm(): void {
    const today = new Date().toISOString().split("T")[0];
    this.form = this.fb.group({
      direction: ["INCOME", Validators.required],
      transactionDate: [today, Validators.required],
      valueDate: [null],
      accountingMonth: [today.substring(0, 7), Validators.required],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      description: [null],
      externalReference: [null, Validators.maxLength(200)],
      counterpartyAccount: [null, Validators.maxLength(50)],
      bankAccountId: [null],
      categoryId: [null],
      subcategoryId: [null],
      leaseId: [null],
      housingUnitId: [null],
      buildingId: [null],
      ...(this.isEdit
        ? {}
        : { status: ["CONFIRMED", Validators.required] }),
    });
  }

  loadRefData(): void {
    this.tagSubcategoryService
      .getAll()
      .subscribe((subcategories) => (this.subcategories = subcategories));
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
        const patchAndFinish = () => {
          this.form.patchValue(
            {
              direction: tx.direction,
              transactionDate: tx.transactionDate,
              valueDate: tx.valueDate,
              accountingMonth: tx.accountingMonth.substring(0, 7),
              amount: tx.amount,
              description: tx.description,
              externalReference: tx.externalReference,
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

  onSubcategorySearch(event: Event): void {
    this.subcategorySearch = (event.target as HTMLInputElement).value;
  }

  onAssetSubcategoryPreFill(subcategoryId: number): void {
    if (!this.form.get("subcategoryId")?.value) {
      this.form.patchValue(
        {
          subcategoryId,
          categoryId: this.subcategories.find((item) => item.id === subcategoryId)
            ?.categoryId ?? null,
        },
        { emitEvent: false },
      );
    }
  }

  get filteredSubcategoryGroups(): {
    categoryName: string;
    subcategories: TagSubcategory[];
  }[] {
    const direction = this.form.get("direction")?.value;
    const query = this.subcategorySearch.trim().toLocaleLowerCase();
    const groups = new Map<string, TagSubcategory[]>();
    for (const subcategory of this.subcategories) {
      if (
        subcategory.direction !== "BOTH" &&
        subcategory.direction !== direction
      ) {
        continue;
      }
      if (
        query &&
        !subcategory.categoryName.toLocaleLowerCase().includes(query) &&
        !subcategory.name.toLocaleLowerCase().includes(query)
      ) {
        continue;
      }
      const group = groups.get(subcategory.categoryName) ?? [];
      group.push(subcategory);
      groups.set(subcategory.categoryName, group);
    }
    return Array.from(groups, ([categoryName, subcategories]) => ({
      categoryName,
      subcategories,
    }));
  }

  get currentStatus(): TransactionStatus {
    return this.existing?.status ?? "CONFIRMED";
  }

  private fillCounterpartyAccountFromLease(
    leaseId: number | string | null,
  ): void {
    const lookupVersion = ++this.leaseLookupVersion;
    this.leaseAccountError = null;
    if (
      leaseId == null ||
      String(leaseId).length === 0 ||
      this.hasCounterpartyAccount()
    ) {
      return;
    }

    this.leaseService.getById(Number(leaseId)).subscribe({
      next: (lease) => {
        if (!this.isCurrentLeaseLookup(lookupVersion, leaseId)) return;
        const tenant =
          lease.tenants.find((item: LeaseTenant) => item.role === "PRIMARY") ??
          lease.tenants[0];
        if (!tenant) return;

        this.personService.getPersonById(tenant.personId).subscribe({
          next: (person) => {
            if (
              !this.isCurrentLeaseLookup(lookupVersion, leaseId) ||
              this.hasCounterpartyAccount()
            ) {
              return;
            }
            const account =
              person.bankAccounts?.find((item) => item.primary) ??
              person.bankAccounts?.[0];
            if (account?.iban) {
              this.form.patchValue(
                { counterpartyAccount: account.iban },
                { emitEvent: false },
              );
            }
          },
          error: () => {
            if (this.isCurrentLeaseLookup(lookupVersion, leaseId)) {
              this.leaseAccountError =
                "TRANSACTION.LEASE_ACCOUNT_LOOKUP_FAILED";
            }
          },
        });
      },
      error: () => {
        if (this.isCurrentLeaseLookup(lookupVersion, leaseId)) {
          this.leaseAccountError = "TRANSACTION.LEASE_ACCOUNT_LOOKUP_FAILED";
        }
      },
    });
  }

  private hasCounterpartyAccount(): boolean {
    return !!this.form.get("counterpartyAccount")?.value?.trim();
  }

  private isCurrentLeaseLookup(
    lookupVersion: number,
    leaseId: number | string,
  ): boolean {
    return (
      lookupVersion === this.leaseLookupVersion &&
      String(this.form.get("leaseId")?.value) === String(leaseId)
    );
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const req: TransactionRequestFields = {
      direction: value.direction,
      transactionDate: value.transactionDate,
      accountingMonth: `${value.accountingMonth}-01`,
      amount: Number(value.amount),
      description: value.description || undefined,
      externalReference: value.externalReference || undefined,
      valueDate: value.valueDate || undefined,
      counterpartyAccount: value.counterpartyAccount || undefined,
      bankAccountId: value.bankAccountId ?? undefined,
      subcategoryId: value.subcategoryId ?? undefined,
      leaseId: value.leaseId ?? undefined,
      housingUnitId: value.housingUnitId ?? undefined,
      buildingId: value.buildingId ?? undefined,
      assetLinks: this.assetLinks,
    };
    let obs: Observable<FinancialTransaction>;
    if (this.isEdit) {
      obs = this.transactionService.update(this.transactionId!, req);
    } else {
      const status: unknown = value.status;
      if (status !== "DRAFT" && status !== "CONFIRMED") {
        this.form.get("status")?.setErrors({ invalid: true });
        return;
      }
      const createRequest: CreateTransactionRequest = { ...req, status };
      obs = this.transactionService.create(createRequest);
    }

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
