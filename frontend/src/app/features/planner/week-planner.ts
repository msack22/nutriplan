import { Component, computed, effect, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DAYS, DAY_LABEL, MEALS, MEAL_ICON, MEAL_LABEL, addDays, mondayOf, toIso } from '../../core/labels';
import { DayOfWeek, GenerateRequest, MealType, Plan, PlannedMeal, Profile, RecipeSummary } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { WeekStore } from '../../core/week.store';
import { RecipePicker } from './recipe-picker';

@Component({
  selector: 'app-week-planner',
  imports: [FormsModule, RouterLink, DecimalPipe, RecipePicker],
  template: `
    <div class="page-head">
      <div>
        <h1>Mi semana</h1>
        <div class="row week-nav">
          <button class="btn icon ghost" (click)="shiftWeek(-7)" aria-label="Semana anterior">‹</button>
          <b>{{ weekLabel() }}</b>
          <button class="btn icon ghost" (click)="shiftWeek(7)" aria-label="Semana siguiente">›</button>
          <button class="btn small ghost" (click)="goToday()">Hoy</button>
        </div>
      </div>
      <div class="row">
        <span class="muted small">Cocino para</span>
        <button class="btn icon ghost" (click)="setPeople(-1)" [disabled]="(plan()?.people ?? 1) <= 1">−</button>
        <b>{{ plan()?.people }} {{ plan()?.people === 1 ? 'persona' : 'personas' }}</b>
        <button class="btn icon ghost" (click)="setPeople(1)">+</button>
      </div>
    </div>

    <div class="row actions">
      <button class="btn primary" (click)="showGenerator.set(!showGenerator())">🤖 Generar automáticamente</button>
      <button class="btn ghost" (click)="copyPrevious()">↺ Repetir semana anterior</button>
      <button class="btn danger" (click)="clear()" [disabled]="!plan()?.meals?.length">Vaciar</button>
      <span class="spacer"></span>
      <a class="btn ghost" routerLink="/estadisticas">📊 Estadísticas</a>
      <a class="btn ghost" routerLink="/compras">🛒 Lista de compras</a>
    </div>

    @if (showGenerator()) {
      <section class="card generator">
        <h2>Generar plan</h2>
        <p class="muted small">Elige recetas para acercarse a tu objetivo diario
          @if (profile()?.targets) { (<b>{{ profile()!.targets!.dailyKcal | number }} kcal</b>) }
          priorizando proteína y variedad.</p>
        <div class="gen-grid">
          <label class="field">Comidas
            <div class="row">
              @for (m of allMeals; track m) {
                <label class="check"><input type="checkbox" [checked]="genMeals().includes(m)" (change)="toggleGenMeal(m)" /> {{ mealLabel[m] }}</label>
              }
            </div>
          </label>
          <label class="field">Preferencias (búsqueda semántica)
            <input [(ngModel)]="gen.preference" placeholder="Ej: comida casera, pollo y verduras, nada de pescado" />
          </label>
          <label class="field">Estrategia
            <select [(ngModel)]="gen.strategy">
              <option value="balanced">Balanceada (calorías + proteína)</option>
              <option value="variety">Máxima variedad</option>
            </select>
          </label>
          <label class="field">Repetir cada receta hasta
            <select [(ngModel)]="gen.maxRepeatsPerWeek">
              <option [ngValue]="1">1 vez</option><option [ngValue]="2">2 veces (cocinar doble)</option><option [ngValue]="3">3 veces</option>
            </select>
          </label>
          <label class="check"><input type="checkbox" [(ngModel)]="gen.keepExisting" /> Mantener lo que ya elegí</label>
          <label class="check"><input type="checkbox" [(ngModel)]="gen.vegetarian" /> Solo vegetariano</label>
        </div>
        <div class="row">
          <button class="btn primary" (click)="generate()" [disabled]="generating()">
            {{ generating() ? 'Generando…' : 'Generar semana' }}
          </button>
        </div>
      </section>
    }

    @if (plan(); as p) {
      <div class="board" [style.--cols]="7">
        <div class="corner"></div>
        @for (d of days; track d; let i = $index) {
          <div class="day-head" [class.today]="dateOf(i) === today">
            <b>{{ dayLabel[d] }}</b>
            <span class="muted small">{{ dateOf(i).slice(8) }}/{{ dateOf(i).slice(5, 7) }}</span>
          </div>
        }

        @for (m of visibleMeals(); track m) {
          <div class="meal-head"><span>{{ mealIcon[m] }}</span> {{ mealLabel[m] }}</div>
          @for (d of days; track d) {
            @let slot = slotOf(d, m);
            <div class="cell" [class.filled]="!!slot" [class.eaten]="slot?.eaten">
              @if (slot) {
                <a class="title" [routerLink]="['/recetas', slot.recipeId]">{{ slot.recipeTitle }}</a>
                <span class="kcal">{{ slot.nutritionPerServing?.kcal ? (slot.nutritionPerServing!.kcal! | number: '1.0-0') + ' kcal' : 's/d' }}
                  @if (slot.servings !== p.people) { · {{ slot.servings }} porc. }</span>
                <div class="cell-actions">
                  <label class="check small" title="Marcar como comida"><input type="checkbox" [checked]="slot.eaten" (change)="toggleEaten(slot)" /> Comí</label>
                  <button class="btn icon ghost small" (click)="openPicker(d, m)" title="Cambiar">⇄</button>
                  <button class="btn icon ghost small" (click)="remove(d, m)" title="Quitar">✕</button>
                </div>
              } @else {
                <button class="add" (click)="openPicker(d, m)">+ Agregar</button>
              }
            </div>
          }
        }

        <div class="meal-head total">Total / persona</div>
        @for (d of days; track d) {
          @let kcal = dayKcal(d);
          <div class="cell total">
            <b>{{ kcal | number: '1.0-0' }} kcal</b>
            @if (profile()?.targets; as t) {
              <div class="progress" [title]="'Objetivo ' + t.dailyKcal + ' kcal'">
                <span [style.width.%]="Math.min(100, kcal / t.dailyKcal * 100)" [class.over]="kcal > t.dailyKcal * 1.05"></span>
              </div>
            }
          </div>
        }
      </div>
      <p class="muted small">Las calorías son por persona (1 porción de cada comida). "s/d" = receta sin datos nutricionales todavía.</p>
    } @else {
      <div class="skeleton" style="height: 420px"></div>
    }

    @if (picker(); as pk) {
      <app-recipe-picker [day]="pk.day" [meal]="pk.meal" (picked)="assign($event)" (closed)="picker.set(null)" />
    }
  `,
  styles: `
    .week-nav { margin-top: .3rem; }
    .actions { margin-bottom: 1rem; }
    .generator { margin-bottom: 1rem; display: grid; gap: .8rem; }
    .gen-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: .8rem 1.2rem; align-items: end; }
    .board { display: grid; grid-template-columns: 120px repeat(7, minmax(130px, 1fr)); gap: 6px; overflow-x: auto; padding-bottom: .5rem; }
    .day-head { text-align: center; padding: .4rem; border-radius: 10px; display: grid; }
    .day-head.today { background: var(--primary-soft); color: var(--primary-strong); }
    .meal-head { display: flex; align-items: center; gap: .4rem; font-weight: 700; font-size: .88rem; color: var(--muted); }
    .cell { min-height: 108px; border-radius: 12px; border: 1px dashed var(--line); padding: .5rem; display: flex; flex-direction: column;
      gap: .25rem; background: transparent; }
    .cell.filled { border-style: solid; background: var(--surface); box-shadow: var(--shadow); }
    .cell.eaten { background: var(--primary-soft); }
    .title { font-weight: 650; font-size: .85rem; color: var(--text); text-decoration: none; line-height: 1.25; }
    .title:hover { color: var(--primary-strong); }
    .kcal { font-size: .75rem; color: var(--muted); }
    .cell-actions { margin-top: auto; display: flex; align-items: center; gap: .15rem; }
    .cell-actions .check { margin-right: auto; font-size: .75rem; }
    .add { flex: 1; border: 0; background: none; color: var(--muted); cursor: pointer; font: inherit; font-size: .85rem; border-radius: 10px; }
    .add:hover { background: var(--surface-2); color: var(--primary-strong); }
    .cell.total { min-height: auto; border: 0; gap: .35rem; font-size: .85rem; }
    .progress span.over { background: var(--accent); }
    .total { align-self: center; }
  `,
})
export class WeekPlanner {
  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  private readonly week = inject(WeekStore);

  protected readonly Math = Math;
  protected readonly days = DAYS;
  protected readonly allMeals = MEALS;
  protected readonly dayLabel = DAY_LABEL;
  protected readonly mealLabel = MEAL_LABEL;
  protected readonly mealIcon = MEAL_ICON;
  protected readonly today = toIso(new Date());

  protected readonly plan = signal<Plan | null>(null);
  protected readonly profile = signal<Profile | null>(null);
  protected readonly picker = signal<{ day: DayOfWeek; meal: MealType } | null>(null);
  protected readonly showGenerator = signal(false);
  protected readonly generating = signal(false);
  protected readonly genMeals = signal<MealType[]>(['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER']);
  protected gen: GenerateRequest = { strategy: 'balanced', maxRepeatsPerWeek: 2, keepExisting: false, vegetarian: false, preference: '' };

  /** Filas visibles: las comidas del perfil + cualquier comida que ya tenga algo planificado. */
  protected readonly visibleMeals = computed(() => {
    const enabled = this.profile()?.enabledMeals ?? MEALS;
    const used = new Set(this.plan()?.meals.map((m) => m.mealType));
    return MEALS.filter((m) => enabled.includes(m) || used.has(m));
  });

  protected readonly weekLabel = computed(() => {
    const start = this.week.weekStart();
    const end = addDays(start, 6);
    return `${start.slice(8)}/${start.slice(5, 7)} – ${end.slice(8)}/${end.slice(5, 7)}/${end.slice(0, 4)}`;
  });

  constructor() {
    this.api.profile().subscribe((p) => {
      this.profile.set(p);
      this.genMeals.set(p.enabledMeals);
      this.gen.vegetarian = p.vegetarian;
    });
    effect(() => this.load(this.week.weekStart()));
  }

  private load(weekStart: string): void {
    this.plan.set(null);
    this.api.week(weekStart).subscribe((p) => this.plan.set(p));
  }

  dateOf(index: number): string {
    return addDays(this.week.weekStart(), index);
  }

  slotOf(day: DayOfWeek, meal: MealType): PlannedMeal | undefined {
    return this.plan()?.meals.find((m) => m.day === day && m.mealType === meal);
  }

  dayKcal(day: DayOfWeek): number {
    return (this.plan()?.meals ?? []).filter((m) => m.day === day)
      .reduce((sum, m) => sum + (m.nutritionPerServing?.kcal ?? 0), 0);
  }

  shiftWeek(days: number): void {
    this.week.weekStart.set(addDays(this.week.weekStart(), days));
  }

  goToday(): void {
    this.week.weekStart.set(mondayOf(new Date()));
  }

  openPicker(day: DayOfWeek, meal: MealType): void {
    this.picker.set({ day, meal });
  }

  assign(recipe: RecipeSummary): void {
    const pk = this.picker();
    const p = this.plan();
    if (!pk || !p) return;
    this.picker.set(null);
    this.api.assign(p.id, pk.day, pk.meal, recipe.id).subscribe((plan) => this.plan.set(plan));
  }

  remove(day: DayOfWeek, meal: MealType): void {
    this.api.removeMeal(this.plan()!.id, day, meal).subscribe((plan) => this.plan.set(plan));
  }

  toggleEaten(slot: PlannedMeal): void {
    this.api.markEaten(this.plan()!.id, slot.day, slot.mealType, !slot.eaten).subscribe((plan) => this.plan.set(plan));
  }

  setPeople(delta: number): void {
    const p = this.plan()!;
    this.api.people(p.id, p.people + delta).subscribe((plan) => this.plan.set(plan));
  }

  clear(): void {
    if (!confirm('¿Vaciar todas las comidas de esta semana?')) return;
    this.api.clear(this.plan()!.id).subscribe((plan) => this.plan.set(plan));
  }

  copyPrevious(): void {
    this.api.copyPrevious(this.plan()!.id).subscribe((plan) => {
      this.plan.set(plan);
      this.toasts.show('Semana anterior copiada', 'ok');
    });
  }

  toggleGenMeal(m: MealType): void {
    this.genMeals.update((list) => (list.includes(m) ? list.filter((x) => x !== m) : [...list, m]));
  }

  generate(): void {
    this.generating.set(true);
    this.api.generate(this.week.weekStart(), { ...this.gen, meals: this.genMeals() }).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.generating.set(false);
        this.showGenerator.set(false);
        this.toasts.show(`Plan generado: ${plan.meals.length} comidas`, 'ok');
      },
      error: () => this.generating.set(false),
    });
  }
}
