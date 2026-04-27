// features/estate/components/admin-estate-form/admin-estate-form.component.ts
// UC003.001 (create with members list) + UC003.002 (edit + manage members)
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  FormsModule,
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
import {
  ESTATE_ROLE_COLORS,
  ESTATE_ROLE_LABELS,
  EstateMember,
  EstateRole,
} from "../../../../models/estate.model";
import { User } from "../../../../models/user.model";

/** A pending member entry for the create-mode member panel. */
interface PendingMember {
  user: User;
  role: EstateRole;
}

@Component({
  selector: "app-admin-estate-form",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: "./admin-estate-form.component.html",
  styleUrls: ["./admin-estate-form.component.scss"],
})
export class AdminEstateFormComponent implements OnInit, OnDestroy {
  // ─── General form ─────────────────────────────────────────────────────────
  form!: FormGroup;
  isEdit = false;
  estateId?: string;
  loading = false;
  submitting = false;
  errorMessage = "";
  isDirty = false;

  // ─── Active section (edit mode) ───────────────────────────────────────────
  activeSection: "general" | "members" = "general";

  // ─── Members (edit mode — live management) ────────────────────────────────
  members: EstateMember[] = [];
  membersLoading = false;
  membersError: string | null = null;

  // Add member sub-panel (edit mode)
  showAddMemberPanel = false;
  addMemberSearch = "";
  addMemberResults: User[] = [];
  addMemberSearchLoading = false;
  addMemberDropdownOpen = false;
  selectedAddUser: User | null = null;
  addMemberRole: EstateRole = "VIEWER";
  addMemberError: string | null = null;
  addMemberSubmitting = false;

  // Role edit (edit mode)
  roleEditMemberId: number | null = null;
  roleEditValue: EstateRole = "VIEWER";
  roleEditSubmitting = false;
  roleEditError: string | null = null;

  // Delete confirm (edit mode)
  deleteConfirmMemberId: number | null = null;
  deleteError: string | null = null;
  deleting = false;

  // Current user for self-check (edit mode)
  private currentUserId: number | null = null;

  // ─── Members (create mode — pending list, not yet persisted) ─────────────
  pendingMembers: PendingMember[] = [];
  pendingSearch = "";
  pendingResults: User[] = [];
  pendingSearchLoading = false;
  pendingDropdownOpen = false;
  selectedPendingUser: User | null = null;
  pendingRole: EstateRole = "MANAGER";
  pendingError: string | null = null;

  // ─── Shared user-search ───────────────────────────────────────────────────
  private userSearch$ = new Subject<{
    term: string;
    mode: "add" | "pending";
  }>();
  private destroy$ = new Subject<void>();

  readonly ESTATE_ROLE_LABELS = ESTATE_ROLE_LABELS;
  readonly ESTATE_ROLE_COLORS = ESTATE_ROLE_COLORS;
  readonly ROLES: EstateRole[] = ["MANAGER", "VIEWER"];

  constructor(
    private fb: FormBuilder,
    private estateService: EstateService,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    const id =
      this.route.snapshot.paramMap.get("id") ??
      this.route.snapshot.paramMap.get("estateId");
    this.isEdit = !!id;
    this.estateId = id ?? undefined;

    this.buildForm();

    if (this.isEdit && this.estateId) {
      this.loading = true;
      this.estateService
        .getEstateById(this.estateId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (estate) => {
            this.form.patchValue({
              name: estate.name,
              description: estate.description,
            });
            this.loading = false;
          },
          error: () => {
            this.loading = false;
            this.errorMessage = "Failed to load estate.";
          },
        });

      this.loadMembers();
    }

    // Shared debounced user search
    this.userSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => a.term === b.term && a.mode === b.mode),
        takeUntil(this.destroy$),
      )
      .subscribe(({ term, mode }) => this.executeUserSearch(term, mode));

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

  // ─── Section navigation (edit mode) ──────────────────────────────────────

  setSection(section: "general" | "members"): void {
    this.activeSection = section;
  }

  // ─── Members load (edit mode) ─────────────────────────────────────────────

  loadMembers(): void {
    if (!this.estateId) return;
    this.membersLoading = true;
    this.membersError = null;
    this.estateService
      .getMembers(this.estateId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (members) => {
          this.members = members;
          this.membersLoading = false;
        },
        error: () => {
          this.membersError = "Failed to load members.";
          this.membersLoading = false;
        },
      });
  }

  // ─── Add member (edit mode) ───────────────────────────────────────────────

  toggleAddMemberPanel(): void {
    this.showAddMemberPanel = !this.showAddMemberPanel;
    if (!this.showAddMemberPanel) this.resetAddMemberPanel();
  }

  private resetAddMemberPanel(): void {
    this.addMemberSearch = "";
    this.addMemberResults = [];
    this.selectedAddUser = null;
    this.addMemberRole = "VIEWER";
    this.addMemberError = null;
    this.addMemberDropdownOpen = false;
  }

  onAddMemberSearchInput(term: string): void {
    this.addMemberSearch = term;
    this.userSearch$.next({ term, mode: "add" });
  }

  selectAddUser(user: User): void {
    this.selectedAddUser = user;
    this.addMemberSearch = user.username;
    this.addMemberDropdownOpen = false;
    this.addMemberResults = [];
  }

  clearAddUser(): void {
    this.selectedAddUser = null;
    this.addMemberSearch = "";
    this.addMemberDropdownOpen = false;
  }

  closeAddDropdown(): void {
    setTimeout(() => {
      this.addMemberDropdownOpen = false;
    }, 200);
  }

  submitAddMember(): void {
    if (!this.selectedAddUser || !this.estateId) {
      this.addMemberError = "Please select a user.";
      return;
    }
    this.addMemberSubmitting = true;
    this.addMemberError = null;
    this.estateService
      .addMember(this.estateId, {
        userId: this.selectedAddUser.id,
        role: this.addMemberRole,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.addMemberSubmitting = false;
          this.showAddMemberPanel = false;
          this.resetAddMemberPanel();
          this.loadMembers();
        },
        error: (err) => {
          this.addMemberSubmitting = false;
          this.addMemberError = err.error?.message ?? "Failed to add member.";
        },
      });
  }

  // ─── Role edit (edit mode) ────────────────────────────────────────────────

  openRoleEdit(member: EstateMember): void {
    this.roleEditMemberId = member.userId;
    this.roleEditValue = member.role;
    this.roleEditError = null;
  }

  cancelRoleEdit(): void {
    this.roleEditMemberId = null;
    this.roleEditError = null;
  }

  saveRoleEdit(member: EstateMember): void {
    if (!this.estateId) return;
    this.roleEditSubmitting = true;
    this.roleEditError = null;
    this.estateService
      .updateMemberRole(this.estateId, member.userId, {
        role: this.roleEditValue,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.roleEditSubmitting = false;
          this.roleEditMemberId = null;
          this.loadMembers();
        },
        error: (err) => {
          this.roleEditSubmitting = false;
          this.roleEditError = err.error?.message ?? "Failed to update role.";
        },
      });
  }

  // ─── Remove member (edit mode) ────────────────────────────────────────────

  requestRemove(member: EstateMember): void {
    this.deleteConfirmMemberId = member.userId;
    this.deleteError = null;
  }

  cancelRemove(): void {
    this.deleteConfirmMemberId = null;
    this.deleteError = null;
  }

  confirmRemove(): void {
    if (this.deleteConfirmMemberId === null || !this.estateId) return;
    this.deleting = true;
    this.estateService
      .removeMember(this.estateId, this.deleteConfirmMemberId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.deleting = false;
          this.deleteConfirmMemberId = null;
          this.loadMembers();
        },
        error: (err) => {
          this.deleting = false;
          this.deleteError = err.error?.message ?? "Failed to remove member.";
          this.deleteConfirmMemberId = null;
        },
      });
  }

  isSelf(member: EstateMember): boolean {
    return member.userId === this.currentUserId;
  }

  isLastManager(member: EstateMember): boolean {
    if (member.role !== "MANAGER") return false;
    return this.members.filter((m) => m.role === "MANAGER").length <= 1;
  }

  canRemove(member: EstateMember): boolean {
    return !this.isSelf(member) && !this.isLastManager(member);
  }

  canEditRole(member: EstateMember): boolean {
    return !this.isSelf(member);
  }

  removeTooltip(member: EstateMember): string {
    if (this.isSelf(member)) return "You cannot remove yourself from an estate";
    if (this.isLastManager(member)) return "Cannot remove the last manager";
    return "";
  }

  editRoleTooltip(member: EstateMember): string {
    if (this.isSelf(member)) return "You cannot change your own role";
    return "";
  }

  roleStyle(role: EstateRole): { background: string; color: string } {
    const c = ESTATE_ROLE_COLORS[role];
    return { background: c.bg, color: c.text };
  }

  memberToRemove(): EstateMember | undefined {
    return this.members.find((m) => m.userId === this.deleteConfirmMemberId);
  }

  // ─── Pending members (create mode) ───────────────────────────────────────

  onPendingSearchInput(term: string): void {
    this.pendingSearch = term;
    this.userSearch$.next({ term, mode: "pending" });
  }

  selectPendingUser(user: User): void {
    this.selectedPendingUser = user;
    this.pendingSearch = user.username;
    this.pendingDropdownOpen = false;
    this.pendingResults = [];
  }

  clearPendingUser(): void {
    this.selectedPendingUser = null;
    this.pendingSearch = "";
    this.pendingDropdownOpen = false;
  }

  closePendingDropdown(): void {
    setTimeout(() => {
      this.pendingDropdownOpen = false;
    }, 200);
  }

  addPendingMember(): void {
    this.pendingError = null;
    if (!this.selectedPendingUser) {
      this.pendingError = "Please select a user.";
      return;
    }
    if (
      this.pendingMembers.some(
        (m) => m.user.id === this.selectedPendingUser!.id,
      )
    ) {
      this.pendingError = "This user is already in the list.";
      return;
    }
    this.pendingMembers = [
      ...this.pendingMembers,
      { user: this.selectedPendingUser, role: this.pendingRole },
    ];
    this.clearPendingUser();
    this.pendingRole = "MANAGER";
  }

  removePendingMember(userId: number): void {
    this.pendingMembers = this.pendingMembers.filter(
      (m) => m.user.id !== userId,
    );
  }

  updatePendingRole(userId: number, role: EstateRole): void {
    this.pendingMembers = this.pendingMembers.map((m) =>
      m.user.id === userId ? { ...m, role } : m,
    );
  }

  hasPendingManager(): boolean {
    return this.pendingMembers.some((m) => m.role === "MANAGER");
  }

  // ─── Shared user search ───────────────────────────────────────────────────

  private executeUserSearch(term: string, mode: "add" | "pending"): void {
    if (term.trim().length < 2) {
      if (mode === "add") {
        this.addMemberResults = [];
        this.addMemberDropdownOpen = false;
      } else {
        this.pendingResults = [];
        this.pendingDropdownOpen = false;
      }
      return;
    }

    const existingIds =
      mode === "add"
        ? new Set(this.members.map((m) => m.userId))
        : new Set(this.pendingMembers.map((m) => m.user.id));

    if (mode === "add") {
      this.addMemberSearchLoading = true;
      this.addMemberDropdownOpen = true;
    } else {
      this.pendingSearchLoading = true;
      this.pendingDropdownOpen = true;
    }

    this.userService
      .getAll()
      .pipe(
        catchError(() => of([] as User[])),
        takeUntil(this.destroy$),
      )
      .subscribe((users) => {
        const t = term.toLowerCase();
        const results = users
          .filter(
            (u) =>
              !existingIds.has(u.id) &&
              (u.username.toLowerCase().includes(t) ||
                u.email.toLowerCase().includes(t)),
          )
          .slice(0, 10);

        if (mode === "add") {
          this.addMemberResults = results;
          this.addMemberSearchLoading = false;
        } else {
          this.pendingResults = results;
          this.pendingSearchLoading = false;
        }
      });
  }

  // ─── General form helpers ─────────────────────────────────────────────────

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

    // Create mode: validate pending members list
    if (
      !this.isEdit &&
      this.pendingMembers.length > 0 &&
      !this.hasPendingManager()
    ) {
      this.errorMessage = "At least one member must have the MANAGER role.";
      return;
    }

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
            members: this.pendingMembers.map((m) => ({
              userId: m.user.id,
              role: m.role,
            })),
          });

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.submitting = false;
        this.form.markAsPristine();
        this.router.navigate(['/estates', this.estateId, 'dashboard']);
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
      this.router.navigate(['/estates', this.estateId, 'dashboard']);
  }
}
