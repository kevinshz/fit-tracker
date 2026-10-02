import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-card__header">
          <i class="fa-solid fa-dumbbell auth-card__icon"></i>
          <h1>FitTracker</h1>
          <p>Crea tu cuenta</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="auth-card__form">
          @if (errorMessage) {
            <div class="alert alert-danger">{{ errorMessage }}</div>
          }

          <div class="form-floating mb-3">
            <input type="text" class="form-control" id="name"
                   formControlName="name" placeholder="Nombre">
            <label for="name">Nombre</label>
          </div>

          <div class="form-floating mb-3">
            <input type="email" class="form-control" id="email"
                   formControlName="email" placeholder="Email">
            <label for="email">Email</label>
          </div>

          <div class="form-floating mb-3">
            <input type="password" class="form-control" id="password"
                   formControlName="password" placeholder="Contrasena">
            <label for="password">Contrasena (min. 6 caracteres)</label>
          </div>

          <div class="form-floating mb-4">
            <input type="password" class="form-control" id="confirmPassword"
                   formControlName="confirmPassword" placeholder="Confirmar">
            <label for="confirmPassword">Confirmar contrasena</label>
          </div>

          @if (form.errors?.['passwordMismatch'] && form.get('confirmPassword')?.touched) {
            <div class="text-danger mb-3" style="font-size: 0.85rem">
              Las contrasenas no coinciden
            </div>
          }

          <button type="submit" class="btn btn-primary btn-lg w-100 auth-btn"
                  [disabled]="form.invalid || loading">
            @if (loading) {
              <span class="spinner-border spinner-border-sm me-2"></span>
            }
            Crear Cuenta
          </button>
        </form>

        <div class="auth-card__footer">
          <span>Ya tienes cuenta?</span>
          <a routerLink="/login">Inicia sesion</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
      padding: 20px;
    }

    .auth-card {
      background: #fff;
      border-radius: 16px;
      padding: 40px 32px;
      width: 100%;
      max-width: 400px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.15);
    }

    .auth-card__header {
      text-align: center;
      margin-bottom: 32px;

      h1 { font-weight: 800; color: #111827; margin: 8px 0 4px; font-size: 1.8rem; }
      p { color: #6B7280; margin: 0; font-size: 0.9rem; }
    }

    .auth-card__icon {
      font-size: 2.5rem;
      color: #4F46E5;
    }

    .auth-card__form {
      .form-control {
        border-radius: 10px;
        border: 1px solid #E5E7EB;
        font-size: 1rem;

        &:focus {
          border-color: #4F46E5;
          box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
        }
      }
    }

    .auth-btn {
      border-radius: 10px;
      font-weight: 600;
      height: 50px;
      background: #4F46E5;
      border: none;

      &:hover { background: #4338CA; }
      &:disabled { opacity: 0.7; }
    }

    .auth-card__footer {
      text-align: center;
      margin-top: 24px;
      font-size: 0.9rem;
      color: #6B7280;

      a {
        color: #4F46E5;
        font-weight: 600;
        text-decoration: none;
        margin-left: 4px;

        &:hover { text-decoration: underline; }
      }
    }
  `]
})
export class RegisterComponent {
  form: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): { [key: string]: boolean } | null {
    const password = control.get('password');
    const confirm = control.get('confirmPassword');
    if (password && confirm && password.value !== confirm.value) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';

    const { name, email, password } = this.form.value;
    this.authService.register({ name, email, password }).subscribe({
      next: () => this.router.navigate(['/workout']),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Error al crear la cuenta';
      }
    });
  }
}
