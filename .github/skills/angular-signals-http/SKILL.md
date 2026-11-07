---
name: angular-signals-http
description: "Apply the project's Angular Signals, RxJS, and HttpClient conventions when implementing or maintaining frontend service state, loading/error handling, or HTTP-backed components."
---

# Angular Signals and HTTP Management

## State convention

Signals are the standard for new Angular application and component state. Use
`signal()` for writable state and `computed()` for derived state. Use `effect()` only
for side effects that cannot be expressed by templates or computed values.

The current application contains legacy RxJS-driven state. Preserve existing
behavior during feature changes, but do not introduce more RxJS-owned component
state unless required by an observable integration. HTTP APIs may continue to return
`Observable<T>`; bridge responses into signals at the component/service state
boundary and handle loading and error state explicitly.

Prefer functional `inject()` in new code. Constructor injection is present in
existing code and does not need incidental conversion.

## HTTP and error handling

- Keep loading and error state explicit.
- Follow established UI error and localization patterns; do not swallow errors or
  log-and-continue.
- Keep models and HTTP services synchronized with backend DTOs and estate-scoped
  routes.
- Keep user-facing text in Transloco resources and update both
  `frontend/src/assets/i18n/en.json` and `frontend/src/assets/i18n/fr.json`.

For API field mapping, route, HTTP verb, and authorization checks, also load the
[`api-contract-sync`](../api-contract-sync/SKILL.md) skill.
