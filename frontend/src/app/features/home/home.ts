import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { DAYS, MEAL_ICON, MEAL_LABEL } from '../../core/labels';
import { Plan, Profile } from '../../core/models';
import { MacrosComponent } from '../../shared/macros.component';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DecimalPipe, MacrosComponent],
  template: `
    @if (!auth.isLoggedIn()) {
      <section class="hero">
        <div>
          <h1>Planificá tu semana.<br /><span>Comé rico, bajá de peso.</span></h1>
          <p class="muted lead">
            Armá tu menú de desayuno, almuerzo, merienda y cena con recetas saludables, mirá tus calorías y
            proteínas de la semana y llevá la lista del súper con precios estimados.
          </p>
          <button class="btn primary" (click)="auth.login()">Ingresar / Crear cuenta</button>
        </div>
        <ul class="features">
          <li><b>🤖 Plan automático</b><span>Genera la semana según tu objetivo calórico y tus gustos</span></li>
          <li><b>🔎 Búsqueda inteligente</b><span>"algo liviano con pollo" encuentra recetas por significado</span></li>
          <li><b>📊 Estadísticas</b><span>Calorías y macros por día vs. tu objetivo</span></li>
          <li><b>🛒 Compras con precios</b><span>Lista por góndola y costo estimado en Día, Jumbo y Carrefour</span></li>
        </ul>
      </section>
    } @else {
      <div class="page-head">
        <div>
          <h1>Hola, {{ auth.user()?.name }} 👋</h1>
          <p class="muted">Esto es lo que tenés planificado para hoy.</p>
        </div>
        <a class="btn primary" routerLink="/plan">Ir a mi semana</a>
      </div>

      @if (profile() && !profile()!.complete) {
        <div class="card warn-card">
          <b>Completá tu perfil</b> (peso, altura, edad y actividad) para calcular tu objetivo diario de calorías.
          <a routerLink="/perfil">Completar ahora →</a>
        </div>
      }

      <div class="grid cols-2">
        <section class="card">
          <h2>Hoy</h2>
          @for (m of todayMeals(); track m.mealType) {
            <a class="meal" [routerLink]="['/recetas', m.recipeId]">
              <span class="icon">{{ icons[m.mealType] }}</span>
              <span><small class="muted">{{ labels[m.mealType] }}</small><br />{{ m.recipeTitle }}</span>
              <app-macros [n]="m.nutritionPerServing" />
            </a>
          } @empty {
            <p class="muted">No hay comidas planificadas para hoy. <a routerLink="/plan">Armá tu semana</a>.</p>
          }
        </section>
        <section class="card">
          <h2>Tu objetivo</h2>
          @if (profile()?.targets; as t) {
            <div class="targets">
              <div class="kpi"><span class="value">{{ t.dailyKcal | number }}</span><span class="label">kcal / día</span></div>
              <div class="kpi"><span class="value">{{ t.proteinG }} g</span><span class="label">proteína</span></div>
              @if (t.weeklyLossKg) {
                <div class="kpi"><span class="value">-{{ t.weeklyLossKg }} kg</span><span class="label">por semana</span></div>
              }
            </div>
            @if (t.weeksToGoal) {
              <p class="muted small">A este ritmo llegás a tu peso objetivo en ~{{ t.weeksToGoal }} semanas.</p>
            }
          } @else {
            <p class="muted">Completá tu perfil para ver tu objetivo.</p>
          }
        </section>
      </div>
    }
  `,
  styles: `
    .hero { display: grid; grid-template-columns: 1.2fr 1fr; gap: 2.5rem; align-items: center; padding: 3rem 0; }
    .hero h1 { font-size: clamp(2rem, 5vw, 3.2rem); }
    .hero h1 span { color: var(--primary); }
    .lead { font-size: 1.1rem; max-width: 560px; }
    .features { list-style: none; padding: 0; margin: 0; display: grid; gap: .8rem; }
    .features li { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: 1rem;
      display: grid; gap: .2rem; box-shadow: var(--shadow); }
    .features span { color: var(--muted); font-size: .92rem; }
    .warn-card { margin-bottom: 1rem; background: #fff8ea; border-color: #f4dfb6; color: #5c4300; }
    .meal { display: grid; grid-template-columns: 2rem 1fr; gap: .2rem .6rem; padding: .6rem 0; border-top: 1px solid var(--line);
      color: var(--text); text-decoration: none; }
    .meal:first-of-type { border-top: 0; }
    .meal app-macros { grid-column: 2; }
    .icon { font-size: 1.3rem; grid-row: span 2; }
    .targets { display: flex; gap: 2rem; flex-wrap: wrap; margin: .5rem 0; }
    @media (max-width: 860px) { .hero { grid-template-columns: 1fr; padding: 1rem 0; } }
  `,
})
export class Home {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  protected readonly labels = MEAL_LABEL;
  protected readonly icons = MEAL_ICON;
  protected readonly plan = signal<Plan | null>(null);
  protected readonly profile = signal<Profile | null>(null);

  protected readonly todayMeals = computed(() => {
    const today = DAYS[(new Date().getDay() + 6) % 7];
    return (this.plan()?.meals ?? []).filter((m) => m.day === today);
  });

  constructor() {
    if (this.auth.isLoggedIn()) {
      forkJoin([this.api.week(), this.api.profile()]).subscribe(([plan, profile]) => {
        this.plan.set(plan);
        this.profile.set(profile);
      });
    }
  }
}
