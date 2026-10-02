import { MuscleGroup, Equipment, ExerciseType, ProgressionStrategy } from './enums';

export interface ExerciseSummary {
  id: string;
  name: string;
  primaryMuscle: MuscleGroup;
  equipment: Equipment;
  isCustom: boolean;
}

export interface ExerciseResponse {
  id: string;
  name: string;
  type: ExerciseType;
  primaryMuscle: MuscleGroup;
  secondaryMuscles: MuscleGroup[];
  equipment: Equipment;
  progressionStrategy: ProgressionStrategy;
  isCustom: boolean;
  createdBy: string;
  aliases: string[];
}

export interface ExerciseRequest {
  name: string;
  type: ExerciseType;
  primaryMuscle: MuscleGroup;
  secondaryMuscles: MuscleGroup[];
  equipment: Equipment;
  progressionStrategy: ProgressionStrategy;
  aliases: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
