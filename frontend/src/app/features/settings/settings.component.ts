// features/settings/components/settings/settings.component.ts
// Platform-wide settings — PLATFORM_ADMIN only.
// Layout mirrors estate-platform-settings; content is different (global keys only).
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Subject, forkJoin } from "rxjs";
import { finalize, takeUntil } from "rxjs/operators";

import { TranslocoModule } from "@ngneat/transloco";
import { PlatformConfigService } from "../../core/services/platform-config.service";
import {
  PLATFORM_CONFIG_LABELS,
  PlatformConfigDTO,
} from "../../models/platform-config.model";

type Section = "general";

@Component({
  selector: "app-settings",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  templateUrl: "./settings.component.html",
  styleUrls: ["./settings.component.scss"],
})
export class SettingsComponent implements OnInit, OnDestroy {
  // ── State ─────────────────────────────────────────────────────────────────
  configs: PlatformConfigDTO[] = [];
  loading = false;
  saving = false;
  error: string | null = null;
  successMessage: string | null = null;
  form!: FormGroup;
  activeSection: Section = "general";

  // ── Constants exposed to template ─────────────────────────────────────────
  readonly PLATFORM_CONFIG_LABELS = PLATFORM_CONFIG_LABELS;
  readonly sections: { id: Section; label: string; icon: string }[] = [
    { id: "general", label: "Général", icon: "🌐" },
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private configService: PlatformConfigService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
  ) {}

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const s = this.route.snapshot.queryParamMap.get("section") as Section;
    if (s && this.sections.find((sec) => sec.id === s)) this.activeSection = s;
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Navigation ────────────────────────────────────────────────────────────

  setSection(section: Section): void {
    this.activeSection = section;
    this.router.navigate([], { queryParams: { section }, replaceUrl: true });
  }

  // ── Data ──────────────────────────────────────────────────────────────────

  load(): void {
    this.loading = true;
    this.error = null;
    this.configService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (configs) => {
          this.configs = configs;
          this.buildForm(configs);
          this.loading = false;
        },
        error: () => {
          this.error = "Impossible de charger les paramètres.";
          this.loading = false;
        },
      });
  }

  buildForm(configs: PlatformConfigDTO[]): void {
    const group: Record<string, unknown[]> = {};
    for (const c of configs) {
      group[c.configKey] = [c.configValue, Validators.required];
    }
    this.form = this.fb.group(group);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = null;
    this.successMessage = null;

    const updates = Object.entries(this.form.value).map(([key, value]) =>
      this.configService.updateOne(key, { configValue: String(value).trim() }),
    );

    forkJoin(updates)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.saving = false)),
      )
      .subscribe({
        next: () => {
          this.successMessage = "Paramètres enregistrés.";
          setTimeout(() => (this.successMessage = null), 3000);
          this.load();
        },
        error: (err) => {
          this.error =
            err?.error?.message ?? "Erreur lors de l'enregistrement.";
        },
      });
  }

  // ── Template helpers ──────────────────────────────────────────────────────

  get generalConfigs(): PlatformConfigDTO[] {
    return this.configs;
  }

  labelFor(key: string): string {
    return PLATFORM_CONFIG_LABELS[key] ?? key;
  }

  fieldError(key: string): string | null {
    const ctrl = this.form?.get(key);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return null;
    if (ctrl.hasError("required")) return "Ce champ est obligatoire.";
    return "Valeur invalide.";
  }
}
