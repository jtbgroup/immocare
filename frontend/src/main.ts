import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { APP_INITIALIZER } from "@angular/core";
import { bootstrapApplication } from "@angular/platform-browser";
import { provideRouter } from "@angular/router";

import { AppComponent } from "./app/app.component";
import { AuthGuard } from "./app/core/auth/auth.guard";
import { AuthInterceptor } from "./app/core/auth/auth.interceptor";
import { DateFormatService } from "./app/core/services/date-format.service";

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    // ─── Load app.date_format before any component renders ────────────────────
    {
      provide: APP_INITIALIZER,
      useFactory: (svc: DateFormatService) => () => svc.load(),
      deps: [DateFormatService],
      multi: true,
    },
    provideRouter([
      {
        path: "login",
        loadComponent: () =>
          import("./app/features/auth/login/components/login.component").then(
            (m) => m.LoginComponent,
          ),
      },
      // ─── Persons ────────────────────────────────────────────────────────────
      {
        path: "persons",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/person/components/person-list/person-list.component").then(
            (m) => m.PersonListComponent,
          ),
      },
      {
        path: "persons/new",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/person/components/person-form/person-form.component").then(
            (m) => m.PersonFormComponent,
          ),
      },
      {
        path: "persons/:id/edit",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/person/components/person-form/person-form.component").then(
            (m) => m.PersonFormComponent,
          ),
      },
      {
        path: "persons/:id",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/person/components/person-details/person-details.component").then(
            (m) => m.PersonDetailsComponent,
          ),
      },
      // ─── Buildings ──────────────────────────────────────────────────────────
      {
        path: "buildings",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/building/components/building-list/building-list.component").then(
            (m) => m.BuildingListComponent,
          ),
      },
      {
        path: "buildings/new",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/building/components/building-form/building-form.component").then(
            (m) => m.BuildingFormComponent,
          ),
      },
      {
        path: "buildings/:id/edit",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/building/components/building-form/building-form.component").then(
            (m) => m.BuildingFormComponent,
          ),
      },
      {
        path: "buildings/:id",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/building/components/building-details/building-details.component").then(
            (m) => m.BuildingDetailsComponent,
          ),
      },
      // ─── Housing Units ──────────────────────────────────────────────────────
      {
        path: "units/new",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/housing-unit/components/housing-unit-form/housing-unit-form.component").then(
            (m) => m.HousingUnitFormComponent,
          ),
      },
      {
        path: "units/:id/edit",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/housing-unit/components/housing-unit-form/housing-unit-form.component").then(
            (m) => m.HousingUnitFormComponent,
          ),
      },
      {
        path: "units/:id",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/housing-unit/components/housing-unit-details/housing-unit-details.component").then(
            (m) => m.HousingUnitDetailsComponent,
          ),
      },
      {
        path: "units",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/housing-unit/components/unit-global-list/unit-global-list.component").then(
            (m) => m.UnitGlobalListComponent,
          ),
      },
      // ─── Leases (UC010) ─────────────────────────────────────────────────────
      {
        path: "housing-units/:unitId/leases/new",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/lease/components/lease-form/lease-form.component").then(
            (m) => m.LeaseFormComponent,
          ),
      },
      {
        path: "leases",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/lease/components/lease-global-list/lease-global-list.component").then(
            (m) => m.LeaseGlobalListComponent,
          ),
      },
      {
        path: "leases/:id/edit",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/lease/components/lease-form/lease-form.component").then(
            (m) => m.LeaseFormComponent,
          ),
      },
      {
        path: "leases/:id",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/lease/components/lease-details/lease-details.component").then(
            (m) => m.LeaseDetailsComponent,
          ),
      },
      // ─── Transactions (UC014) ────────────────────────────────────────────────
      {
        path: "transactions",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/transaction/components/transactions-page/transactions-page.component").then(
            (m) => m.TransactionsPageComponent,
          ),
      },
      {
        path: "transactions/new",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/transaction/components/transaction-form/transaction-form.component").then(
            (m) => m.TransactionFormComponent,
          ),
      },
      {
        path: "transactions/import/:batchId",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/transaction/components/transaction-review/transaction-review.component").then(
            (m) => m.TransactionReviewComponent,
          ),
      },
      {
        path: "transactions/:id/edit",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/transaction/components/transaction-form/transaction-form.component").then(
            (m) => m.TransactionFormComponent,
          ),
      },
      {
        path: "transactions/:id",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/transaction/components/transaction-detail/transaction-detail.component").then(
            (m) => m.TransactionDetailComponent,
          ),
      },
      // ─── Users ──────────────────────────────────────────────────────────────
      {
        path: "users",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/user/components/user-list/user-list.component").then(
            (m) => m.UserListComponent,
          ),
      },
      {
        path: "users/new",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/user/components/user-form/user-form.component").then(
            (m) => m.UserFormComponent,
          ),
      },
      {
        path: "users/:id",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/user/components/user-details/user-details.component").then(
            (m) => m.UserDetailsComponent,
          ),
      },
      {
        path: "users/:id/edit",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/user/components/user-form/user-form.component").then(
            (m) => m.UserFormComponent,
          ),
      },
      // ─── Settings ───────────────────────────────────────────────────────────
      {
        path: "settings",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/settings/components/settings/settings.component").then(
            (m) => m.SettingsComponent,
          ),
      },
      // ─── Alerts ─────────────────────────────────────────────────────────────
      {
        path: "alerts",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/alerts/components/alerts/alerts.component").then(
            (m) => m.AlertsComponent,
          ),
      },
      // ─── Fallback ───────────────────────────────────────────────────────────
      { path: "", redirectTo: "/buildings", pathMatch: "full" },
      { path: "**", redirectTo: "/buildings" },
    ]),
  ],
}).catch((err) => console.error(err));
