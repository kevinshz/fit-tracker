import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'volume', standalone: true })
export class VolumePipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value == null) return '0 kg';
    return value.toLocaleString('es-ES', { maximumFractionDigits: 0 }) + ' kg';
  }
}
