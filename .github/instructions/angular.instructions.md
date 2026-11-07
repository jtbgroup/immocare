---
description: "Use when creating or changing Angular 19+ frontend TypeScript, templates, styles, or tests."
name: "Angular frontend conventions"
applyTo: ["frontend/**/*.ts", "frontend/**/*.html", "frontend/**/*.scss", "frontend/e2e/**/*.ts"]
---

# Angular frontend conventions

- Target Angular 19+ and use standalone components for new UI.
- Use Angular built-in template control flow (`@if`, `@for`, `@switch`); do not
  introduce `*ngIf` or `*ngFor`.
- Lazy-load feature routes/components through Angular standalone routing APIs rather
  than introducing feature NgModules.
- Use signals for new application or component state; preserve adjacent legacy RxJS
  patterns and use observables at HTTP boundaries where appropriate.
- Prefer functional `inject()` in new code and keep all data strictly typed; do not
  introduce `any`.
- Localize user-facing text with Transloco and update both English and French
  resources.
- Keep API models and services aligned with backend DTO fields, nullability, routes,
  and estate scope.
- Put Playwright end-to-end tests under `frontend/e2e/`; keep unit tests with their
  related frontend source files.
- Load [`angular-signals-http`](../skills/angular-signals-http/SKILL.md) for
  signal/HTTP state patterns and [`api-contract-sync`](../skills/api-contract-sync/SKILL.md)
  when changing API models or clients.
