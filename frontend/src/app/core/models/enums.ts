export type MuscleLabel =
  | 'CHEST_BACK'
  | 'LEGS'
  | 'PUSH'
  | 'PULL'
  | 'SHOULDERS_ARMS'
  | 'FULL_BODY';

export type SessionType = 'STRENGTH' | 'HYPERTROPHY';

export type MuscleGroup =
  | 'ABS' | 'ABDUCTORS' | 'ADDUCTORS' | 'BICEPS' | 'CALVES'
  | 'CHEST' | 'FOREARMS' | 'GLUTES' | 'HAMSTRINGS' | 'LATS'
  | 'LOWER_BACK' | 'MIDDLE_BACK' | 'NECK' | 'QUADRICEPS'
  | 'SHOULDERS' | 'TRAPS' | 'TRICEPS' | 'OTHER';

export type Equipment =
  | 'BARBELL' | 'DUMBBELL' | 'MACHINE' | 'CABLE' | 'BODY_WEIGHT'
  | 'KETTLEBELL' | 'BANDS' | 'MEDICINE_BALL' | 'EXERCISE_BALL'
  | 'EZ_CURL_BAR' | 'FOAM_ROLL' | 'OTHER';

export type ExerciseType = 'STRENGTH' | 'CARDIO' | 'MOBILITY';

export type ProgressionStrategy = 'LINEAR' | 'DOUBLE_PROGRESSION';

export const MUSCLE_LABEL_OPTIONS: { value: MuscleLabel; label: string }[] = [
  { value: 'CHEST_BACK', label: 'Pecho + Espalda' },
  { value: 'LEGS', label: 'Piernas' },
  { value: 'PUSH', label: 'Empuje' },
  { value: 'PULL', label: 'Tirón' },
  { value: 'SHOULDERS_ARMS', label: 'Hombros + Brazos' },
  { value: 'FULL_BODY', label: 'Full Body' },
];

export const SESSION_TYPE_OPTIONS: { value: SessionType; label: string }[] = [
  { value: 'STRENGTH', label: 'Fuerza' },
  { value: 'HYPERTROPHY', label: 'Hipertrofia' },
];

export const MUSCLE_GROUP_LABELS: Record<MuscleGroup, string> = {
  ABS: 'Abdomen',
  ABDUCTORS: 'Abductores',
  ADDUCTORS: 'Aductores',
  BICEPS: 'Bíceps',
  CALVES: 'Gemelos',
  CHEST: 'Pecho',
  FOREARMS: 'Antebrazos',
  GLUTES: 'Glúteos',
  HAMSTRINGS: 'Isquios',
  LATS: 'Dorsales',
  LOWER_BACK: 'Zona lumbar',
  MIDDLE_BACK: 'Espalda media',
  NECK: 'Cuello',
  QUADRICEPS: 'Cuádriceps',
  SHOULDERS: 'Hombros',
  TRAPS: 'Trapecios',
  TRICEPS: 'Tríceps',
  OTHER: 'Otro',
};

export const EQUIPMENT_LABELS: Record<Equipment, string> = {
  BARBELL: 'Barra',
  DUMBBELL: 'Mancuerna',
  MACHINE: 'Máquina',
  CABLE: 'Polea',
  BODY_WEIGHT: 'Peso corporal',
  KETTLEBELL: 'Kettlebell',
  BANDS: 'Bandas',
  MEDICINE_BALL: 'Balón medicinal',
  EXERCISE_BALL: 'Pelota',
  EZ_CURL_BAR: 'Barra EZ',
  FOAM_ROLL: 'Rodillo',
  OTHER: 'Otro',
};

export const EXERCISE_TYPE_LABELS: Record<ExerciseType, string> = {
  STRENGTH: 'Fuerza',
  CARDIO: 'Cardio',
  MOBILITY: 'Movilidad',
};

export function muscleGroupLabel(value: string): string {
  return MUSCLE_GROUP_LABELS[value as MuscleGroup] ?? value;
}

export function equipmentLabel(value: string): string {
  return EQUIPMENT_LABELS[value as Equipment] ?? value;
}

export function exerciseTypeLabel(value: string): string {
  return EXERCISE_TYPE_LABELS[value as ExerciseType] ?? value;
}
