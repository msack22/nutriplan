import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TAG_LABEL } from '../../core/labels';
import { RecipeSummary } from '../../core/models';
import { MacrosComponent } from '../../shared/macros.component';

@Component({
  selector: 'app-recipe-card',
  imports: [RouterLink, MacrosComponent],
  template: `
    <a class="card recipe" [routerLink]="['/recetas', r().id]">
      <div class="emoji">{{ r().categoryEmoji }}</div>
      <div class="cat muted small">{{ r().categoryName }}</div>
      <h3>{{ r().title }}</h3>
      <app-macros [n]="r().nutritionPerServing" />
      <div class="tags">
        @if (score() != null) { <span class="chip ai">{{ (score()! * 100).toFixed(0) }}% afinidad</span> }
        @if (r().nutritionSource === 'ESTIMATED') { <span class="chip ai">IA</span> }
        @for (t of r().tags.slice(0, 3); track t) { <span class="chip">{{ tagLabel[t] ?? t }}</span> }
      </div>
    </a>
  `,
  styles: `
    .recipe { display: grid; gap: .35rem; text-decoration: none; color: var(--text); height: 100%;
      transition: transform .12s ease, border-color .12s; align-content: start; }
    .recipe:hover { transform: translateY(-2px); border-color: var(--primary); }
    .emoji { font-size: 1.8rem; }
    h3 { font-size: 1.02rem; margin: 0; }
    .tags { display: flex; gap: .3rem; flex-wrap: wrap; margin-top: .2rem; }
  `,
})
export class RecipeCard {
  readonly r = input.required<RecipeSummary>();
  readonly score = input<number | null>(null);
  protected readonly tagLabel = TAG_LABEL;
}
