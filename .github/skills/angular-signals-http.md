# Skill: Angular Signals & HTTP Management

## State Convention

Signals are the standard for new Angular application and component state. Use `signal()` for writable state and `computed()` for derived state. Use `effect()` only for side effects that cannot be expressed by templates or computed values.

The current application contains legacy RxJS-driven state. Preserve existing behavior during feature changes, but do not introduce more RxJS-owned component state unless required by an observable integration. HTTP APIs may continue to return `Observable<T>`; bridge the response to signals at the component/service state boundary and handle loading/error state explicitly.

Prefer functional `inject()` in new code. Constructor injection is present in existing code and does not need incidental conversion.

## Pattern: Signals with HttpClient

```typescript
@Injectable({ providedIn: "root" })
export class UserService {
  private readonly http = inject(HttpClient);

  readonly users = signal<User[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  loadUsers(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.http.get<User[]>("/api/v1/users").subscribe({
      next: (data) => this.users.set(data),
      error: () => {
        this.isLoading.set(false);
        this.error.set("users.loadFailed");
      },
      complete: () => this.isLoading.set(false),
    });
  }
}
```

Do not swallow errors or log-and-continue. Surface failures through the established UI error and localization patterns.

## Angular Conventions

- Use standalone components and Angular built-in template control flow for new UI.
- Keep user-facing text in Transloco resources. Update both `frontend/src/assets/i18n/en.json` and `frontend/src/assets/i18n/fr.json`.
- Keep models and HTTP services synchronized with backend DTOs and estate-scoped API routes.
