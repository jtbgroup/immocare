// features/estate/components/estate-member-list/estate-member-list.component.ts
// UC016 US097-US100
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { Subject, of } from "rxjs";
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  takeUntil,
} from "rxjs/operators";
import { AuthService } from "../../../../core/auth/auth.service";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { EstateService } from "../../../../core/services/estate.service";
import { UserService } from "../../../../core/services/user.service";
import {
  ESTATE_ROLE_COLORS,
  ESTATE_ROLE_LABELS,
  EstateMember,
  EstateRole,
} from "../../../../models/estate.model";
import { User } from "../../../../models/user.model";

@Component({
  selector: "app-estate-member-list",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./estate-member-list.component.html",
  styleUrls: ["./estate-member-list.component.scss"],
})
export class EstateMemberListComponent implements OnInit, OnDestroy {
  estateId = "";
  members: EstateMember[] = [];
  loading = false;
  error: string | null = null;

  currentUserId: number | null = null;

  // Add member panel
  showAddPanel = false;
  addUserSearch = "";
  addUserResults: User[] = [];
  addUserSearchLoading = false;
  addUserDropdownOpen = false;
  selectedNewUser: User | null = null;
  newMemberRole: EstateRole = "VIEWER";
  addError: string | null = null;
  addSubmitting = false;

  // Delete confirmation
  deleteConfirmMemberId: number | null = null;
  deleteError: string | null = null;
  deleting = false;

  // Role edit
  roleEditMemberId: number | null = null;
  roleEditValue: EstateRole = "VIEWER";
  roleEditSubmitting = false;
  roleEditError: string | null = null;

  readonly ESTATE_ROLE_LABELS = ESTATE_ROLE_LABELS;
  readonly ESTATE_ROLE_COLORS = ESTATE_ROLE_COLORS;
  readonly ROLES: EstateRole[] = ["MANAGER", "VIEWER"];

  private userSearch$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private estateService: EstateService,
    private userService: UserService,
    private activeEstateService: ActiveEstateService,
    private authService: AuthService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.estateId = this.route.snapshot.paramMap.get("estateId") ?? "";

    // Resolve current user id for self-check
    this.authService
      .getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe((user) => {
        // AuthUser has username; we match against members by username
        this._currentUsername = user?.username ?? null;
      });

    this.load();

    // User search debounce
    this.userSearch$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((term) => this.searchUsers(term));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private _currentUsername: string | null = null;

  isSelf(member: EstateMember): boolean {
    return member.username === this._currentUsername;
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

  load(): void {
    this.loading = true;
    this.error = null;
    this.estateService
      .getMembers(this.estateId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (members) => {
          this.members = members;
          this.loading = false;
        },
        error: () => {
          this.error = "Failed to load members.";
          this.loading = false;
        },
      });
  }

  // ─── Add member ───────────────────────────────────────────────────────────

  toggleAddPanel(): void {
    this.showAddPanel = !this.showAddPanel;
    if (!this.showAddPanel) this.resetAddPanel();
  }

  private resetAddPanel(): void {
    this.addUserSearch = "";
    this.addUserResults = [];
    this.selectedNewUser = null;
    this.newMemberRole = "VIEWER";
    this.addError = null;
    this.addUserDropdownOpen = false;
  }

  onAddUserSearchInput(term: string): void {
    this.addUserSearch = term;
    this.userSearch$.next(term);
  }

  private searchUsers(term: string): void {
    if (term.trim().length < 2) {
      this.addUserResults = [];
      this.addUserDropdownOpen = false;
      return;
    }
    this.addUserSearchLoading = true;
    this.addUserDropdownOpen = true;
    this.userService
      .getAll()
      .pipe(
        catchError(() => of([] as User[])),
        takeUntil(this.destroy$),
      )
      .subscribe((users) => {
        const t = term.toLowerCase();
        const memberIds = new Set(this.members.map((m) => m.userId));
        this.addUserResults = users
          .filter(
            (u) =>
              !memberIds.has(u.id) &&
              (u.username.toLowerCase().includes(t) ||
                u.email.toLowerCase().includes(t)),
          )
          .slice(0, 10);
        this.addUserSearchLoading = false;
      });
  }

  selectNewUser(user: User): void {
    this.selectedNewUser = user;
    this.addUserSearch = user.username;
    this.addUserDropdownOpen = false;
    this.addUserResults = [];
  }

  clearNewUser(): void {
    this.selectedNewUser = null;
    this.addUserSearch = "";
    this.addUserDropdownOpen = false;
  }

  closeAddDropdown(): void {
    setTimeout(() => {
      this.addUserDropdownOpen = false;
    }, 200);
  }

  submitAddMember(): void {
    if (!this.selectedNewUser) {
      this.addError = "Please select a user.";
      return;
    }
    this.addSubmitting = true;
    this.addError = null;
    this.estateService
      .addMember(this.estateId, {
        userId: this.selectedNewUser.id,
        role: this.newMemberRole,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.addSubmitting = false;
          this.showAddPanel = false;
          this.resetAddPanel();
          this.load();
        },
        error: (err) => {
          this.addSubmitting = false;
          this.addError = err.error?.message ?? "Failed to add member.";
        },
      });
  }

  // ─── Edit role ────────────────────────────────────────────────────────────

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
          this.load();
        },
        error: (err) => {
          this.roleEditSubmitting = false;
          this.roleEditError = err.error?.message ?? "Failed to update role.";
        },
      });
  }

  // ─── Remove ───────────────────────────────────────────────────────────────

  requestRemove(member: EstateMember): void {
    this.deleteConfirmMemberId = member.userId;
    this.deleteError = null;
  }

  cancelRemove(): void {
    this.deleteConfirmMemberId = null;
    this.deleteError = null;
  }

  confirmRemove(): void {
    if (this.deleteConfirmMemberId === null) return;
    this.deleting = true;
    this.estateService
      .removeMember(this.estateId, this.deleteConfirmMemberId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.deleting = false;
          this.deleteConfirmMemberId = null;
          this.load();
        },
        error: (err) => {
          this.deleting = false;
          this.deleteError = err.error?.message ?? "Failed to remove member.";
          this.deleteConfirmMemberId = null;
        },
      });
  }

  memberToRemove(): EstateMember | undefined {
    return this.members.find((m) => m.userId === this.deleteConfirmMemberId);
  }

  roleStyle(role: EstateRole): { background: string; color: string } {
    const c = ESTATE_ROLE_COLORS[role];
    return { background: c.bg, color: c.text };
  }
}
