import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MEALS, MEAL_LABEL, TAG_LABEL } from '../../core/labels';
import { Category, MealType, RecipeSummary } from '../../core/models';
import { RecipeCard } from './recipe-card';

@Component({
  selector: 'app-recipe-list',
  imports: [FormsModule, RouterLink, RecipeCard],
  template: `
    <div class="page-head">
      <div>
        <h1>Recetas</h1>
        <p class="muted">{{ total() }} recetas saludables. Probá la búsqueda inteligente: describí lo que tenés ganas de comer.</p>
      </div>
      <a class="btn primary" routerLink="/recetas/nueva">➕ Crear mi receta</a>
    </div>

    <div class="card filters">
      <div class="row">
        <input class="search" [(ngModel)]="query" (keyup.enter)="search()"
               [placeholder]="semantic() ? 'Ej: algo liviano con pollo para la cena' : 'Buscar por nombre…'" />
        <label class="check"><input type="checkbox" [ngModel]="semantic()" (ngModelChange)="semantic.set($event)" /> 🤖 Búsqueda inteligente</label>
        <button class="btn primary" (click)="search()">Buscar</button>
      </div>
      <div class="row">
        <button class="chip" [class.on]="!category() && !mine()" (click)="setCategory(null)">Todas</button>
        <button class="chip" [class.on]="mine()" (click)="toggleMine()">⭐ Mis recetas</button>
        @for (c of categories(); track c.slug) {
          <button class="chip" [class.on]="category() === c.slug" (click)="setCategory(c.slug)">{{ c.emoji }} {{ c.name }}</button>
        }
      </div>
      <div class="row">
        <select [(ngModel)]="mealType" (change)="search()">
          <option [ngValue]="null">Cualquier comida</option>
          @for (m of meals; track m) { <option [ngValue]="m">{{ mealLabel[m] }}</option> }
        </select>
        @if (!semantic()) {
          <select [(ngModel)]="tag" (change)="search()">
            <option [ngValue]="null">Cualquier etiqueta</option>
            @for (t of tags; track t) { <option [ngValue]="t">{{ tagLabel[t] }}</option> }
          </select>
          <label class="check">Máx. kcal <input type="number" class="num" [(ngModel)]="maxKcal" (change)="search()" min="0" step="50" /></label>
        }
      </div>
    </div>

    @if (loading()) {
      <div class="grid cards">@for (i of [1,2,3,4,5,6]; track i) { <div class="skeleton"></div> }</div>
    } @else {
      <div class="grid cards">
        @for (r of results(); track r.recipe.id) {
          <app-recipe-card [r]="r.recipe" [score]="r.score" />
        } @empty {
          <div class="empty">No encontramos recetas con esos filtros.</div>
        }
      </div>
      @if (hasMore()) {
        <div class="row center"><button class="btn ghost" (click)="loadMore()">Cargar más</button></div>
      }
    }
  `,
  styles: `
    .filters { display: grid; gap: .8rem; margin-bottom: 1.2rem; }
    .search { flex: 1; min-width: 240px; font-size: 1rem; }
    .num { width: 90px; }
    .center { justify-content: center; margin-top: 1.2rem; }
  `,
})
export class RecipeList {
  private readonly api = inject(ApiService);
  protected readonly meals = MEALS;
  protected readonly mealLabel = MEAL_LABEL;
  protected readonly tagLabel = TAG_LABEL;
  protected readonly tags = Object.keys(TAG_LABEL);

  protected readonly categories = signal<Category[]>([]);
  protected readonly results = signal<{ recipe: RecipeSummary; score: number | null }[]>([]);
  protected readonly loading = signal(true);
  protected readonly total = signal(0);
  protected readonly hasMore = signal(false);
  protected readonly semantic = signal(false);
  protected readonly category = signal<string | null>(null);
  protected readonly mine = signal(false);

  protected query = '';
  protected mealType: MealType | null = null;
  protected tag: string | null = null;
  protected maxKcal: number | null = null;
  private page = 0;

  constructor() {
    this.api.categories().subscribe((c) => this.categories.set(c));
    this.search();
  }

  toggleMine(): void {
    this.mine.set(!this.mine());
    this.category.set(null);
    this.semantic.set(false);
    this.search();
  }

  setCategory(slug: string | null): void {
    this.category.set(slug);
    this.mine.set(false);
    this.semantic.set(false);
    this.search();
  }

  search(): void {
    this.loading.set(true);
    this.page = 0;
    if (this.semantic() && this.query.trim()) {
      this.api.semanticSearch(this.query, this.mealType, 24).subscribe({
        next: (hits) => {
          this.results.set(hits.map((h) => ({ recipe: h.recipe, score: h.score })));
          this.hasMore.set(false);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
      return;
    }
    this.fetch(false);
  }

  loadMore(): void {
    this.page++;
    this.fetch(true);
  }

  private fetch(append: boolean): void {
    this.api.recipes({ q: this.query, category: this.category(), mealType: this.mealType, tag: this.tag,
      maxKcal: this.maxKcal, mine: this.mine() ? 'true' : null }, this.page).subscribe({
      next: (p) => {
        const mapped = p.content.map((r) => ({ recipe: r, score: null }));
        this.results.update((prev) => (append ? [...prev, ...mapped] : mapped));
        this.total.set(p.totalElements);
        this.hasMore.set(p.page + 1 < p.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
