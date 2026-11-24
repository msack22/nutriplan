import { Injectable, signal } from '@angular/core';
import { mondayOf } from './labels';

/** Estado compartido: la semana seleccionada (planner, stats y compras muestran la misma). */
@Injectable({ providedIn: 'root' })
export class WeekStore {
  readonly weekStart = signal(mondayOf(new Date()));
}
