import { DayOfWeek, MealType } from './models';

export const MEALS: MealType[] = ['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER'];
export const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export const MEAL_LABEL: Record<MealType, string> = {
  BREAKFAST: 'Desayuno',
  LUNCH: 'Almuerzo',
  SNACK: 'Merienda',
  DINNER: 'Cena',
};

export const MEAL_ICON: Record<MealType, string> = { BREAKFAST: '☕', LUNCH: '🍽️', SNACK: '🍎', DINNER: '🌙' };

export const DAY_LABEL: Record<DayOfWeek, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

export const TAG_LABEL: Record<string, string> = {
  VEGETARIAN: 'Vegetariana',
  LOW_CARB: 'Low carb',
  HIGH_PROTEIN: 'Alta en proteína',
  LOW_CALORIE: 'Liviana',
  BREAD: 'Pan',
  DESSERT: 'Postre',
  CRUNCHY_SNACK: 'Snack crunch',
};

export const SOURCE_LABEL: Record<string, string> = {
  DECLARED: 'Del recetario',
  ESTIMATED: 'Estimado con IA',
  MANUAL: 'Cargado a mano',
  UNKNOWN: 'Sin datos',
};

export const CHAIN_LABEL: Record<string, string> = { dia: 'Día', jumbo: 'Jumbo', carrefour: 'Carrefour', vea: 'Vea', disco: 'Disco' };

export function fmtQty(quantity: number | null, unit: string): string {
  if (quantity == null) return 'a gusto';
  const units: Record<string, string> = {
    g: 'g', ml: 'ml', unit: 'u.', slice: 'rebanadas', can: 'latas', clove: 'dientes',
    G: 'g', KG: 'kg', ML: 'ml', L: 'l', CUP: 'tazas', TBSP: 'cdas', TSP: 'cdtas', UNIT: 'u.', SLICE: 'rebanadas',
    CAN: 'latas', CLOVE: 'dientes', PINCH: 'pizca',
  };
  let q = quantity;
  let u = units[unit] ?? unit;
  if (unit === 'g' && q >= 1000) { q = q / 1000; u = 'kg'; }
  if (unit === 'ml' && q >= 1000) { q = q / 1000; u = 'l'; }
  const n = Number.isInteger(q) ? q.toString() : q.toFixed(q < 10 ? 2 : 1).replace(/\.?0+$/, '');
  return `${n} ${u}`;
}

export function money(v: number): string {
  return v.toLocaleString('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 });
}

/** Lunes de la semana de una fecha (yyyy-MM-dd). */
export function mondayOf(date: Date): string {
  const d = new Date(date);
  const day = (d.getDay() + 6) % 7;
  d.setDate(d.getDate() - day);
  return toIso(d);
}

export function toIso(d: Date): string {
  const p = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  return toIso(new Date(y, m - 1, d + days));
}
