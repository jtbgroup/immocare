import {
  ActivatedRoute,
  convertToParamMap,
  ParamMap,
  Router,
} from "@angular/router";
import { Subject } from "rxjs";
import { TransactionsPageComponent } from "./transactions-page.component";

describe("TransactionsPageComponent", () => {
  let queryParams: Subject<ParamMap>;
  let router: jasmine.SpyObj<Router>;
  let component: TransactionsPageComponent;

  beforeEach(() => {
    queryParams = new Subject<ParamMap>();
    router = jasmine.createSpyObj<Router>("Router", ["navigate"]);
    const route = {
      queryParamMap: queryParams.asObservable(),
    } as ActivatedRoute;

    component = new TransactionsPageComponent(route, router);
  });

  it("opens the dashboard by default and keeps dashboard, list, and import views available", () => {
    component.ngOnInit();

    expect(component.activeTab).toBe("dashboard");
    expect(component.tabs.map((tab) => tab.id)).toEqual([
      "dashboard",
      "list",
      "import",
    ]);
  });

  it("restores the selected view from the tab query parameter", () => {
    component.ngOnInit();
    queryParams.next(convertToParamMap({ tab: "import" }));
    expect(component.activeTab).toBe("import");
  });

  it("supports arrow-key navigation between tabs", () => {
    const tablist = document.createElement("div");
    const dashboardTab = document.createElement("button");
    const listTab = document.createElement("button");
    listTab.id = "transaction-tab-list";
    tablist.append(dashboardTab, listTab);
    const focusSpy = spyOn(listTab, "focus");
    const event = new KeyboardEvent("keydown", {
      key: "ArrowRight",
      cancelable: true,
    });
    Object.defineProperty(event, "currentTarget", { value: dashboardTab });

    component.onTabKeydown(event, "dashboard");

    expect(event.defaultPrevented).toBeTrue();
    expect(component.activeTab).toBe("list");
    expect(router.navigate).toHaveBeenCalledWith(
      [],
      jasmine.objectContaining({ queryParams: { tab: "list" } }),
    );
    expect(focusSpy).toHaveBeenCalled();
  });
});
