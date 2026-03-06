import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { TransactionDashboardComponent } from "../transaction-dashboard/transaction-dashboard.component";
import { TransactionImportComponent } from "../transaction-import/transaction-import.component";
import { TransactionListComponent } from "../transaction-list/transaction-list.component";
import { TransactionSettingsComponent } from "../transaction-settings/transaction-settings.component";

type Tab = "list" | "import" | "dashboard" | "settings";

@Component({
  selector: "app-transactions-page",
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TransactionListComponent,
    TransactionImportComponent,
    TransactionDashboardComponent,
    TransactionSettingsComponent,
  ],
  templateUrl: "./transactions-page.component.html",
  // styleUrls: ["./transactions-page.component.scss"],
})
export class TransactionsPageComponent implements OnInit {
  activeTab: Tab = "list";

  tabs: { id: Tab; label: string }[] = [
    { id: "list", label: "Transactions" },
    { id: "import", label: "Import" },
    { id: "dashboard", label: "Dashboard" },
    { id: "settings", label: "Settings" },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const tab = params.get("tab") as Tab;
      if (tab && this.tabs.find((t) => t.id === tab)) {
        this.activeTab = tab;
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
}
