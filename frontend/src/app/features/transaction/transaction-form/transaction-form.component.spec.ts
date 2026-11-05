import { TestBed } from "@angular/core/testing";
import { ActivatedRoute, provideRouter, Router } from "@angular/router";
import { FormBuilder } from "@angular/forms";
import { of } from "rxjs";
import { ActiveEstateService } from "../../../core/services/active-estate.service";
import { BankAccountService } from "../../../core/services/bank-account.service";
import { BuildingService } from "../../../core/services/building.service";
import { HousingUnitService } from "../../../core/services/housing-unit.service";
import { LeaseService } from "../../../core/services/lease.service";
import { PersonService } from "../../../core/services/person.service";
import { TagSubcategoryService } from "../../../core/services/tag-subcategory.service";
import { TransactionService } from "../../../core/services/transaction.service";
import { Lease } from "../../../models/lease.model";
import { Person } from "../../../models/person.model";
import { TagSubcategory } from "../../../models/transaction.model";
import { TransactionFormComponent } from "./transaction-form.component";

describe("TransactionFormComponent", () => {
  let component: TransactionFormComponent;
  let leaseService: jasmine.SpyObj<LeaseService>;
  let personService: jasmine.SpyObj<PersonService>;

  beforeEach(() => {
    const transactionService = jasmine.createSpyObj<TransactionService>(
      "TransactionService",
      ["create", "update", "getById"],
    );
    const tagSubcategoryService =
      jasmine.createSpyObj<TagSubcategoryService>("TagSubcategoryService", [
        "getAll",
      ]);
    tagSubcategoryService.getAll.and.returnValue(of([]));
    const bankAccountService = jasmine.createSpyObj<BankAccountService>(
      "BankAccountService",
      ["getAll"],
    );
    bankAccountService.getAll.and.returnValue(of([]));
    const buildingService = jasmine.createSpyObj<BuildingService>(
      "BuildingService",
      ["getAllBuildings"],
    );
    buildingService.getAllBuildings.and.returnValue(
      of({
        content: [],
        page: 0,
        size: 200,
        totalElements: 0,
        totalPages: 0,
      }),
    );
    const housingUnitService = jasmine.createSpyObj<HousingUnitService>(
      "HousingUnitService",
      ["getUnitsByBuilding"],
    );
    housingUnitService.getUnitsByBuilding.and.returnValue(of([]));
    leaseService = jasmine.createSpyObj<LeaseService>("LeaseService", [
      "getById",
      "getByUnit",
    ]);
    leaseService.getByUnit.and.returnValue(of([]));
    personService = jasmine.createSpyObj<PersonService>("PersonService", [
      "getPersonById",
    ]);
    const router = jasmine.createSpyObj<Router>("Router", ["navigate"]);
    const activeEstateService = jasmine.createSpyObj<ActiveEstateService>(
      "ActiveEstateService",
      ["activeEstateId"],
    );
    activeEstateService.activeEstateId.and.returnValue(null);

    TestBed.configureTestingModule({
      providers: [FormBuilder, provideRouter([])],
    });

    component = new TransactionFormComponent(
      TestBed.inject(FormBuilder),
      TestBed.inject(ActivatedRoute),
      router,
      transactionService,
      tagSubcategoryService,
      bankAccountService,
      buildingService,
      housingUnitService,
      leaseService,
      personService,
      activeEstateService,
    );
    component.ngOnInit();
  });

  it("groups category and subcategory in one searchable selection", () => {
    const subcategories: TagSubcategory[] = [
      {
        id: 1,
        categoryId: 10,
        categoryName: "Maintenance",
        name: "Boiler service",
        direction: "EXPENSE",
        usageCount: 0,
      },
      {
        id: 2,
        categoryId: 20,
        categoryName: "Income",
        name: "Rent",
        direction: "INCOME",
        usageCount: 0,
      },
    ];
    component.subcategories = subcategories;
    component.form.patchValue({ direction: "EXPENSE" });
    component.subcategorySearch = "boiler";

    expect(component.filteredSubcategoryGroups).toEqual([
      {
        categoryName: "Maintenance",
        subcategories: [subcategories[0]],
      },
    ]);

    component.form.get("subcategoryId")?.setValue(1);
    expect(component.form.get("categoryId")?.value).toBe(10);
  });

  it("fills an empty counterparty account from the primary tenant's primary bank account", () => {
    leaseService.getById.and.returnValue(
      of({
        tenants: [{ personId: 42, role: "PRIMARY" }],
      } as Lease),
    );
    const person: Person = {
      id: 42,
      firstName: "Alex",
      lastName: "Tenant",
      owner: false,
      tenant: true,
      bankAccounts: [
        {
          id: 1,
          personId: 42,
          iban: "BE0000000001",
          primary: false,
          createdAt: "2026-01-01T00:00:00Z",
        },
        {
          id: 2,
          personId: 42,
          iban: "BE0000000002",
          primary: true,
          createdAt: "2026-01-01T00:00:00Z",
        },
      ],
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };
    personService.getPersonById.and.returnValue(of(person));

    component.form.patchValue({ leaseId: 7 });

    expect(component.form.get("counterpartyAccount")?.value).toBe(
      "BE0000000002",
    );
  });

  it("does not replace a counterparty account already entered by the user", () => {
    component.form.patchValue({ counterpartyAccount: "BE0000000099" });

    component.form.patchValue({ leaseId: 7 });

    expect(leaseService.getById).not.toHaveBeenCalled();
    expect(component.form.get("counterpartyAccount")?.value).toBe(
      "BE0000000099",
    );
  });
});
