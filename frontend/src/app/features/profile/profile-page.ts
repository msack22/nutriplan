import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ChartConfiguration } from 'chart.js';
import { ApiService } from '../../core/api.service';
import { MEALS, MEAL_LABEL, toIso } from '../../core/labels';
import { MealType, Profile, WeightEntry } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { ChartComponent } from '../../shared/chart.component';

/** Formulario REACTIVO (FormGroup tipado + validadores): el estándar para formularios complejos en Angular. */
@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule, FormsModule, DecimalPipe, ChartComponent],
  template: `
    <div class="page-head"><h1>Mi perfil</h1></div>
    <div class="layout">
      <form class="card grid" [formGroup]="form" (ngSubmit)="save()">
        <h2>Datos personales</h2>
        <div class="fields">
          <label class="field">Sexo
            <select formControlName="sex"><option value="FEMALE">Femenino</option><option value="MALE">Masculino</option></select>
          </label>
          <label class="field">Fecha de nacimiento <input type="date" formControlName="birthDate" /></label>
          <label class="field">Altura (cm) <input type="number" formControlName="heightCm" /></label>
          <label class="field">Peso actual (kg) <input type="number" step="0.1" formControlName="currentWeightKg" /></label>
          <label class="field">Peso objetivo (kg) <input type="number" step="0.1" formControlName="targetWeightKg" /></label>
          <label class="field">Actividad física
            <select formControlName="activityLevel">
              <option value="SEDENTARY">Sedentario (oficina, sin ejercicio)</option>
              <option value="LIGHT">Ligera (1-3 días/semana)</option>
              <option value="MODERATE">Moderada (3-5 días/semana)</option>
              <option value="ACTIVE">Alta (6-7 días/semana)</option>
              <option value="VERY_ACTIVE">Muy alta (trabajo físico + entreno)</option>
            </select>
          </label>
          <label class="field">Objetivo
            <select formControlName="goal">
              <option value="LOSE_SLOW">Bajar despacio (~0,25 kg/sem)</option>
              <option value="LOSE">Bajar (~0,5 kg/sem)</option>
              <option value="LOSE_FAST">Bajar rápido (~0,7 kg/sem)</option>
              <option value="MAINTAIN">Mantener</option>
            </select>
          </label>
          <label class="field">Personas por defecto <input type="number" min="1" max="12" formControlName="defaultPeople" /></label>
        </div>
        <h2>Preferencias</h2>
        <div class="row">
          @for (m of meals; track m) {
            <label class="check"><input type="checkbox" [checked]="enabledMeals().includes(m)" (change)="toggleMeal(m)" /> {{ mealLabel[m] }}</label>
          }
        </div>
        <label class="check"><input type="checkbox" formControlName="vegetarian" /> Vegetariano</label>
        <label class="field">Ingredientes que no como (separados por coma)
          <input formControlName="excluded" placeholder="ej: hongo, atún, berenjena" />
        </label>
        <div class="row">
          <button class="btn primary" type="submit" [disabled]="form.invalid || saving()">Guardar</button>
          @if (form.invalid) { <span class="muted small">Revisá los valores (altura 100-250 cm, peso 30-350 kg).</span> }
        </div>
      </form>

      <aside class="grid">
        <section class="card">
          <h2>Tu objetivo diario</h2>
          @if (profile()?.targets; as t) {
            <div class="kpi"><span class="value">{{ t.dailyKcal | number }} kcal</span><span class="label">para tu objetivo</span></div>
            <div class="macros">
              <div><b>{{ t.proteinG }} g</b><span class="muted small">proteína</span></div>
              <div><b>{{ t.carbsG }} g</b><span class="muted small">carbohidratos</span></div>
              <div><b>{{ t.fatG }} g</b><span class="muted small">grasas</span></div>
            </div>
            <p class="muted small">Metabolismo basal {{ t.bmr | number }} kcal · Gasto diario {{ t.tdee | number }} kcal
              @if (t.weeksToGoal) { · ~{{ t.weeksToGoal }} semanas a tu objetivo }</p>
            <p class="muted small">Cálculo orientativo (Mifflin-St Jeor). Consultá con un profesional de la salud.</p>
          } @else {
            <p class="muted">Completá sexo, fecha de nacimiento, altura y peso.</p>
          }
        </section>
        <section class="card">
          <h2>Registrar peso</h2>
          <div class="row">
            <input type="date" [(ngModel)]="weightDate" />
            <input type="number" step="0.1" [(ngModel)]="weightKg" placeholder="kg" class="kg" />
            <button class="btn primary small" (click)="logWeight()" [disabled]="!weightKg">Guardar</button>
          </div>
          @if (weights().length > 1) { <app-chart [config]="weightChart()!" [height]="200" /> }
        </section>
      </aside>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr 360px; gap: 1rem; align-items: start; }
    .fields { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: .8rem; }
    .macros { display: flex; gap: 1.5rem; margin: .75rem 0; }
    .macros div { display: grid; }
    .kg { width: 90px; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class ProfilePage {
  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly meals = MEALS;
  protected readonly mealLabel = MEAL_LABEL;
  protected readonly profile = signal<Profile | null>(null);
  protected readonly saving = signal(false);
  protected readonly enabledMeals = signal<MealType[]>([...MEALS]);
  protected readonly weights = signal<WeightEntry[]>([]);
  protected weightDate = toIso(new Date());
  protected weightKg: number | null = null;

  protected readonly form = this.fb.group({
    sex: ['FEMALE' as 'MALE' | 'FEMALE'],
    birthDate: [''],
    heightCm: [null as number | null, [Validators.min(100), Validators.max(250)]],
    currentWeightKg: [null as number | null, [Validators.min(30), Validators.max(350)]],
    targetWeightKg: [null as number | null, [Validators.min(30), Validators.max(350)]],
    activityLevel: ['LIGHT' as Profile['activityLevel']],
    goal: ['LOSE' as Profile['goal']],
    defaultPeople: [1, [Validators.required, Validators.min(1), Validators.max(12)]],
    vegetarian: [false],
    excluded: [''],
  });

  protected readonly weightChart = computed<ChartConfiguration | null>(() => ({
    type: 'line',
    data: {
      labels: this.weights().map((w) => w.date.slice(5)),
      datasets: [{ label: 'kg', data: this.weights().map((w) => w.weightKg), borderColor: '#2f9e5b', tension: .3 }],
    },
    options: { plugins: { legend: { display: false } } },
  }));

  constructor() {
    this.api.profile().subscribe((p) => this.fill(p));
    this.api.progress(52).subscribe((p) => this.weights.set(p.weights));
  }

  private fill(p: Profile): void {
    this.profile.set(p);
    this.enabledMeals.set(p.enabledMeals);
    this.form.patchValue({
      sex: p.sex ?? 'FEMALE', birthDate: p.birthDate ?? '', heightCm: p.heightCm, currentWeightKg: p.currentWeightKg,
      targetWeightKg: p.targetWeightKg, activityLevel: p.activityLevel, goal: p.goal, defaultPeople: p.defaultPeople,
      vegetarian: p.vegetarian, excluded: p.excludedIngredients.join(', '),
    });
  }

  toggleMeal(m: MealType): void {
    this.enabledMeals.update((list) => (list.includes(m) ? list.filter((x) => x !== m) : [...list, m]));
  }

  save(): void {
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.api.updateProfile({
      sex: v.sex, birthDate: v.birthDate || null, heightCm: v.heightCm, currentWeightKg: v.currentWeightKg,
      targetWeightKg: v.targetWeightKg, activityLevel: v.activityLevel, goal: v.goal, defaultPeople: v.defaultPeople,
      vegetarian: v.vegetarian, enabledMeals: this.enabledMeals(),
      excludedIngredients: v.excluded.split(',').map((s) => s.trim()).filter(Boolean),
    }).subscribe({
      next: (p) => {
        this.fill(p);
        this.saving.set(false);
        this.toasts.show('Perfil guardado', 'ok');
      },
      error: () => this.saving.set(false),
    });
  }

  logWeight(): void {
    this.api.logWeight({ date: this.weightDate, weightKg: this.weightKg!, note: null }).subscribe((w) => {
      this.weights.set(w);
      this.weightKg = null;
      this.toasts.show('Peso registrado', 'ok');
      this.api.profile().subscribe((p) => this.fill(p));
    });
  }
}
