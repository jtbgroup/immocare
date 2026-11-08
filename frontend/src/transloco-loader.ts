import { inject, Injectable } from "@angular/core";
import { Translation, TranslocoLoader } from "@ngneat/transloco";
import { HttpClient } from "@angular/common/http";
import { forkJoin, map } from "rxjs";
@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
    private http = inject(HttpClient);
    getTranslation(lang: string) {
        return forkJoin({
            translations: this.http.get<Translation>(`/assets/i18n/${lang}.json`),
            sharedLabels: this.http.get<Translation>(`/assets/i18n/${lang}.shared.json`),
        }).pipe(
            map(({ translations, sharedLabels }) => {
                const globalTranslations = translations["GLOBAL"] ?? {};
                const globalSharedLabels = sharedLabels["GLOBAL"] ?? {};
                return {
                    ...translations,
                    GLOBAL: {
                        ...globalTranslations,
                        ...globalSharedLabels,
                        FILTER: {
                            ...(globalTranslations["FILTER"] ?? {}),
                            ...(globalSharedLabels["FILTER"] ?? {}),
                        },
                    },
                };
            }),
        );
    }
}
