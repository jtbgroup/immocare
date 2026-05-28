import { Routes } from "@angular/router";
import { AuthGuard } from "./core/auth/auth.guard";
import { EstateGuard } from "./core/auth/estate.guard";
import { PlatformAdminGuard } from "./core/auth/platform-admin.guard";
import { AdminEstateFormComponent } from "./features/estate/admin/admin-estate-form/admin-estate-form.component";

export const routes: Routes = [
  {
    path: "login",
    loadComponent: () =>
      import("./features/auth/login/login.component").then(
        (m) => m.LoginComponent,
      ),
  },

  // ─── Estate selector ─────────────────────────────────────────────────────
  {
    path: "select-estate",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/estate/user/estate-selector/estate-selector.component").then(
        (m) => m.EstateSelectorComponent,
      ),
  },

  // ─── Admin: Estates (PLATFORM_ADMIN only) ────────────────────────────────
  {
    path: "admin/estates/new",
    canActivate: [AuthGuard, PlatformAdminGuard],
    loadComponent: () =>
      import("./features/estate/admin/admin-estate-form/admin-estate-form.component").then(
        (m) => m.AdminEstateFormComponent,
      ),
  },
  {
    path: "admin/estates/:id/edit",
    canActivate: [AuthGuard, PlatformAdminGuard],
    loadComponent: () =>
      import("./features/estate/admin/admin-estate-form/admin-estate-form.component").then(
        (m) => m.AdminEstateFormComponent,
      ),
  },
  {
    path: "estates/:estateId/edit",
    component: AdminEstateFormComponent,
    canActivate: [EstateGuard],
    data: { requiresManager: true },
  },
  {
    path: "admin/estates",
    pathMatch: "full",
    canActivate: [AuthGuard, PlatformAdminGuard],
    loadComponent: () =>
      import("./features/estate/admin/admin-estate-list/admin-estate-list.component").then(
        (m) => m.AdminEstateListComponent,
      ),
  },

  // ─── Estate dashboard ────────────────────────────────────────────────────
  {
    path: "estates/:estateId/dashboard",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/estate/user/estate-dashboard/estate-dashboard.component").then(
        (m) => m.EstateDashboardComponent,
      ),
  },

  // ─── Estate members ──────────────────────────────────────────────────────
  {
    path: "estates/:estateId/members",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/estate/user/estate-member-list/estate-member-list.component").then(
        (m) => m.EstateMemberListComponent,
      ),
  },

  // ─── Estate settings (Phase 5) ───────────────────────────────────────────
  {
    path: "estates/:estateId/settings",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/estate/user/estate-settings/estate-settings.component").then(
        (m) => m.EstateSettingsComponent,
      ),
  },

  // ─── Buildings (Phase 2: estate-scoped) ──────────────────────────────────
  {
    path: "estates/:estateId/buildings",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/building/building-list/building-list.component").then(
        (m) => m.BuildingListComponent,
      ),
  },
  {
    path: "estates/:estateId/buildings/new",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/building/building-form/building-form.component").then(
        (m) => m.BuildingFormComponent,
      ),
  },
  {
    path: "estates/:estateId/buildings/:id/edit",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/building/building-form/building-form.component").then(
        (m) => m.BuildingFormComponent,
      ),
  },
  {
    path: "estates/:estateId/buildings/:id",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/building/building-details/building-details.component").then(
        (m) => m.BuildingDetailsComponent,
      ),
  },

  // ─── Housing Units (Phase 2: estate-scoped) ───────────────────────────────
  {
    path: "estates/:estateId/units",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/housing-unit/housing-unit-global-list/housing-unit-global-list.component").then(
        (m) => m.UnitGlobalListComponent,
      ),
  },
  {
    path: "estates/:estateId/units/new",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/housing-unit/housing-unit-form/housing-unit-form.component").then(
        (m) => m.HousingUnitFormComponent,
      ),
  },
  {
    path: "estates/:estateId/units/:id/edit",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/housing-unit/housing-unit-form/housing-unit-form.component").then(
        (m) => m.HousingUnitFormComponent,
      ),
  },
  {
    path: "estates/:estateId/units/:id",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/housing-unit/housing-unit-details/housing-unit-details.component").then(
        (m) => m.HousingUnitDetailsComponent,
      ),
  },

  // ─── Persons (Phase 3: estate-scoped) ────────────────────────────────────
  {
    path: "estates/:estateId/persons",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/person/person-list/person-list.component").then(
        (m) => m.PersonListComponent,
      ),
  },
  {
    path: "estates/:estateId/persons/new",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/person/person-form/person-form.component").then(
        (m) => m.PersonFormComponent,
      ),
  },
  {
    path: "estates/:estateId/persons/:id/edit",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/person/person-form/person-form.component").then(
        (m) => m.PersonFormComponent,
      ),
  },
  {
    path: "estates/:estateId/persons/:id",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/person/person-details/person-details.component").then(
        (m) => m.PersonDetailsComponent,
      ),
  },

  // ─── Leases (Phase 3: estate-scoped) ─────────────────────────────────────
  {
    path: "estates/:estateId/housing-units/:unitId/leases/new",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/lease/lease-form/lease-form.component").then(
        (m) => m.LeaseFormComponent,
      ),
  },
  {
    path: "estates/:estateId/leases",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/lease/lease-global-list/lease-global-list.component").then(
        (m) => m.LeaseGlobalListComponent,
      ),
  },
  {
    path: "estates/:estateId/leases/:id/edit",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/lease/lease-form/lease-form.component").then(
        (m) => m.LeaseFormComponent,
      ),
  },
  {
    path: "estates/:estateId/leases/:id",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/lease/lease-details/lease-details.component").then(
        (m) => m.LeaseDetailsComponent,
      ),
  },

  // ─── Transactions (Phase 4: estate-scoped) ───────────────────────────────
  {
    path: "estates/:estateId/transactions",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/transaction/transactions-page/transactions-page.component").then(
        (m) => m.TransactionsPageComponent,
      ),
  },
  {
    path: "estates/:estateId/transactions/new",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-form/transaction-form.component").then(
        (m) => m.TransactionFormComponent,
      ),
  },
  {
    path: "estates/:estateId/transactions/import/:batchId",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-review/transaction-review.component").then(
        (m) => m.TransactionReviewComponent,
      ),
  },
  {
    path: "estates/:estateId/transactions/:id/edit",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-form/transaction-form.component").then(
        (m) => m.TransactionFormComponent,
      ),
  },
  {
    path: "estates/:estateId/transactions/:id",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-detail/transaction-detail.component").then(
        (m) => m.TransactionDetailComponent,
      ),
  },

  // ─── Transactions (legacy flat routes) ───────────────────────────────────
  {
    path: "transactions",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/transaction/transactions-page/transactions-page.component").then(
        (m) => m.TransactionsPageComponent,
      ),
  },
  {
    path: "transactions/new",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-form/transaction-form.component").then(
        (m) => m.TransactionFormComponent,
      ),
  },
  {
    path: "transactions/import/:batchId",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-review/transaction-review.component").then(
        (m) => m.TransactionReviewComponent,
      ),
  },
  {
    path: "transactions/:id/edit",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-form/transaction-form.component").then(
        (m) => m.TransactionFormComponent,
      ),
  },
  {
    path: "transactions/:id",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/transaction/transaction-detail/transaction-detail.component").then(
        (m) => m.TransactionDetailComponent,
      ),
  },

  // ─── Users ───────────────────────────────────────────────────────────────
  {
    path: "users",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/user/user-list/user-list.component").then(
        (m) => m.UserListComponent,
      ),
  },
  {
    path: "users/new",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/user/user-form/user-form.component").then(
        (m) => m.UserFormComponent,
      ),
  },
  {
    path: "users/:id",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/user/user-details/user-details.component").then(
        (m) => m.UserDetailsComponent,
      ),
  },
  {
    path: "users/:id/edit",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/user/user-form/user-form.component").then(
        (m) => m.UserFormComponent,
      ),
  },

  // ─── Management ──────────────────────────────────────────────────────────
  {
    path: "bank-accounts",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/bank-accounts/bank-accounts.component").then(
        (m) => m.BankAccountsComponent,
      ),
  },

  // ─── Legacy global settings ──────────────────────────────────────────────
  {
    path: "settings",
    canActivate: [AuthGuard],
    loadComponent: () =>
      import("./features/settings/settings.component").then(
        (m) => m.SettingsComponent,
      ),
  },

  // ─── Alerts ──────────────────────────────────────────────────────────────
  {
    path: "estates/:estateId/alerts",
    canActivate: [AuthGuard, EstateGuard],
    loadComponent: () =>
      import("./features/alerts/alerts.component").then(
        (m) => m.AlertsComponent,
      ),
  },

  // ─── Fallback ────────────────────────────────────────────────────────────
  { path: "", redirectTo: "/login", pathMatch: "full" },
  { path: "**", redirectTo: "/login" },
];
