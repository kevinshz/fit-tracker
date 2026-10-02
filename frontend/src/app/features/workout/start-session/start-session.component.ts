import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkoutService } from '../../../core/services/workout.service';
import { RoutineService } from '../../../core/services/routine.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { MUSCLE_LABEL_OPTIONS, SESSION_TYPE_OPTIONS, MuscleLabel, SessionType } from '../../../core/models/enums';
import { RoutineResponse } from '../../../core/models/workout.model';

@Component({
  selector: 'app-start-session',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Nuevo Entrenamiento</h1>
      </header>

      <main class="page-body">
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="start-form">
          <div class="form-section">
            <label class="form-label">Rutina (opcional)</label>
            <div class="routine-grid">
              <button type="button" class="routine-chip"
                      [class.routine-chip--active]="!form.get('routineId')?.value"
                      (click)="selectRoutine(null)">
                <i class="fa-solid fa-minus"></i> Sin rutina
              </button>
              @for (routine of routines; track routine.id) {
                <button type="button" class="routine-chip"
                        [class.routine-chip--active]="form.get('routineId')?.value === routine.id"
                        (click)="selectRoutine(routine)">
                  <i class="fa-solid fa-list-check"></i> {{ routine.name }}
                  <span class="routine-chip__count">{{ routine.exercises.length }}</span>
                </button>
              }
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">Tipo de sesion</label>
            <div class="type-grid">
              @for (opt of sessionTypes; track opt.value) {
                <button type="button" class="type-card"
                        [class.type-card--active]="form.get('sessionType')?.value === opt.value"
                        (click)="form.get('sessionType')?.setValue(opt.value)">
                  <i class="fa-solid" [ngClass]="opt.value === 'STRENGTH' ? 'fa-bolt' : 'fa-expand'"></i>
                  <span>{{ opt.label }}</span>
                </button>
              }
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">Grupo muscular</label>
            <div class="muscle-grid">
              @for (opt of muscleLabels; track opt.value) {
                <button type="button" class="muscle-chip"
                        [class.muscle-chip--active]="form.get('muscleLabel')?.value === opt.value"
                        (click)="form.get('muscleLabel')?.setValue(opt.value)">
                  {{ opt.label }}
                </button>
              }
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">Notas (opcional)</label>
            <textarea formControlName="notes" class="form-control" rows="2"
                      placeholder="Ej: Dias de descanso, sensaciones..."></textarea>
          </div>

          <button type="submit" class="btn btn-primary btn-lg w-100 btn-start"
                  [disabled]="form.invalid || loading">
            @if (loading) {
              <span class="spinner-border spinner-border-sm me-2"></span>
            }
            <i class="fa-solid fa-play"></i> Iniciar Entrenamiento
          </button>
        </form>
      </main>

      <app-navbar></app-navbar>
    </div>
  `,
  styles: [`
    .page-container { height: 100%; display: flex; flex-direction: column; }
    .page-header {
      padding: 16px; padding-top: calc(16px + env(safe-area-inset-top, 0));
      background: #fff; border-bottom: 1px solid #E5E7EB;
      h1 { font-size: 1.3rem; font-weight: 700; margin: 0; }
    }
    .page-body { flex: 1; overflow-y: auto; padding: 16px 16px 80px; }
    .start-form { max-width: 480px; margin: 0 auto; }
    .form-section { margin-bottom: 24px; }
    .form-label { display: block; font-weight: 600; font-size: 0.85rem; color: #374151; margin-bottom: 10px; }

    .type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .type-card {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      padding: 16px; border-radius: 12px; border: 2px solid #E5E7EB;
      background: #fff; cursor: pointer; transition: all 0.2s; font-weight: 600; font-size: 0.9rem; color: #374151;
      i { font-size: 1.5rem; color: #9CA3AF; }
      &:hover { border-color: #4F46E5; }
      &--active { border-color: #4F46E5; background: rgba(79,70,229,0.05); color: #4F46E5; i { color: #4F46E5; } }
    }

    .muscle-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .muscle-chip {
      padding: 8px 16px; border-radius: 20px; border: 1.5px solid #E5E7EB;
      background: #fff; cursor: pointer; font-size: 0.85rem; font-weight: 500; color: #374151; transition: all 0.2s;
      &:hover { border-color: #4F46E5; }
      &--active { border-color: #4F46E5; background: #4F46E5; color: #fff; }
    }

    .routine-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .routine-chip {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 8px 14px; border-radius: 10px; border: 1.5px solid #E5E7EB;
      background: #fff; cursor: pointer; font-size: 0.82rem; font-weight: 500; color: #374151; transition: all 0.2s;
      &:hover { border-color: #4F46E5; }
      &--active { border-color: #4F46E5; background: rgba(79,70,229,0.06); color: #4F46E5; }
      &__count {
        font-size: 0.65rem; background: #E5E7EB; color: #374151;
        padding: 1px 6px; border-radius: 10px; font-weight: 600;
      }
    }

    .btn-start {
      border-radius: 12px; font-weight: 700; height: 52px; display: flex; align-items: center; justify-content: center; gap: 8px;
      background: #4F46E5; border: none; &:hover { background: #4338CA; }
    }

    .form-control { border-radius: 10px; border: 1px solid #E5E7EB; font-size: 1rem;
      &:focus { border-color: #4F46E5; box-shadow: 0 0 0 3px rgba(79,70,229,0.1); }
    }
  `]
})
export class StartSessionComponent implements OnInit {
  form: FormGroup;
  loading = false;
  muscleLabels = MUSCLE_LABEL_OPTIONS;
  sessionTypes = SESSION_TYPE_OPTIONS;
  routines: RoutineResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private workoutService: WorkoutService,
    private routineService: RoutineService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      muscleLabel: ['PUSH', Validators.required],
      sessionType: ['HYPERTROPHY', Validators.required],
      notes: [''],
      routineId: [null]
    });
  }

  ngOnInit(): void {
    this.routineService.list().subscribe({
      next: (routines) => {
        this.routines = routines;
        const routineId = this.route.snapshot.queryParamMap.get('routine');
        if (routineId) {
          const preselected = routines.find(r => r.id === routineId);
          if (preselected) this.selectRoutine(preselected);
        }
      },
      error: () => {}
    });
  }

  selectRoutine(routine: RoutineResponse | null): void {
    if (!routine) {
      this.form.get('routineId')?.setValue(null);
      return;
    }
    this.form.get('routineId')?.setValue(routine.id);
    if (routine.muscleLabel) {
      this.form.get('muscleLabel')?.setValue(routine.muscleLabel);
    }
    if (routine.sessionType) {
      this.form.get('sessionType')?.setValue(routine.sessionType);
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    const { muscleLabel, sessionType, notes, routineId } = this.form.value;
    this.workoutService.startSession({
      performedAt: new Date().toISOString().split('T')[0],
      muscleLabel: muscleLabel as MuscleLabel,
      sessionType: sessionType as SessionType,
      notes: notes || '',
      routineId: routineId || null
    }).subscribe({
      next: (session) => this.router.navigate(['/workout/session', session.id]),
      error: () => this.loading = false
    });
  }
}
