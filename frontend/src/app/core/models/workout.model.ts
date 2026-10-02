import { MuscleLabel, SessionType } from './enums';

// --- Backend DTOs ---

export interface StartSessionRequest {
  performedAt: string;
  muscleLabel: MuscleLabel;
  sessionType: SessionType;
  notes: string;
  routineId?: string | null;
}

export interface LogSetRequest {
  exerciseId: string;
  exerciseName: string;
  setNumber: number;
  weightKg: number;
  reps: number;
  rir: number | null;
}

export interface SessionResponse {
  id: string;
  performedAt: string;
  sessionType: SessionType;
  muscleLabel: MuscleLabel;
  notes: string;
  sets: SetResponse[];
  createdAt: string;
  exercises?: SessionExerciseDto[] | null;
  routineName?: string | null;
}

export interface SessionExerciseDto {
  id: string;
  exerciseId: string;
  exerciseName: string;
  position: number;
  plannedSets: number;
}

export interface RoutineExerciseResponse {
  id: string;
  exerciseId: string;
  exerciseName: string;
  position: number;
  plannedSets: number;
}

export interface RoutineResponse {
  id: string;
  name: string;
  sessionType: SessionType | null;
  muscleLabel: MuscleLabel | null;
  notes: string | null;
  exercises: RoutineExerciseResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface RoutineExerciseRequest {
  exerciseId: string;
  plannedSets: number;
}

export interface RoutineRequest {
  name: string;
  sessionType: SessionType | null;
  muscleLabel: MuscleLabel | null;
  notes: string | null;
  exercises: RoutineExerciseRequest[];
}

export interface SetResponse {
  id: string;
  exerciseId: string;
  exerciseName: string;
  setNumber: number;
  weightKg: number;
  reps: number;
  rir: number | null;
  volumeKg: number;
}

export interface VolumeReportDto {
  from: string;
  to: string;
  items: VolumeByGroup[];
}

export interface VolumeByGroup {
  groupName: string;
  totalVolumeKg: number;
  totalSets: number;
}

export interface ProgressionRecommendation {
  nextSessionType: SessionType;
  muscleLabel: MuscleLabel;
  items: ExerciseRecommendation[];
}

export interface ExerciseRecommendation {
  exerciseId: string;
  exerciseName: string;
  action: string;
  currentLoad: number;
  suggestedLoad: number;
  reason: string;
}

// --- Frontend Presentation Models ---

export interface ActiveWorkout {
  sessionId: string;
  titulo: string;
  tipo: SessionType;
  muscleLabel: MuscleLabel;
  timerGlobalSegundos: number;
  volumenTotalKg: number;
  ejercicios: WorkoutExercise[];
}

export interface WorkoutExercise {
  exerciseId: string;
  nombre: string;
  notas: string;
  showNotas: boolean;
  sets: WorkoutSet[];
}

export interface WorkoutSet {
  numeroSerie: number;
  pesoActual: number | null;
  repsActuales: number | null;
  rir: number | null;
  completada: boolean;
  backendSetId?: string;
  rendimientoAnterior?: string | null;
}
