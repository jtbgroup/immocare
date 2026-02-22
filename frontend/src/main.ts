import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { bootstrapApplication } from "@angular/platform-browser";
import { provideRouter } from "@angular/router";

import { AppComponent } from "./app/app.component";
import { AuthGuard } from "./app/core/auth/auth.guard";
import { AuthInterceptor } from "./app/core/auth/auth.interceptor";

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    provideRouter([
      {
        path: "login",
        loadComponent: () =>
          import("./app/features/auth/login/login.component").then(
            (m) => m.LoginComponent,
          ),
      },
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
        path: "buildings/:id",
        canActivate: [AuthGuard],
        loadComponent: () =>
          import("./app/features/building/components/building-details/building-details.component").then(
            (m) => m.BuildingDetailsComponent,
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
      {
        path: "units/new",
        loadComponent: () =>
          import("./app/features/housing-unit/components/housing-unit-form/housing-unit-form.component").then(
            (m) => m.HousingUnitFormComponent,
          ),
      },
      {
        path: "units/:id",
        loadComponent: () =>
          import("./app/features/housing-unit/components/housing-unit-details/housing-unit-details.component").then(
            (m) => m.HousingUnitDetailsComponent,
          ),
      },
      {
        path: "units/:id/edit",
        loadComponent: () =>
          import("./app/features/housing-unit/components/housing-unit-form/housing-unit-form.component").then(
            (m) => m.HousingUnitFormComponent,
          ),
      },
      {
        path: "",
        redirectTo: "/buildings",
        pathMatch: "full",
      },
      {
        path: "**",
        redirectTo: "/buildings",
      },
    ]),
  ],
}).catch((err) => console.error(err));
