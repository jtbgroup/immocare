import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { User } from '../../../../models/user.model';

function passwordComplexity(control: AbstractControl): ValidationErrors | null {
  const v = control.value as string;
  if (!v) return null;
  if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}/.test(v)) {
    return { complexity: 'Must contain at least one uppercase letter, one lowercase letter, and one digit' };
  }
  return null;
}

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const pw  = group.get('newPassword')?.value;
  const cpw = group.get('confirmPassword')?.value;
  if (pw && cpw && pw !== cpw) return { passwordsMismatch: true };
  return null;
}

@Component({
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.scss'],
})
export class UserDetailsComponent implements OnInit {
  user?:           User;
  currentUsername: string = '';   // set from /auth/me if available via AuthService
  loading          = false;
  errorMessage     = '';

  // Password change inline form
  showPasswordForm = false;
  pwForm!:          FormGroup;
  pwSubmitting     = false;
  pwError          = '';
  pwSuccess        = false;

  // Delete state
  deleting         = false;
  deleteError      = '';

  constructor(
    private userService: UserService,
    private route:       ActivatedRoute,
    private router:      Router,
    private fb:          FormBuilder,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading = true;
    this.userService.getById(id).subscribe({
      next: (u) => { this.user = u; this.loading = false; },
      error: () => { this.errorMessage = 'User not found.'; this.loading = false; },
    });

    this.pwForm = this.fb.group(
      {
        newPassword:     ['', [Validators.required, Validators.minLength(8), passwordComplexity]],
        confirmPassword: ['', Validators.required],
      },
      { validators: passwordsMatch },
    );
  }

  // -------------------------------------------------------------------------
  // Edit
  // -------------------------------------------------------------------------

  edit(): void {
    this.router.navigate(['/users', this.user!.id, 'edit']);
  }

  // -------------------------------------------------------------------------
  // Change password (US034)
  // -------------------------------------------------------------------------

  togglePasswordForm(): void {
    this.showPasswordForm = !this.showPasswordForm;
    this.pwError   = '';
    this.pwSuccess = false;
    this.pwForm.reset();
  }

  submitPassword(): void {
    this.pwForm.markAllAsTouched();
    if (this.pwForm.invalid) return;

    this.pwSubmitting = true;
    this.pwError      = '';
    this.pwSuccess    = false;
    const val         = this.pwForm.value;

    this.userService.changePassword(this.user!.id, {
      newPassword:     val['newPassword'],
      confirmPassword: val['confirmPassword'],
    }).subscribe({
      next: () => {
        this.pwSubmitting    = false;
        this.pwSuccess       = true;
        this.showPasswordForm = false;
        this.pwForm.reset();
      },
      error: (err) => {
        this.pwSubmitting = false;
        this.pwError      = err?.error?.message ?? 'Failed to update password.';
      },
    });
  }

  pwFieldError(name: string): string {
    const ctrl = this.pwForm.get(name);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return '';
    if (ctrl.errors?.['required'])   return 'This field is required';
    if (ctrl.errors?.['minlength'])  return 'At least 8 characters required';
    if (ctrl.errors?.['complexity']) return ctrl.errors['complexity'];
    return '';
  }

  get pwMismatch(): boolean {
    return this.pwForm.hasError('passwordsMismatch')
        && !!this.pwForm.get('confirmPassword')?.touched;
  }

  // -------------------------------------------------------------------------
  // Delete (US035)
  // -------------------------------------------------------------------------

  /** True when the user being viewed IS the currently logged-in user. */
  get isSelf(): boolean {
    return !!this.user && this.user.username === this.currentUsername;
  }

  /** True when deleting would remove the last ADMIN — computed client-side
   *  as a UX hint; the server enforces it definitively. */
  cannotDelete(isLastAdmin: boolean): boolean {
    return this.isSelf || isLastAdmin;
  }

  confirmDelete(isLastAdmin: boolean): void {
    if (this.isSelf) return;     // safeguard — button should be disabled
    if (isLastAdmin) return;

    const confirmed = confirm(`Delete user "${this.user!.username}"? This action cannot be undone.`);
    if (!confirmed) return;

    this.deleting     = true;
    this.deleteError  = '';

    this.userService.delete(this.user!.id).subscribe({
      next:  () => this.router.navigate(['/users']),
      error: (err) => {
        this.deleting    = false;
        this.deleteError = err?.error?.message ?? 'Failed to delete user.';
      },
    });
  }

  back(): void {
    this.router.navigate(['/users']);
  }
}
