import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { UserProfileResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Mi Perfil</h1>
        <button class="btn btn-outline-danger btn-sm" (click)="logout()">
          <i class="fa-solid fa-right-from-bracket"></i> Salir
        </button>
      </header>

      <main class="page-body">
        @if (loading) {
          <div class="text-center py-5"><div class="spinner-border text-primary"></div></div>
        } @else if (profile) {
          <div class="profile-card">
            <div class="profile-card__avatar">
              {{ profile.name?.charAt(0) || '?' }}
            </div>
            <h3>{{ profile.name }}</h3>
            <p class="text-muted">{{ profile.email }}</p>
          </div>

          <form [formGroup]="form" (ngSubmit)="onSave()" class="profile-form">
            <div class="mb-3">
              <label class="form-label">Nombre</label>
              <input type="text" class="form-control" formControlName="name">
            </div>
            <div class="row mb-3">
              <div class="col-6">
                <label class="form-label">Peso (kg)</label>
                <input type="number" class="form-control" formControlName="bodyWeight">
              </div>
              <div class="col-6">
                <label class="form-label">Altura (cm)</label>
                <input type="number" class="form-control" formControlName="height">
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label">Genero</label>
              <select class="form-select" formControlName="gender">
                <option value="">Seleccionar</option>
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Femenino</option>
                <option value="OTHER">Otro</option>
              </select>
            </div>
            <div class="mb-3">
              <label class="form-label">Objetivo</label>
              <select class="form-select" formControlName="fitnessGoal">
                <option value="">Seleccionar</option>
                <option value="MUSCLE_GAIN">Ganar musculo</option>
                <option value="FAT_LOSS">Perder grasa</option>
                <option value="STRENGTH">Fuerza</option>
                <option value="ENDURANCE">Resistencia</option>
              </select>
            </div>

            <button type="submit" class="btn btn-primary w-100" [disabled]="saving">
              @if (saving) {
                <span class="spinner-border spinner-border-sm me-2"></span>
              }
              Guardar Cambios
            </button>
          </form>
        } @else {
          <div class="empty-state">
            <p>No se pudo cargar el perfil</p>
            <button class="btn btn-primary" (click)="loadProfile()">Reintentar</button>
          </div>
        }
      </main>

      <app-navbar></app-navbar>
    </div>
  `,
  styles: [`
    .page-container { height: 100%; display: flex; flex-direction: column; }
    .page-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px; padding-top: calc(16px + env(safe-area-inset-top, 0));
      background: #fff; border-bottom: 1px solid #E5E7EB;
      h1 { font-size: 1.3rem; font-weight: 700; margin: 0; }
    }
    .page-body { flex: 1; overflow-y: auto; padding: 16px 16px 80px; }
    .profile-card {
      text-align: center; padding: 24px; background: #fff; border-radius: 12px; margin-bottom: 20px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.06);
      &__avatar {
        width: 64px; height: 64px; border-radius: 50%; background: #4F46E5; color: #fff;
        display: inline-flex; align-items: center; justify-content: center;
        font-size: 1.5rem; font-weight: 700; margin-bottom: 12px;
      }
      h3 { font-weight: 700; margin: 0 0 4px; }
    }
    .profile-form {
      background: #fff; padding: 20px; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06);
      .form-label { font-weight: 600; font-size: 0.85rem; }
      .form-control, .form-select { border-radius: 10px; border: 1px solid #E5E7EB;
        &:focus { border-color: #4F46E5; box-shadow: 0 0 0 3px rgba(79,70,229,0.1); }
      }
    }
    .empty-state { text-align: center; padding: 40px; color: #6B7280; }
  `]
})
export class ProfileComponent implements OnInit {
  profile: UserProfileResponse | null = null;
  form: FormGroup;
  loading = true;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      name: [''],
      bodyWeight: [null],
      height: [null],
      gender: [''],
      fitnessGoal: ['']
    });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.userService.getProfile().subscribe({
      next: (data: UserProfileResponse) => {
        this.profile = data;
        this.form.patchValue({
          name: data.name,
          bodyWeight: data.bodyWeight,
          height: data.height,
          gender: data.gender || '',
          fitnessGoal: data.fitnessGoal || ''
        });
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  onSave(): void {
    this.saving = true;
    this.userService.updateProfile(this.form.value).subscribe({
      next: (data: UserProfileResponse) => { this.profile = data; this.saving = false; },
      error: () => this.saving = false
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
