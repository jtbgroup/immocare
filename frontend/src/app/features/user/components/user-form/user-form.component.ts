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
  const pw = group.get('password')?.value;
  const cpw = group.get('confirmPassword')?.value;
  if (pw && cpw && pw !== cpw) { return { passwordsMismatch: true }; }
  return null;
}

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.scss']
})
export class UserFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  userId?: number;
  loading = false;
  submitting = false;
  errorMessage = '';
  isDirty = false;
  readonly roles = ['ADMIN'];

  constructor(private fb: FormBuilder, private userService: UserService, private router: Router, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!id && id !== 'new';
    this.buildForm();
    if (this.isEdit) {
      this.userId = Number(id);
      this.loading = true;
      this.userService.getById(this.userId).subscribe({
        next: (user) => { this.patchForm(user); this.loading = false; setTimeout(() => this.form.markAsPristine()); },
        error: () => { this.errorMessage = 'Failed to load user.'; this.loading = false; }
      });
    }
    this.form.valueChanges.subscribe(() => { this.isDirty = this.form.dirty; });
  }

  private buildForm(): void {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern('^[a-zA-Z0-9_]+$')]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', this.isEdit ? [] : [Validators.required, Validators.minLength(8), passwordComplexity]],
      confirmPassword: ['', this.isEdit ? [] : [Validators.required]],
      role: ['ADMIN', Validators.required],
    }, { validators: this.isEdit ? [] : [passwordsMatch] });
    if (this.isEdit) { this.form.removeControl('password'); this.form.removeControl('confirmPassword'); }
  }

  private patchForm(user: User): void { this.form.patchValue({ username: user.username, email: user.email, role: user.role }); }

  get f(): Record<string, AbstractControl> { return this.form.controls; }

  fieldError(name: string): string {
    const ctrl = this.form.get(name);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return '';
    if (ctrl.errors?.['required']) return `${name} is required`;
    if (ctrl.errors?.['minlength']) return `${name} is too short`;
    if (ctrl.errors?.['maxlength']) return `${name} is too long`;
    if (ctrl.errors?.['email']) return 'Email must be valid';
    if (ctrl.errors?.['pattern']) return 'Only letters, digits, and underscores allowed';
    if (ctrl.errors?.['complexity']) return ctrl.errors['complexity'];
    return '';
  }

  get passwordMismatch(): boolean { return !this.isEdit && this.form.hasError('passwordsMismatch') && !!this.form.get('confirmPassword')?.touched; }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.submitting = true;
    this.errorMessage = '';
    const val = this.form.value;
    const obs = this.isEdit && this.userId != null
      ? this.userService.update(this.userId, { username: val['username'], email: val['email'], role: val['role'] })
      : this.userService.create({ username: val['username'], email: val['email'], password: val['password'], confirmPassword: val['confirmPassword'], role: val['role'] });
    obs.subscribe({
      next: (user) => { this.submitting = false; this.form.markAsPristine(); this.router.navigate(['/users', user.id]); },
      error: (err) => { this.submitting = false; this.errorMessage = err.error?.message ?? 'An error occurred.'; }
    });
  }

  cancel(): void {
    if (this.isDirty && !confirm('You have unsaved changes. Are you sure you want to cancel?')) return;
    this.router.navigate(['/users']);
  }
}
