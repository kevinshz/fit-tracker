import {
  EQUIPMENT_LABELS,
  EXERCISE_TYPE_LABELS,
  MUSCLE_GROUP_LABELS,
  equipmentLabel,
  exerciseTypeLabel,
  muscleGroupLabel,
} from './enums';

describe('enums: etiquetas en espanol', () => {
  it('muscleGroupLabel traduce valores conocidos', () => {
    expect(muscleGroupLabel('CHEST')).toBe('Pecho');
    expect(muscleGroupLabel('QUADRICEPS')).toBe('Cuádriceps');
    expect(muscleGroupLabel('LOWER_BACK')).toBe('Zona lumbar');
  });

  it('muscleGroupLabel devuelve el valor crudo si es desconocido', () => {
    expect(muscleGroupLabel('UNKNOWN_GROUP')).toBe('UNKNOWN_GROUP');
    expect(muscleGroupLabel('')).toBe('');
  });

  it('equipmentLabel traduce valores conocidos y devuelve el crudo si no', () => {
    expect(equipmentLabel('BARBELL')).toBe('Barra');
    expect(equipmentLabel('BODY_WEIGHT')).toBe('Peso corporal');
    expect(equipmentLabel('SOMETHING_NEW')).toBe('SOMETHING_NEW');
  });

  it('exerciseTypeLabel traduce tipos de ejercicio', () => {
    expect(exerciseTypeLabel('STRENGTH')).toBe('Fuerza');
    expect(exerciseTypeLabel('CARDIO')).toBe('Cardio');
    expect(exerciseTypeLabel('MOBILITY')).toBe('Movilidad');
    expect(exerciseTypeLabel('OTHER')).toBe('OTHER');
  });

  it('los mapas de etiquetas no tienen valores vacios', () => {
    const all = [...Object.values(MUSCLE_GROUP_LABELS), ...Object.values(EQUIPMENT_LABELS), ...Object.values(EXERCISE_TYPE_LABELS)];
    expect(all.every(label => label.trim().length > 0)).toBeTrue();
  });

  it('no hay etiquetas duplicadas dentro de cada mapa', () => {
    expect(new Set(Object.values(MUSCLE_GROUP_LABELS)).size).toBe(Object.keys(MUSCLE_GROUP_LABELS).length);
    expect(new Set(Object.values(EQUIPMENT_LABELS)).size).toBe(Object.keys(EQUIPMENT_LABELS).length);
    expect(new Set(Object.values(EXERCISE_TYPE_LABELS)).size).toBe(Object.keys(EXERCISE_TYPE_LABELS).length);
  });
});
