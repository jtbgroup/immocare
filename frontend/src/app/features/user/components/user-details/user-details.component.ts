import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { User } from '../../../../models/user.model';

function passwordComplexity(control: AbstractControl): ValidationErrors | null {
  const v = control.value as string;
  if (!v) return null;
  if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}/.test(v)) { return { complexity: 'Must contain at least one uppercase letter, one lowercase letter, and one digit' }; }
  return null;
}

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const pw = group.get('newPassword')?.value;
  const cpw = group.get('confirmPassword')?.value;
  if (pw && cpw && pw !== cpw) return { passwordsMismatch: true };
  return null;
}

@Component({
  selector: 'app-user-details',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.scss']
})
export class UserDetailsComponent implements OnInit {
  user?: User;
  currentUsername = '';
  loading = false;
  errorMessage = '';
  showPasswordForm = false;
  pwForm!: FormGroup;
  pwSubmitting = false;
  pwError = '';
  pwSuccess = false;
  deleting = false;
  deleteError = '';

  constructor(private userService: UserService, private route: ActivatedRoute, private router: Router, private fb: FormBuilder) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading = true;
    this.userService.getById(id).subscribe({
      next: (u) => { this.user = u; this.loading = false; },
      error: () => { this.errorMessage = 'User not found.'; this.loading = false; }
    });
    this.pwForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), passwordComplexity]],
      confirmPassword: ['', Validators.required]
    }, { validators: [passwordsMatch] });
  }

  get isSelf(): boolean { return this.user?.username === this.currentUsername; }
  get pwMismatch(): boolean { return this.pwForm.hasError('passwordsMismatch') && !!this.pwForm.get('confirmPassword')?.touched; }

  edit(): void { if (this.user) { this.router.navigate(['/users', this.user.id, 'edit']); } }
  back(): void { this.router.navigate(['/users']); }
  togglePasswordForm(): void { this.showPasswordForm = !this.showPasswordForm; this.pwError = ''; this.pwSuccess = false; this.pwForm.reset(); }

  pwFieldError(name: string): string {
    const ctrl = this.pwForm.get(name);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return '';
    if (ctrl.errors?.['required']) return 'Required';
    if (ctrl.errors?.['minlength']) return 'Too short';
    if (ctrl.errors?.['complexity']) return ctrl.errors['complexity'];
    return '';
  }

  submitPassword(): void {
    this.pwForm.markAllAsTouched();
    if (this.pwForm.invalid || !this.user) return;
    this.pwSubmitting = true;
    this.userService.changePassword(this.user.id, { newPassword: this.pwForm.value.newPassword, confirmPassword: this.pwForm.value.confirmPassword }).subscribe({
      next: () => { this.pwSubmitting = false; this.pwSuccess = true; this.showPasswordForm = false; this.pwForm.reset(); },
      error: (err) => { this.pwSubmitting = false; this.pwError = err.error?.message ?? 'Failed to change password.'; }
    });
  }

  confirmDelete(isLastAdmin: boolean): void {
    if (this.isSelf || isLastAdmin) return;
    if (!confirm(`Delete user "${this.user?.username}"? This cannot be undone.`)) return;
    this.deleting = true;
    this.userService.delete(this.user!.id).subscribe({
      next: () => { this.router.navigate(['/users']); },
      error: (err) => { this.deleting = false; this.deleteError = err.error?.message ?? 'Failed to delete user.'; }
    });
  }
}
