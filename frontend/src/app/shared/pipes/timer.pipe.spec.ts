import { TimerPipe } from './timer.pipe';

describe('TimerPipe', () => {
  const pipe = new TimerPipe();

  it('crea 00:00 para 0 segundos', () => {
    expect(pipe.transform(0)).toBe('00:00');
  });

  it('formatea minutos y segundos con dos digitos', () => {
    expect(pipe.transform(65)).toBe('01:05');
    expect(pipe.transform(45 * 60)).toBe('45:00');
  });

  it('incluye horas cuando supera 3600 segundos', () => {
    expect(pipe.transform(3661)).toBe('1:01:01');
    expect(pipe.transform(7200)).toBe('2:00:00');
  });
});
