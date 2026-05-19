import { Injectable, signal } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";

export type SupportedLang = "fr" | "en" | "nl";
const STORAGE_KEY = "immocare_lang";

@Injectable({ providedIn: "root" })
export class LanguageService {
  readonly currentLang = signal<SupportedLang>("fr");
  readonly supported: SupportedLang[] = ["fr", "en", "nl"];

  constructor(private translate: TranslateService) {
    const saved = localStorage.getItem(STORAGE_KEY) as SupportedLang;
    const initial = saved ?? this.detectBrowserLang() ?? "fr";
    this.setLang(initial);
  }

  setLang(lang: SupportedLang): void {
    this.translate.use(lang);
    this.currentLang.set(lang);
    localStorage.setItem(STORAGE_KEY, lang);
    document.documentElement.lang = lang;
  }

  private detectBrowserLang(): SupportedLang | null {
    const browser = navigator.language.slice(0, 2);
    return this.supported.includes(browser as SupportedLang)
      ? (browser as SupportedLang)
      : null;
  }
}
