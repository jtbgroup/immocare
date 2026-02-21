import { HTTP_INTERCEPTORS, HttpClientModule } from "@angular/common/http";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";

import { AppComponent } from "./app.component";
import { AuthGuard } from "./core/auth/auth.guard";
import { AuthInterceptor } from "./core/auth/auth.interceptor";

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    HttpClientModule,
    RouterModule.forRoot([
      {
        path: "login",
        loadChildren: () =>
          import("./features/auth/auth.module").then((m) => m.AuthModule),
      },
      {
        path: "buildings",app
        canActivate: [AuthGuard],
        loadChildren: () =>
          import("./features/building/building.module").then(
            (m) => m.BuildingModule,
          ),
      },
      {
        path: "users",
        canActivate: [AuthGuard],
        loadChildren: () =>
          import("./features/user/user.module").then((m) => m.UserModule),
      },
      {
        path: "units",
        loadChildren: () =>
          import("./features/housing-unit/housing-unit.module").then(
            (m) => m.HousingUnitModule,
          ),
      },
      {
        path: "",
        redirectTo: "/buildings",
        pathMatch: "full",
      },
    ]),
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
