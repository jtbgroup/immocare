import { Injectable, signal } from "@angular/core";
import { PlatformConfigService } from "./platform-config.service";

@Injectable({ providedIn: "root" })
export class AppNameService {
  readonly appName = signal("ImmoCare");

  constructor(private platformConfigService: PlatformConfigService) {}

  load(): Promise<void> {
    return new Promise((resolve) => {
      this.platformConfigService.getOne("app_name").subscribe({
        next: (cfg) => {
          if (cfg?.configValue) this.appName.set(cfg.configValue);
          resolve();
        },
        error: () => resolve(), // fallback silencieux
      });
    });
  }
}
