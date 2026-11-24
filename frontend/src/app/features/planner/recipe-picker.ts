import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { DAY_LABEL, MEAL_LABEL } from '../../core/labels';
import { DayOfWeek, MealType, RecipeSummary } from '../../core/models';
import { MacrosComponent } from '../../shared/macros.component';

/** Modal para elegir la receta de un hueco del plan (búsqueda semántica o por nombre). */
@Component({
  selector: 'app-recipe-picker',
  imports: [FormsModule, MacrosComponent],
  template: `
    <div class="modal-backdrop" (click)="closed.emit()">
      <div class="modal" (click)="$event.stopPropagation()" role="dialog" aria-modal="true">
        <div class="row">
          <h2>{{ mealLabel[meal()] }} · {{ dayLabel[day()] }}</h2>
          <span class="spacer"></span>
          <button class="btn icon ghost" (click)="closed.emit()" aria-label="Cerrar">✕</button>
        </div>
        <div class="row">
          <input class="q" [(ngModel)]="query" (keyup.enter)="search()" placeholder="¿Qué tenés ganas de comer? (ej: pescado con verduras)" />
          <button class="btn primary" (click)="search()">Buscar</button>
        </div>
        <ul class="list">
          @for (r of results(); track r.id) {
            <li>
              <button (click)="picked.emit(r)">
                <span>{{ r.categoryEmoji }} {{ r.title }}</span>
                <app-macros [n]="r.nutritionPerServing" />
              </button>
            </li>
          } @empty {
            <li class="muted">{{ loading() ? 'Buscando…' : 'Sin resultados' }}</li>
          }
        </ul>
      </div>
    </div>
  `,
  styles: `
    .q { flex: 1; }
    .list { list-style: none; padding: 0; margin: 1rem 0 0; display: grid; gap: .4rem; }
    .list button { width: 100%; text-align: left; display: grid; gap: .2rem; padding: .7rem .8rem; border-radius: 10px;
      border: 1px solid var(--line); background: var(--surface); color: var(--text); cursor: pointer; font: inherit; }
    .list button:hover { border-color: var(--primary); background: var(--primary-soft); }
  `,
})
export class RecipePicker {
  readonly day = input.required<DayOfWeek>();
  readonly meal = input.required<MealType>();
  readonly picked = output<RecipeSummary>();
  readonly closed = output<void>();

  private readonly api = inject(ApiService);
  protected readonly mealLabel = MEAL_LABEL;
  protected readonly dayLabel = DAY_LABEL;
  protected readonly results = signal<RecipeSummary[]>([]);
  protected readonly loading = signal(false);
  protected query = '';

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.loading.set(true);
    const done = (list: RecipeSummary[]) => {
      this.results.set(list);
      this.loading.set(false);
    };
    if (this.query.trim()) {
      this.api.semanticSearch(this.query, this.meal(), 20).subscribe((h) => done(h.map((x) => x.recipe)));
    } else {
      this.api.recipes({ mealType: this.meal() }).subscribe((p) => done(p.content));
    }
  }
}
