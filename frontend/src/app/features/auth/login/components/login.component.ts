import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../../../core/auth/auth.service";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h1 class="login-title">ImmoCare</h1>
        <p class="login-subtitle">Property Management</p>

        <div class="warning-banner" *ngIf="showDefaultWarning">
          ⚠️ You are using the default admin account. Please change your
          password after login.
        </div>

        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" novalidate>
          <div class="form-group">
            <label for="username">Username</label>
            <input
              id="username"
              type="text"
              formControlName="username"
              autocomplete="username"
              placeholder="Enter username"
              [class.error]="hasError('username')"
            />
            <span class="field-error" *ngIf="hasError('username')"
              >Username is required</span
            >
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="current-password"
              placeholder="Enter password"
              [class.error]="hasError('password')"
            />
            <span class="field-error" *ngIf="hasError('password')"
              >Password is required</span
            >
          </div>

          <div class="auth-error" *ngIf="authError">{{ authError }}</div>

          <button type="submit" [disabled]="loading" class="btn-login">
            {{ loading ? "Signing in…" : "Sign in" }}
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .login-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 100vh;
        background: #f5f5f5;
      }
      .login-card {
        background: white;
        padding: 2.5rem;
        border-radius: 8px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
        width: 100%;
        max-width: 400px;
      }
      .login-title {
        font-size: 1.8rem;
        font-weight: 700;
        margin: 0 0 0.25rem;
        color: #1a1a2e;
      }
      .login-subtitle {
        color: #666;
        margin: 0 0 1.5rem;
        font-size: 0.9rem;
      }
      .warning-banner {
        background: #fff3cd;
        border: 1px solid #ffc107;
        border-radius: 4px;
        padding: 0.75rem 1rem;
        font-size: 0.85rem;
        margin-bottom: 1.25rem;
        color: #856404;
      }
      .form-group {
        margin-bottom: 1.25rem;
        display: flex;
        flex-direction: column;
      }
      label {
        font-size: 0.875rem;
        font-weight: 500;
        margin-bottom: 0.4rem;
        color: #333;
      }
      input {
        padding: 0.6rem 0.75rem;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-size: 1rem;
        transition: border-color 0.2s;
      }
      input:focus {
        outline: none;
        border-color: #4a6fa5;
      }
      input.error {
        border-color: #dc3545;
      }
      .field-error {
        font-size: 0.8rem;
        color: #dc3545;
        margin-top: 0.25rem;
      }
      .auth-error {
        background: #f8d7da;
        border: 1px solid #f5c6cb;
        border-radius: 4px;
        padding: 0.6rem 0.75rem;
        font-size: 0.875rem;
        color: #721c24;
        margin-bottom: 1rem;
      }
      .btn-login {
        width: 100%;
        padding: 0.75rem;
        background: #4a6fa5;
        color: white;
        border: none;
        border-radius: 4px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.2s;
      }
      .btn-login:hover:not(:disabled) {
        background: #3a5f95;
      }
      .btn-login:disabled {
        opacity: 0.65;
        cursor: not-allowed;
      }
    `,
  ],
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  authError: string | null = null;
  showDefaultWarning = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ["", Validators.required],
      password: ["", Validators.required],
    });

    this.authService.isAuthenticated().subscribe((auth) => {
      if (auth) this.router.navigate(["/"]);
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.authError = null;
    const { username, password } = this.loginForm.value;
    this.showDefaultWarning = username === "admin";

    this.authService.login(username, password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(["/"]);
      },
      error: () => {
        this.loading = false;
        this.authError = "Invalid username or password. Please try again.";
      },
    });
  }

  hasError(field: string): boolean {
    const control = this.loginForm.get(field);
    return !!(control && control.invalid && control.touched);
  }
}
