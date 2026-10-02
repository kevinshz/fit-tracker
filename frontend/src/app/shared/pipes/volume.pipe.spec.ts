import { VolumePipe } from './volume.pipe';

describe('VolumePipe', () => {
  const pipe = new VolumePipe();

  it('devuelve 0 kg para null y undefined', () => {
    expect(pipe.transform(null)).toBe('0 kg');
    expect(pipe.transform(undefined)).toBe('0 kg');
  });

  it('formatea volumenes sencillos', () => {
    expect(pipe.transform(640)).toBe('640 kg');
    expect(pipe.transform(0)).toBe('0 kg');
  });

  it('usa separador de miles en espanol', () => {
    expect(pipe.transform(12345)).toBe('12.345 kg');
  });
});
