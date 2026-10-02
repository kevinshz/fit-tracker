import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';
import { RoutineService } from '../../../core/services/routine.service';
import { ExerciseService } from '../../../core/services/exercise.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { RoutineExerciseRequest, RoutineResponse } from '../../../core/models/workout.model';
import { ExerciseSummary, PageResponse } from '../../../core/models/exercise.model';
import { MUSCLE_LABEL_OPTIONS, SESSION_TYPE_OPTIONS } from '../../../core/models/enums';

interface RoutineExerciseRow {
  exerciseId: string;
  name: string;
  plannedSets: number;
}

@Component({
  selector: 'app-routine-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, NavbarComponent],
  template: `
    <div class="page-container">
      <header class="page-header">
        <a routerLink="/routines" class="btn-back" title="Volver">
          <i class="fa-solid fa-arrow-left"></i>
        </a>
        <h1>{{ isEdit ? 'Editar Rutina' : 'Nueva Rutina' }}</h1>
      </header>

      <main class="page-body">
        @if (loading) {
          <div class="text-center py-5"><div class="spinner-border text-primary"></div></div>
        } @else {
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="routine-form">
            <div class="form-section">
              <label class="form-label">Nombre *</label>
              <input type="text" formControlName="name" class="form-control"
                     placeholder="Ej: Empuje A, Piernas 5x5..." maxlength="100">
              @if (form.get('name')?.touched && form.get('name')?.invalid) {
                <span class="field-error">El nombre es obligatorio (max 100 caracteres)</span>
              }
            </div>

            <div class="form-section">
              <label class="form-label">Tipo de sesion (opcional)</label>
              <div class="chip-row">
                @for (opt of sessionTypes; track opt.value) {
                  <button type="button" class="chip"
                          [class.chip--active]="form.get('sessionType')?.value === opt.value"
                          (click)="toggleSessionType(opt.value)">
                    {{ opt.label }}
                  </button>
                }
              </div>
            </div>

            <div class="form-section">
              <label class="form-label">Grupo muscular (opcional)</label>
              <div class="chip-row">
                @for (opt of muscleLabels; track opt.value) {
                  <button type="button" class="chip"
                          [class.chip--active]="form.get('muscleLabel')?.value === opt.value"
                          (click)="toggleMuscleLabel(opt.value)">
                    {{ opt.label }}
                  </button>
                }
              </div>
            </div>

            <div class="form-section">
              <label class="form-label">Notas (opcional)</label>
              <textarea formControlName="notes" class="form-control" rows="2" maxlength="500"
                        placeholder="Objetivo, observaciones..."></textarea>
            </div>

            <div class="form-section">
              <label class="form-label">Ejercicios * ({{ exercises.length }})</label>
              <div class="exercise-search">
                <i class="fa-solid fa-magnifying-glass"></i>
                <input type="text" [(ngModel)]="searchQuery" [ngModelOptions]="{ standalone: true }"
                       (input)="onSearch()" placeholder="Buscar ejercicio para agregar...">
              </div>

              @if (searching) {
                <div class="search-results"><span class="text-muted">Buscando...</span></div>
              } @else if (searchQuery.trim() && searchResults.length === 0) {
                <div class="search-results"><span class="text-muted">Sin resultados</span></div>
              } @else if (searchQuery.trim()) {
                @for (result of searchResults; track result.id) {
                  <button type="button" class="search-result" (click)="addExercise(result)"
                          [disabled]="isAdded(result.id)">
                    <span class="search-result__name">{{ result.name }}</span>
                    @if (isAdded(result.id)) {
                      <span class="search-result__added">Agregado</span>
                    } @else {
                      <i class="fa-solid fa-plus"></i>
                    }
                  </button>
                }
              }

              @if (exercises.length === 0) {
                <p class="hint">Agrega al menos un ejercicio a la rutina.</p>
              }

              @for (row of exercises; track row.exerciseId; let i = $index) {
                <div class="exercise-row">
                  <span class="exercise-row__name">{{ i + 1 }}. {{ row.name }}</span>
                  <div class="exercise-row__sets">
                    <label>Series</label>
                    <input type="number" min="1" max="20" [value]="row.plannedSets"
                           (input)="onPlannedSetsChange(i, $event)" class="sets-input">
                  </div>
                  <button type="button" class="btn-remove" (click)="removeExercise(i)" title="Quitar">
                    <i class="fa-solid fa-xmark"></i>
                  </button>
                </div>
              }

              @if (submitError) {
                <span class="field-error">{{ submitError }}</span>
              }
            </div>

            <button type="submit" class="btn btn-primary btn-lg w-100 btn-save"
                    [disabled]="saving">
              @if (saving) {
                <span class="spinner-border spinner-border-sm me-2"></span>
              }
              <i class="fa-solid fa-floppy-disk"></i> {{ isEdit ? 'Guardar cambios' : 'Crear rutina' }}
            </button>
          </form>
        }
      </main>

      <app-navbar></app-navbar>
    </div>
  `,
  styles: [`
    .page-container { height: 100%; display: flex; flex-direction: column; }
    .page-header {
      display: flex; align-items: center; gap: 12px;
      padding: 16px; padding-top: calc(16px + env(safe-area-inset-top, 0));
      background: #fff; border-bottom: 1px solid #E5E7EB;
      h1 { font-size: 1.3rem; font-weight: 700; margin: 0; }
    }
    .btn-back {
      width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
      border-radius: 10px; background: #F3F4F6; color: #374151; text-decoration: none;
      &:hover { background: #E5E7EB; }
    }
    .page-body { flex: 1; overflow-y: auto; padding: 16px 16px 80px; }
    .routine-form { max-width: 560px; margin: 0 auto; }
    .form-section { margin-bottom: 22px; }
    .form-label { display: block; font-weight: 600; font-size: 0.85rem; color: #374151; margin-bottom: 10px; }
    .form-control { border-radius: 10px; border: 1px solid #E5E7EB; font-size: 1rem; width: 100%;
      &:focus { border-color: #4F46E5; box-shadow: 0 0 0 3px rgba(79,70,229,0.1); }
    }
    .field-error { color: #DC2626; font-size: 0.8rem; display: block; margin-top: 6px; }
    .hint { font-size: 0.8rem; color: #6B7280; margin: 8px 0 0; }

    .chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
    .chip {
      padding: 8px 14px; border-radius: 20px; border: 1.5px solid #E5E7EB;
      background: #fff; cursor: pointer; font-size: 0.82rem; font-weight: 500; color: #374151; transition: all 0.2s;
      &:hover { border-color: #4F46E5; }
      &--active { border-color: #4F46E5; background: #4F46E5; color: #fff; }
    }

    .exercise-search {
      display: flex; align-items: center; gap: 10px; padding: 10px 12px;
      border: 1px solid #E5E7EB; border-radius: 10px; background: #fff;
      i { color: #9CA3AF; }
      input { border: none; outline: none; flex: 1; font-size: 0.95rem; background: transparent; }
    }
    .search-results { margin-top: 8px; }
    .search-result {
      display: flex; align-items: center; justify-content: space-between; width: 100%;
      padding: 10px 12px; border: none; border-bottom: 1px solid #F3F4F6;
      background: #fff; cursor: pointer; font-size: 0.85rem; text-align: left; color: #111827;
      &:hover:not(:disabled) { background: #F9FAFB; }
      &:disabled { cursor: default; opacity: 0.6; }
      &__name { font-weight: 500; }
      &__added { font-size: 0.7rem; color: #059669; font-weight: 600; }
      i { color: #4F46E5; }
    }

    .exercise-row {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 12px; background: #fff; border: 1px solid #E5E7EB;
      border-radius: 10px; margin-top: 8px;
      &__name { flex: 1; font-size: 0.85rem; font-weight: 500; }
      &__sets { display: flex; align-items: center; gap: 6px;
        label { font-size: 0.7rem; color: #6B7280; margin: 0; }
      }
    }
    .sets-input {
      width: 52px; padding: 4px 6px; border: 1px solid #E5E7EB; border-radius: 8px;
      font-size: 0.85rem; text-align: center;
      &:focus { border-color: #4F46E5; outline: none; }
    }
    .btn-remove {
      width: 28px; height: 28px; border-radius: 8px; border: none;
      background: #FEE2E2; color: #DC2626; cursor: pointer;
      &:hover { background: #FCA5A5; color: #fff; }
    }

    .btn-save {
      border-radius: 12px; font-weight: 700; height: 52px;
      display: flex; align-items: center; justify-content: center; gap: 8px;
      background: #4F46E5; border: none; &:hover { background: #4338CA; }
    }
  `]
})
export class RoutineFormComponent implements OnInit, OnDestroy {
  form: FormGroup;
  isEdit = false;
  loading = false;
  saving = false;
  submitError = '';

  sessionTypes = SESSION_TYPE_OPTIONS;
  muscleLabels = MUSCLE_LABEL_OPTIONS;

  exercises: RoutineExerciseRow[] = [];
  searchQuery = '';
  searchResults: ExerciseSummary[] = [];
  searching = false;

  private routineId: string | null = null;
  private searchTrigger = new Subject<string>();
  private searchSubscription: Subscription;

  constructor(
    private fb: FormBuilder,
    private routineService: RoutineService,
    private exerciseService: ExerciseService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      sessionType: [null],
      muscleLabel: [null],
      notes: ['', Validators.maxLength(500)]
    });
    this.searchSubscription = this.searchTrigger.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      filter(q => q.length > 0),
      switchMap(q => this.exerciseService.search(q, 0, 8))
    ).subscribe({
      next: (page: PageResponse<ExerciseSummary>) => {
        this.searchResults = page.content;
        this.searching = false;
      },
      error: () => {
        this.searchResults = [];
        this.searching = false;
      }
    });
  }

  ngOnInit(): void {
    this.routineId = this.route.snapshot.paramMap.get('id');
    if (this.routineId) {
      this.isEdit = true;
      this.loadRoutine(this.routineId);
    }
  }

  private loadRoutine(id: string): void {
    this.loading = true;
    this.routineService.getById(id).subscribe({
      next: (routine: RoutineResponse) => {
        this.form.patchValue({
          name: routine.name,
          sessionType: routine.sessionType,
          muscleLabel: routine.muscleLabel,
          notes: routine.notes
        });
        this.exercises = routine.exercises.map(e => ({
          exerciseId: e.exerciseId,
          name: e.exerciseName,
          plannedSets: e.plannedSets
        }));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/routines']);
      }
    });
  }

  toggleSessionType(value: string): void {
    const control = this.form.get('sessionType');
    control?.setValue(control.value === value ? null : value);
  }

  toggleMuscleLabel(value: string): void {
    const control = this.form.get('muscleLabel');
    control?.setValue(control.value === value ? null : value);
  }

  onSearch(): void {
    const query = this.searchQuery.trim();
    if (!query) {
      this.searchResults = [];
      this.searching = false;
      return;
    }
    this.searching = true;
    this.searchTrigger.next(query);
  }

  isAdded(exerciseId: string): boolean {
    return this.exercises.some(row => row.exerciseId === exerciseId);
  }

  addExercise(exercise: ExerciseSummary): void {
    if (this.isAdded(exercise.id)) return;
    this.exercises.push({ exerciseId: exercise.id, name: exercise.name, plannedSets: 3 });
    this.searchQuery = '';
    this.searchResults = [];
    this.searching = false;
    this.submitError = '';
  }

  removeExercise(index: number): void {
    this.exercises.splice(index, 1);
  }

  onPlannedSetsChange(index: number, event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    this.exercises[index].plannedSets = isNaN(value) ? 3 : Math.min(20, Math.max(1, value));
  }

  onSubmit(): void {
    this.submitError = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.exercises.length === 0) {
      this.submitError = 'Agrega al menos un ejercicio a la rutina.';
      return;
    }

    const payload = {
      name: this.form.value.name.trim(),
      sessionType: this.form.value.sessionType,
      muscleLabel: this.form.value.muscleLabel,
      notes: this.form.value.notes?.trim() || null,
      exercises: this.exercises.map((row): RoutineExerciseRequest => ({
        exerciseId: row.exerciseId,
        plannedSets: row.plannedSets
      }))
    };

    this.saving = true;
    const request = this.isEdit && this.routineId
      ? this.routineService.update(this.routineId, payload)
      : this.routineService.create(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.router.navigate(['/routines']);
      },
      error: (err) => {
        this.saving = false;
        this.submitError = err?.error?.message || 'No se pudo guardar la rutina. Intenta de nuevo.';
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSubscription.unsubscribe();
  }
}
