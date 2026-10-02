import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkoutService } from '../../../core/services/workout.service';
import { ExerciseService } from '../../../core/services/exercise.service';
import { TimerPipe } from '../../../shared/pipes/timer.pipe';
import { VolumePipe } from '../../../shared/pipes/volume.pipe';
import {
  ActiveWorkout, WorkoutExercise, WorkoutSet,
  SessionResponse, SetResponse
} from '../../../core/models/workout.model';
import { ExerciseSummary } from '../../../core/models/exercise.model';

@Component({
  selector: 'app-workout-session',
  standalone: true,
  imports: [CommonModule, FormsModule, TimerPipe, VolumePipe],
  templateUrl: './workout-session.component.html',
  styleUrls: ['./workout-session.component.scss']
})
export class WorkoutSessionComponent implements OnInit, OnDestroy {
  activeWorkout: ActiveWorkout | null = null;
  loading = true;

  private globalTimerInterval: any;

  restTimerActive = false;
  restTimerSeconds = 0;
  restTimerExerciseIdx = -1;
  restTimerSetIdx = -1;
  private restTimerInterval: any;

  showFinishConfirm = false;

  showAddExercise = false;
  addSearchQuery = '';
  addSearchResults: ExerciseSummary[] = [];
  addSearching = false;
  addingExercise = false;
  private addSearchTimeout: any;

  showRemoveConfirm = false;
  exerciseToRemoveIdx = -1;
  removingExercise = false;

  get exerciseToRemoveName(): string {
    if (!this.activeWorkout || this.exerciseToRemoveIdx < 0) return '';
    return this.activeWorkout.ejercicios[this.exerciseToRemoveIdx]?.nombre ?? '';
  }

  /** Ejercicio -> ["60kg x 8", ...] de la sesion anterior mas reciente */
  private previousByExercise = new Map<string, (string | null)[]>();

  constructor(
    private route: ActivatedRoute,
    private workoutService: WorkoutService,
    private exerciseService: ExerciseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const sessionId = this.route.snapshot.paramMap.get('id');
    if (sessionId) {
      this.loadSession(sessionId);
    } else {
      this.router.navigate(['/workout']);
    }
  }

  private loadSession(id: string): void {
    this.workoutService.getSession(id).subscribe({
      next: (response) => {
        this.loadPreviousPerformances(response);
        this.activeWorkout = this.mapSessionResponse(response);
        this.loading = false;
        this.startGlobalTimer();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/workout']);
      }
    });
  }

  private loadPreviousPerformances(current: SessionResponse): void {
    this.workoutService.getSessions().subscribe({
      next: (sessions) => {
        const prior = sessions
          .filter(s => s.id !== current.id)
          .filter(s => (s.sets?.length ?? 0) > 0)
          .sort((a, b) => (b.performedAt || '').localeCompare(a.performedAt || ''));

        for (const session of prior) {
          const byExercise = new Map<string, SetResponse[]>();
          for (const set of session.sets) {
            if (!byExercise.has(set.exerciseId)) byExercise.set(set.exerciseId, []);
            byExercise.get(set.exerciseId)!.push(set);
          }
          for (const [exerciseId, sets] of byExercise) {
            if (this.previousByExercise.has(exerciseId)) continue;
            const sorted = [...sets].sort((a, b) => a.setNumber - b.setNumber);
            this.previousByExercise.set(
              exerciseId,
              sorted.map(s => `${s.weightKg}kg × ${s.reps}`)
            );
          }
        }

        if (this.activeWorkout) {
          this.applyPreviousPerformances();
        }
      },
      error: () => {}
    });
  }

  private applyPreviousPerformances(): void {
    if (!this.activeWorkout) return;
    for (const exercise of this.activeWorkout.ejercicios) {
      const previous = this.previousByExercise.get(exercise.exerciseId);
      if (!previous) continue;
      exercise.sets.forEach((set, idx) => {
        if (!set.rendimientoAnterior && previous[idx]) {
          set.rendimientoAnterior = previous[idx];
        }
      });
    }
  }

  private mapSessionResponse(response: SessionResponse): ActiveWorkout {
    const titulo = response.routineName?.trim()
      || this.muscleLabelToTitle(response.muscleLabel);

    if (response.exercises && response.exercises.length > 0) {
      const setsByExercise = new Map<string, SetResponse[]>();
      for (const set of response.sets) {
        if (!setsByExercise.has(set.exerciseId)) setsByExercise.set(set.exerciseId, []);
        setsByExercise.get(set.exerciseId)!.push(set);
      }

      const ejercicios: WorkoutExercise[] = [...response.exercises]
        .sort((a, b) => a.position - b.position)
        .map(sessionExercise => {
          const backendSets = (setsByExercise.get(sessionExercise.exerciseId) ?? [])
            .sort((a, b) => a.setNumber - b.setNumber);
          const previous = this.previousByExercise.get(sessionExercise.exerciseId) ?? [];
          const totalRows = Math.max(sessionExercise.plannedSets || 0, backendSets.length, 1);
          const sets: WorkoutSet[] = [];
          for (let k = 0; k < totalRows; k++) {
            const backend = backendSets[k];
            sets.push({
              numeroSerie: backend?.setNumber ?? k + 1,
              pesoActual: backend?.weightKg ?? null,
              repsActuales: backend?.reps ?? null,
              rir: backend?.rir ?? null,
              completada: !!backend,
              backendSetId: backend?.id,
              rendimientoAnterior: previous[k] ?? null
            });
          }
          return {
            exerciseId: sessionExercise.exerciseId,
            nombre: sessionExercise.exerciseName,
            notas: '',
            showNotas: false,
            sets
          };
        });

      return {
        sessionId: response.id,
        titulo,
        tipo: response.sessionType,
        muscleLabel: response.muscleLabel,
        timerGlobalSegundos: 0,
        volumenTotalKg: this.calcVolumeFromSets(ejercicios),
        ejercicios
      };
    }

    // Sesiones antiguas sin ejercicios planificados: derivar desde las series
    const exerciseMap = new Map<string, WorkoutExercise>();
    for (const set of response.sets) {
      if (!exerciseMap.has(set.exerciseId)) {
        const previous = this.previousByExercise.get(set.exerciseId) ?? [];
        exerciseMap.set(set.exerciseId, {
          exerciseId: set.exerciseId,
          nombre: set.exerciseName,
          notas: '',
          showNotas: false,
          sets: [],
        });
        exerciseMap.get(set.exerciseId)!.sets.push({
          numeroSerie: set.setNumber,
          pesoActual: set.weightKg,
          repsActuales: set.reps,
          rir: set.rir,
          completada: true,
          backendSetId: set.id,
          rendimientoAnterior: previous[set.setNumber - 1] ?? null
        });
      } else {
        exerciseMap.get(set.exerciseId)!.sets.push({
          numeroSerie: set.setNumber,
          pesoActual: set.weightKg,
          repsActuales: set.reps,
          rir: set.rir,
          completada: true,
          backendSetId: set.id
        });
      }
    }

    return {
      sessionId: response.id,
      titulo,
      tipo: response.sessionType,
      muscleLabel: response.muscleLabel,
      timerGlobalSegundos: 0,
      volumenTotalKg: this.calcVolumeFromSets(Array.from(exerciseMap.values())),
      ejercicios: Array.from(exerciseMap.values())
    };
  }

  private calcVolumeFromSets(exercises: WorkoutExercise[]): number {
    return exercises
      .flatMap(e => e.sets)
      .filter(s => s.completada && s.pesoActual != null && s.repsActuales != null)
      .reduce((sum, s) => sum + s.pesoActual! * s.repsActuales!, 0);
  }

  private muscleLabelToTitle(label: string): string {
    const map: Record<string, string> = {
      CHEST_BACK: 'Pecho + Espalda',
      LEGS: 'Piernas',
      PUSH: 'Empuje',
      PULL: 'Tirón',
      SHOULDERS_ARMS: 'Hombros + Brazos',
      FULL_BODY: 'Full Body'
    };
    return map[label] || label;
  }

  // --- Timer global ---
  private startGlobalTimer(): void {
    this.globalTimerInterval = setInterval(() => {
      if (this.activeWorkout) {
        this.activeWorkout.timerGlobalSegundos++;
      }
    }, 1000);
  }

  // --- Toggle notas ---
  toggleNotas(exerciseIdx: number): void {
    this.activeWorkout!.ejercicios[exerciseIdx].showNotas =
      !this.activeWorkout!.ejercicios[exerciseIdx].showNotas;
  }

  // --- Toggle serie completada ---
  toggleSet(exerciseIdx: number, setIdx: number): void {
    const workout = this.activeWorkout!;
    const set = workout.ejercicios[exerciseIdx].sets[setIdx];

    if (!set.completada) {
      if (set.pesoActual == null || set.repsActuales == null) return;

      set.completada = true;
      this.recalcVolume();
      this.startRestTimer(exerciseIdx, setIdx);

      const exercise = workout.ejercicios[exerciseIdx];
      this.workoutService.logSet(workout.sessionId, {
        exerciseId: exercise.exerciseId,
        exerciseName: exercise.nombre,
        setNumber: set.numeroSerie,
        weightKg: set.pesoActual,
        reps: set.repsActuales,
        rir: set.rir
      }).subscribe({
        next: (saved) => {
          if (saved?.id) set.backendSetId = saved.id;
        },
        error: (err) => console.error('Error guardando serie:', err)
      });
    } else {
      set.completada = false;
      this.recalcVolume();
    }
  }

  // --- Input handlers ---
  onWeightChange(exerciseIdx: number, setIdx: number, event: Event): void {
    const val = parseFloat((event.target as HTMLInputElement).value);
    this.activeWorkout!.ejercicios[exerciseIdx].sets[setIdx].pesoActual =
      isNaN(val) ? null : val;
  }

  onRepsChange(exerciseIdx: number, setIdx: number, event: Event): void {
    const val = parseInt((event.target as HTMLInputElement).value, 10);
    this.activeWorkout!.ejercicios[exerciseIdx].sets[setIdx].repsActuales =
      isNaN(val) ? null : val;
  }

  onRirChange(exerciseIdx: number, setIdx: number, event: Event): void {
    const val = parseInt((event.target as HTMLInputElement).value, 10);
    this.activeWorkout!.ejercicios[exerciseIdx].sets[setIdx].rir =
      isNaN(val) ? null : val;
  }

  onNotasChange(exerciseIdx: number, event: Event): void {
    this.activeWorkout!.ejercicios[exerciseIdx].notas =
      (event.target as HTMLTextAreaElement).value;
  }

  // --- Agregar serie (duplicar ultima) ---
  addSet(exerciseIdx: number): void {
    const exercise = this.activeWorkout!.ejercicios[exerciseIdx];
    const lastSet = exercise.sets[exercise.sets.length - 1];
    exercise.sets.push({
      numeroSerie: exercise.sets.length + 1,
      pesoActual: lastSet?.pesoActual ?? null,
      repsActuales: lastSet?.repsActuales ?? null,
      rir: lastSet?.rir ?? null,
      completada: false
    });
  }

  // --- Timer de descanso ---
  private startRestTimer(exerciseIdx: number, setIdx: number): void {
    this.restTimerActive = true;
    this.restTimerSeconds = 90;
    this.restTimerExerciseIdx = exerciseIdx;
    this.restTimerSetIdx = setIdx;

    if (this.restTimerInterval) clearInterval(this.restTimerInterval);
    this.restTimerInterval = setInterval(() => {
      this.restTimerSeconds--;
      if (this.restTimerSeconds <= 0) this.skipRest();
    }, 1000);
  }

  skipRest(): void {
    clearInterval(this.restTimerInterval);
    this.restTimerActive = false;
    this.restTimerSeconds = 0;
  }

  // --- Volumen total ---
  private recalcVolume(): void {
    if (!this.activeWorkout) return;
    this.activeWorkout.volumenTotalKg = this.calcVolumeFromSets(this.activeWorkout.ejercicios);
  }

  // --- Agregar ejercicio a la sesion ---
  openAddExercise(): void {
    this.showAddExercise = true;
    this.addSearchQuery = '';
    this.addSearchResults = [];
    this.addSearching = false;
  }

  closeAddExercise(): void {
    this.showAddExercise = false;
    clearTimeout(this.addSearchTimeout);
  }

  onAddSearch(): void {
    const query = this.addSearchQuery.trim();
    clearTimeout(this.addSearchTimeout);
    if (!query) {
      this.addSearchResults = [];
      this.addSearching = false;
      return;
    }
    this.addSearching = true;
    this.addSearchTimeout = setTimeout(() => {
      this.exerciseService.search(query, 0, 8).subscribe({
        next: (page) => { this.addSearchResults = page.content; this.addSearching = false; },
        error: () => { this.addSearchResults = []; this.addSearching = false; }
      });
    }, 300);
  }

  addExercise(exercise: ExerciseSummary): void {
    if (!this.activeWorkout || this.addingExercise) return;
    if (this.activeWorkout.ejercicios.some(e => e.exerciseId === exercise.id)) {
      this.closeAddExercise();
      return;
    }
    this.addingExercise = true;
    this.workoutService.addSessionExercise(this.activeWorkout.sessionId, exercise.id, 3).subscribe({
      next: (created) => {
        this.activeWorkout!.ejercicios.push({
          exerciseId: created.exerciseId,
          nombre: created.exerciseName,
          notas: '',
          showNotas: false,
          sets: Array.from({ length: created.plannedSets || 3 }, (_, k) => ({
            numeroSerie: k + 1,
            pesoActual: null,
            repsActuales: null,
            rir: null,
            completada: false
          }))
        });
        this.addingExercise = false;
        this.closeAddExercise();
      },
      error: () => { this.addingExercise = false; }
    });
  }

  // --- Eliminar ejercicio ---
  private exerciseHasLoggedData(exercise: WorkoutExercise): boolean {
    return exercise.sets.some(s =>
      s.backendSetId != null || s.pesoActual != null || s.repsActuales != null
    );
  }

  removeExercise(index: number): void {
    if (!this.activeWorkout) return;
    const exercise = this.activeWorkout.ejercicios[index];
    if (this.exerciseHasLoggedData(exercise)) {
      this.exerciseToRemoveIdx = index;
      this.showRemoveConfirm = true;
      return;
    }
    this.doRemoveExercise(index);
  }

  cancelRemoveExercise(): void {
    this.showRemoveConfirm = false;
    this.exerciseToRemoveIdx = -1;
  }

  confirmRemoveExercise(): void {
    if (this.exerciseToRemoveIdx < 0) return;
    this.removingExercise = true;
    const index = this.exerciseToRemoveIdx;
    const exercise = this.activeWorkout!.ejercicios[index];
    this.workoutService.removeSessionExercise(this.activeWorkout!.sessionId, exercise.exerciseId).subscribe({
      next: () => {
        this.doRemoveExercise(index, false);
        this.removingExercise = false;
        this.showRemoveConfirm = false;
        this.exerciseToRemoveIdx = -1;
      },
      error: () => { this.removingExercise = false; }
    });
  }

  private doRemoveExercise(index: number, callApi = true): void {
    if (!this.activeWorkout) return;
    const [removed] = this.activeWorkout.ejercicios.splice(index, 1);
    if (callApi && removed && !this.activeWorkout.ejercicios.some(e => e.exerciseId === removed.exerciseId)) {
      this.workoutService.removeSessionExercise(this.activeWorkout.sessionId, removed.exerciseId)
        .subscribe({ error: () => {} });
    }
    this.recalcVolume();
  }

  // --- Finalizar ---
  requestFinish(): void {
    this.showFinishConfirm = true;
  }

  confirmFinish(): void {
    clearInterval(this.globalTimerInterval);
    clearInterval(this.restTimerInterval);
    this.showFinishConfirm = false;
    this.router.navigate(['/workout']);
  }

  cancelFinish(): void {
    this.showFinishConfirm = false;
  }

  ngOnDestroy(): void {
    clearInterval(this.globalTimerInterval);
    clearInterval(this.restTimerInterval);
    clearTimeout(this.addSearchTimeout);
  }
}
