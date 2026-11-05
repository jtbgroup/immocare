import { TestBed } from "@angular/core/testing";
import { BuildingService } from "../../../../core/services/building.service";
import { LeaseService } from "../../../../core/services/lease.service";
import { TagSubcategoryService } from "../../../../core/services/tag-subcategory.service";
import { Building } from "../../../../models/building.model";
import {
  ImportPreviewRow,
  SaveAssetLinkRequest,
} from "../../../../models/transaction.model";
import { ImportRowDetailPanelComponent } from "./import-row-detail-panel.component";

describe("ImportRowDetailPanelComponent", () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: TagSubcategoryService, useValue: {} },
        { provide: BuildingService, useValue: {} },
        { provide: LeaseService, useValue: {} },
      ],
    });
  });

  it("links the selected building without a lease or housing unit", () => {
    const component = TestBed.runInInjectionContext(
      () => new ImportRowDetailPanelComponent(),
    );
    const building: Building = {
      id: 7,
      estateId: "estate-1",
      name: "Main building",
      streetAddress: "1 Main Street",
      postalCode: "1000",
      city: "Brussels",
      country: "Belgium",
      unitCount: 2,
    };
    const row: ImportPreviewRow = {
      rowNumber: 1,
      rawLine: "transaction",
      transactionDate: null,
      amount: null,
      direction: null,
      description: null,
      counterpartyAccount: null,
      fingerprint: "fingerprint",
      duplicateInDb: false,
      suggestedSubcategory: null,
      suggestedLease: null,
      parseError: null,
      selected: true,
      enrichedLeaseId: 12,
      enrichedUnitId: 5,
      enrichedUnitNumber: "A",
    };
    component.row = row;
    component.buildings = [building];
    const emit = spyOn(component.rowChanged, "emit");

    component.onBuildingFilterChange(building.id);

    expect(row.enrichedBuildingId).toBe(building.id);
    expect(row.enrichedBuildingName).toBe(building.name);
    expect(row.enrichedLeaseId).toBeUndefined();
    expect(row.enrichedUnitId).toBeUndefined();
    expect(row.enrichedUnitNumber).toBeUndefined();
    expect(emit).toHaveBeenCalledWith(jasmine.objectContaining({
      enrichedBuildingId: building.id,
      enrichedLeaseId: undefined,
      enrichedUnitId: undefined,
    }));
  });

  it("stores multiple asset links on the imported row", () => {
    const component = TestBed.runInInjectionContext(
      () => new ImportRowDetailPanelComponent(),
    );
    const row = {
      rowNumber: 2,
      fingerprint: "fingerprint",
      enrichedAssetLinks: undefined,
    } as ImportPreviewRow;
    const assetLinks: SaveAssetLinkRequest[] = [
      { assetType: "BOILER", assetId: 10, amount: 80 },
      { assetType: "BOILER", assetId: 20, amount: 120 },
    ];
    component.row = row;

    component.onAssetLinksChange(assetLinks);

    expect(row.enrichedAssetLinks).toEqual(assetLinks);
  });
});
