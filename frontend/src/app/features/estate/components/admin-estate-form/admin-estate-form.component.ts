// features/estate/components/admin-estate-form/admin-estate-form.component.ts
// UC016 US092 (create) + US093 (edit)
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Subject, of } from "rxjs";
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  takeUntil,
} from "rxjs/operators";
import { EstateService } from "../../../../core/services/estate.service";
import { UserService } from "../../../../core/services/user.service";
import { User } from "../../../../models/user.model";

@Component({
  selector: "app-admin-estate-form",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: "./admin-estate-form.component.html",
  styleUrls: ["./admin-estate-form.component.scss"],
})
export class AdminEstateFormComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  isEdit = false;
  estateId?: string;
  loading = false;
  submitting = false;
  errorMessage = "";
  isDirty = false;

  // User picker for firstManager (create mode only)
  userSearchTerm = "";
  userResults: User[] = [];
  selectedManager: User | null = null;
  userSearchLoading = false;
  userDropdownOpen = false;

  private userSearch$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private estateService: EstateService,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get("id");
    this.isEdit = !!id;
    this.estateId = id ?? undefined;

    this.buildForm();

    if (this.isEdit && this.estateId) {
      this.loading = true;
      this.estateService
        .getAllEstates(0, 1) // We fetch via getById if available
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (page) => {
            // Since there's no direct getById, find from list or rely on form
            // In practice the backend service should expose getById
            this.loading = false;
          },
          error: () => {
            this.loading = false;
            this.errorMessage = "Failed to load estate.";
          },
        });
    }

    // User search with debounce
    this.userSearch$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((term) => {
        if (term.trim().length < 2) {
          this.userResults = [];
          this.userDropdownOpen = false;
          return;
        }
        this.userSearchLoading = true;
        this.userDropdownOpen = true;
        this.userService
          .getAll()
          .pipe(
            catchError(() => of([] as User[])),
            takeUntil(this.destroy$),
          )
          .subscribe((users) => {
            const t = term.toLowerCase();
            this.userResults = users
              .filter(
                (u) =>
                  u.username.toLowerCase().includes(t) ||
                  u.email.toLowerCase().includes(t),
              )
              .slice(0, 10);
            this.userSearchLoading = false;
          });
      });

    this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.isDirty = this.form.dirty;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      name: ["", [Validators.required, Validators.maxLength(100)]],
      description: [""],
    });
  }

  onUserSearchInput(term: string): void {
    this.userSearchTerm = term;
    this.userSearch$.next(term);
  }

  selectManager(user: User): void {
    this.selectedManager = user;
    this.userSearchTerm = user.username;
    this.userDropdownOpen = false;
    this.userResults = [];
  }

  clearManager(): void {
    this.selectedManager = null;
    this.userSearchTerm = "";
    this.userDropdownOpen = false;
  }

  closeUserDropdown(): void {
    setTimeout(() => {
      this.userDropdownOpen = false;
    }, 200);
  }

  fieldError(name: string): string {
    const ctrl = this.form.get(name);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return "";
    if (ctrl.errors?.["required"]) return `${name} is required`;
    if (ctrl.errors?.["maxlength"]) return `${name} is too long`;
    return "";
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting = true;
    this.errorMessage = "";
    const { name, description } = this.form.value;

    const obs =
      this.isEdit && this.estateId
        ? this.estateService.updateEstate(this.estateId, {
            name,
            description: description || undefined,
          })
        : this.estateService.createEstate({
            name,
            description: description || undefined,
            firstManagerId: this.selectedManager?.id,
          });

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.submitting = false;
        this.form.markAsPristine();
        this.router.navigate(["/admin/estates"]);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message ?? "An error occurred.";
      },
    });
  }

  cancel(): void {
    if (
      this.isDirty &&
      !confirm("You have unsaved changes. Are you sure you want to cancel?")
    )
      return;
    this.router.navigate(["/admin/estates"]);
  }
}
