import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  ApplicationConfig,
  inject,
  isDevMode,
  provideAppInitializer,
} from "@angular/core";
import { provideAnimations } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTransloco } from "@ngneat/transloco";
import { TranslocoHttpLoader } from "../transloco-loader";
import { routes } from "./app.routes";
import { AuthInterceptor } from "./core/auth/auth.interceptor";
import { AppNameService } from "./core/services/app-name.service";
import { DateFormatService } from "./core/services/date-format.service";

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),

    // Configuration Moderne du Multilingue (Bye bye les modules !)
    provideTransloco({
      config: {
        availableLangs: ["fr", "en"],
        defaultLang: "en",
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    provideAppInitializer(() => {
      const svc = inject(AppNameService);
      return svc.load();
    }),
    provideAppInitializer(() => {
      const svc = inject(DateFormatService);
      return svc.load();
    }),
  ],
};
