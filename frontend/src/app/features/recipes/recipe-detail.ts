import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DAYS, DAY_LABEL, MEAL_LABEL, SOURCE_LABEL, TAG_LABEL, fmtQty } from '../../core/labels';
import { DayOfWeek, MealType, RecipeDetail, ScoredRecipe } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { WeekStore } from '../../core/week.store';
import { MacrosComponent } from '../../shared/macros.component';
import { RecipeCard } from './recipe-card';

@Component({
  selector: 'app-recipe-detail',
  imports: [FormsModule, RouterLink, MacrosComponent, RecipeCard],
  template: `
    @if (recipe(); as r) {
      <a routerLink="/recetas" class="small">← Volver a recetas</a>
      <div class="page-head">
        <div>
          <span class="muted small">{{ r.category.emoji }} {{ r.category.name }} · {{ r.yieldLabel ?? 'Rinde ' + r.originalServings }}</span>
          <h1>{{ r.title }}</h1>
          <div class="row">
            @for (t of r.tags; track t) { <span class="chip on">{{ tagLabel[t] ?? t }}</span> }
            <span class="chip" [class.ai]="r.nutritionSource === 'ESTIMATED'">{{ sourceLabel[r.nutritionSource] }}</span>
          </div>
        </div>
      </div>

      <div class="layout">
        <div class="grid">
          <section class="card">
            <div class="row">
              <h2>Ingredientes</h2>
              <span class="spacer"></span>
              <span class="muted small">Porciones</span>
              <button class="btn icon ghost" (click)="setServings(servings() - 1)" [disabled]="servings() <= 1">−</button>
              <b>{{ servings() }}</b>
              <button class="btn icon ghost" (click)="setServings(servings() + 1)">+</button>
            </div>
            <ul class="ingredients">
              @for (i of r.ingredients; track $index) {
                <li [class.optional]="i.optional">
                  <span class="qty">{{ i.quantity != null ? fmt(i.quantity, i.unit) : '' }}</span>
                  <span>{{ i.quantity != null ? i.name : i.rawText }} @if (i.optional) { <small class="muted">(opcional)</small> }</span>
                </li>
              }
            </ul>
          </section>
          <section class="card">
            <h2>Preparación</h2>
            <ol class="steps">@for (s of r.steps; track $index) { <li>{{ s }}</li> }</ol>
            @if (r.notes) { <p class="notes">💡 {{ r.notes }}</p> }
          </section>
        </div>

        <aside class="grid">
          <section class="card">
            <h2>Nutrición</h2>
            <p class="muted small">Por porción</p>
            <app-macros [n]="r.nutritionPerServing" />
            <p class="muted small">Total ({{ r.servings }} porciones)</p>
            <app-macros [n]="r.nutritionTotal" />
          </section>
          <section class="card">
            <h2>Agregar a mi semana</h2>
            <div class="grid">
              <select [(ngModel)]="day">@for (d of days; track d) { <option [ngValue]="d">{{ dayLabel[d] }}</option> }</select>
              <select [(ngModel)]="meal">
                @for (m of r.mealTypes; track m) { <option [ngValue]="m">{{ mealLabel[m] }}</option> }
              </select>
              <button class="btn primary" (click)="addToPlan(r)">Agregar al plan</button>
            </div>
          </section>
        </aside>
      </div>

      @if (similar().length) {
        <h2 class="similar-title">Recetas parecidas</h2>
        <div class="grid cards">
          @for (s of similar(); track s.recipe.id) { <app-recipe-card [r]="s.recipe" [score]="s.score" /> }
        </div>
      }
    } @else {
      <div class="skeleton" style="height: 400px"></div>
    }
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr 320px; gap: 1rem; align-items: start; }
    .ingredients { list-style: none; padding: 0; margin: .5rem 0 0; }
    .ingredients li { display: grid; grid-template-columns: 110px 1fr; gap: .5rem; padding: .45rem 0; border-top: 1px solid var(--line); }
    .ingredients li.optional { opacity: .7; }
    .qty { font-weight: 700; color: var(--primary-strong); }
    .steps { padding-left: 1.2rem; display: grid; gap: .55rem; }
    .notes { background: var(--surface-2); padding: .75rem; border-radius: 10px; font-size: .92rem; }
    .similar-title { margin-top: 2rem; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class RecipeDetailPage {
  /** Viene del path /recetas/:id gracias a withComponentInputBinding() */
  readonly id = input.required<string>();

  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  private readonly week = inject(WeekStore);

  protected readonly recipe = signal<RecipeDetail | null>(null);
  protected readonly similar = signal<ScoredRecipe[]>([]);
  protected readonly servings = signal(0);
  protected readonly days = DAYS;
  protected readonly dayLabel = DAY_LABEL;
  protected readonly mealLabel = MEAL_LABEL;
  protected readonly tagLabel = TAG_LABEL;
  protected readonly sourceLabel = SOURCE_LABEL;
  protected readonly fmt = fmtQty;
  protected day: DayOfWeek = DAYS[(new Date().getDay() + 6) % 7];
  protected meal: MealType = 'LUNCH';

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.recipe.set(null);
      this.api.recipe(id).subscribe((r) => {
        this.recipe.set(r);
        this.servings.set(r.servings);
        this.meal = r.mealTypes[0] ?? 'LUNCH';
      });
      this.api.similar(id).subscribe({ next: (s) => this.similar.set(s), error: () => this.similar.set([]) });
    });
  }

  setServings(n: number): void {
    if (n < 1 || n > 50) return;
    this.servings.set(n);
    this.api.recipe(Number(this.id()), n).subscribe((r) => this.recipe.set(r));
  }

  addToPlan(r: RecipeDetail): void {
    this.api.week(this.week.weekStart()).subscribe((plan) =>
      this.api.assign(plan.id, this.day, this.meal, r.id).subscribe(() =>
        this.toasts.show(`Agregada al ${DAY_LABEL[this.day].toLowerCase()} (${MEAL_LABEL[this.meal].toLowerCase()})`, 'ok'),
      ),
    );
  }
}
