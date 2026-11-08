import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslocoModule } from "@ngneat/transloco";
import { TransactionDashboardComponent } from "../transaction-dashboard/transaction-dashboard.component";
import { TransactionImportComponent } from "../transaction-import/transaction-import.component";
import { TransactionListComponent } from "../transaction-list/transaction-list.component";

type Tab = "dashboard" | "list" | "import";

@Component({
  selector: "app-transactions-page",
  standalone: true,
  imports: [
    TransactionListComponent,
    TransactionImportComponent,
    TransactionDashboardComponent,
    TranslocoModule,
  ],
  templateUrl: "./transactions-page.component.html",
  styleUrls: ["./transactions-page.component.scss"],
})
export class TransactionsPageComponent implements OnInit {
  activeTab: Tab = "dashboard";
  readonly tabs: { id: Tab; label: string }[] = [
    { id: "dashboard", label: "GLOBAL.DASHBOARD" },
    { id: "list", label: "TRANSACTION.TRANSACTIONS" },
    { id: "import", label: "GLOBAL.IMPORT" },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}
  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const selectedTab = this.tabs.find(
        (tab) => tab.id === params.get("tab"),
      );
      if (selectedTab) {
        this.activeTab = selectedTab.id;
      }
    });
  }

  selectTab(tab: Tab): void {
    this.activeTab = tab;
    this.router.navigate([], {
      queryParams: { tab },
      queryParamsHandling: "merge",
    });
  }

  onTabKeydown(event: KeyboardEvent, currentTab: Tab): void {
    const currentIndex = this.tabs.findIndex((tab) => tab.id === currentTab);
    let nextIndex: number;

    switch (event.key) {
      case "ArrowRight":
        nextIndex = (currentIndex + 1) % this.tabs.length;
        break;
      case "ArrowLeft":
        nextIndex = (currentIndex - 1 + this.tabs.length) % this.tabs.length;
        break;
      case "Home":
        nextIndex = 0;
        break;
      case "End":
        nextIndex = this.tabs.length - 1;
        break;
      default:
        return;
    }

    event.preventDefault();
    const nextTab = this.tabs[nextIndex];
    this.selectTab(nextTab.id);
    (event.currentTarget as HTMLElement)
      .parentElement?.querySelector<HTMLElement>(
        `#transaction-tab-${nextTab.id}`,
      )
      ?.focus();
  }
}
