import { bootstrapApplication } from "@angular/platform-browser";

// CORRECTION 1 : Ajout des imports manquants pour ngx-translate

import { AppComponent } from "./app/app.component";
import { appConfig } from "./app/app.config";

bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error(err),
);
