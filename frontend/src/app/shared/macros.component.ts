import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Nutrition } from '../core/models';

/** Fila compacta de macros: kcal · P · C · G */
@Component({
  selector: 'app-macros',
  imports: [DecimalPipe],
  template: `
    @if (n()?.kcal) {
      <span class="macros">
        <b>{{ n()!.kcal | number: '1.0-0' }} kcal</b>
        @if (n()!.proteinG != null) { <span class="p">P {{ n()!.proteinG | number: '1.0-0' }}g</span> }
        @if (n()!.carbsG != null) { <span class="c">C {{ n()!.carbsG | number: '1.0-0' }}g</span> }
        @if (n()!.fatG != null) { <span class="f">G {{ n()!.fatG | number: '1.0-0' }}g</span> }
      </span>
    } @else {
      <span class="macros muted">Sin datos nutricionales</span>
    }
  `,
  styles: `
    .macros { display: inline-flex; gap: .5rem; flex-wrap: wrap; font-size: .82rem; align-items: baseline; }
    .p { color: var(--protein); } .c { color: var(--carbs); } .f { color: var(--fat); }
  `,
})
export class MacrosComponent {
  readonly n = input<Nutrition | null | undefined>();
}
