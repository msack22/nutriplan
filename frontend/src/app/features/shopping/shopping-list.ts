import { Component, computed, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { CHAIN_LABEL, addDays, fmtQty, money } from '../../core/labels';
import { ItemPrice, ShoppingItem, ShoppingList } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { WeekStore } from '../../core/week.store';

@Component({
  selector: 'app-shopping-list',
  imports: [DatePipe],
  template: `
    <div class="page-head">
      <div>
        <h1>Lista de compras</h1>
        <div class="row">
          <button class="btn icon ghost no-print" (click)="shift(-7)">‹</button>
          <b>Semana del {{ week.weekStart().slice(8) }}/{{ week.weekStart().slice(5, 7) }}</b>
          <button class="btn icon ghost no-print" (click)="shift(7)">›</button>
          @if (list()) { <span class="muted small">· para {{ list()!.people }} {{ list()!.people === 1 ? 'persona' : 'personas' }}</span> }
        </div>
      </div>
      <div class="row no-print">
        <button class="btn ghost" (click)="copy()">📋 Copiar (WhatsApp)</button>
        <button class="btn ghost" (click)="print()">🖨️ Imprimir</button>
      </div>
    </div>

    @if (loading()) {
      <div class="skeleton" style="height: 300px"></div>
      <p class="muted small">Calculando cantidades y consultando precios… la primera vez puede tardar unos segundos.</p>
    } @else if (list(); as l) {
      @if (l.prices?.chains?.length) {
        <section class="chains">
          @for (c of l.prices!.chains; track c.chain) {
            <button class="card chain" [class.selected]="chain() === c.chain" (click)="chain.set(c.chain)">
              <span class="muted small">{{ chainLabel[c.chain] ?? c.chain }}
                @if (c.chain === cheapest()) { <span class="badge">más barata</span> }
                @if (c.chain === mostComplete()) { <span class="badge alt">más completa</span> }
              </span>
              <span class="total">{{ money(c.total) }}</span>
              <span class="muted small">{{ c.itemsPriced }} productos cotizados · {{ c.itemsMissing }} sin precio</span>
            </button>
          }
        </section>
        <p class="muted small">Estimación con precios de catálogos online
          @if (l.prices!.pricesUpdatedAt) { (actualizados {{ l.prices!.pricesUpdatedAt | date: 'dd/MM HH:mm' }}) }.
          Se actualizan todos los lunes. No incluye condimentos "a gusto".</p>
      }

      <div class="aisles">
        @for (group of grouped(); track group.label) {
          <section class="card">
            <h2>{{ group.label }} <span class="muted small">({{ group.items.length }})</span></h2>
            <ul>
              @for (i of group.items; track i.name + i.unit) {
                @let price = priceOf(i.name);
                <li [class.done]="checked().has(key(i))">
                  <label class="check">
                    <input type="checkbox" [checked]="checked().has(key(i))" (change)="toggle(i)" />
                    <span class="name">{{ i.name }}</span>
                  </label>
                  <span class="qty">{{ fmt(i.quantity, i.unit) }}</span>
                  @if (price) {
                    <a class="price small" [href]="price.productUrl" target="_blank" rel="noopener noreferrer" [title]="price.productName">
                      {{ price.packages }}× {{ money(price.unitPrice) }}
                    </a>
                  } @else { <span></span> }
                  <span class="recipes muted small">{{ i.recipes.join(', ') }}</span>
                </li>
              }
            </ul>
          </section>
        } @empty {
          <div class="empty card">Tu semana no tiene comidas planificadas todavía.</div>
        }
      </div>
    }
  `,
  styles: `
    .chains { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: .8rem; margin-bottom: .5rem; }
    .chain { display: grid; gap: .2rem; text-align: left; cursor: pointer; font: inherit; color: var(--text); }
    .chain.selected { border-color: var(--primary); outline: 2px solid var(--primary-soft); }
    .chain .total { font-size: 1.5rem; font-weight: 800; }
    .badge.alt { background: var(--primary-soft); color: var(--primary-strong); }
    .aisles { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 1rem; margin-top: 1rem; }
    ul { list-style: none; padding: 0; margin: 0; }
    li { display: grid; grid-template-columns: 1fr auto auto; gap: .1rem .6rem; padding: .5rem 0; border-top: 1px solid var(--line); align-items: center; }
    li.done .name, li.done .qty { text-decoration: line-through; opacity: .5; }
    .name { text-transform: capitalize; font-weight: 600; }
    .qty { font-weight: 700; color: var(--primary-strong); white-space: nowrap; }
    .price { white-space: nowrap; text-decoration: none; }
    .recipes { grid-column: 1 / -1; padding-left: 1.6rem; }
    @media print { .aisles { grid-template-columns: 1fr 1fr; } .recipes { display: none; } }
  `,
})
export class ShoppingListPage {
  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  protected readonly week = inject(WeekStore);
  protected readonly chainLabel = CHAIN_LABEL;
  protected readonly money = money;
  protected readonly fmt = fmtQty;

  protected readonly list = signal<ShoppingList | null>(null);
  protected readonly loading = signal(true);
  protected readonly chain = signal<string | null>(null);
  /** Ítems tachados: se guardan en localStorage (estado de UI, no hace falta backend). */
  protected readonly checked = signal<Set<string>>(new Set());

  protected readonly grouped = computed(() => {
    const groups = new Map<string, ShoppingItem[]>();
    for (const i of this.list()?.items ?? []) {
      groups.set(i.aisleLabel, [...(groups.get(i.aisleLabel) ?? []), i]);
    }
    return [...groups.entries()].map(([label, items]) => ({ label, items }));
  });

  /** Ojo: comparar solo el total engaña si una cadena no tiene precio de varios productos. */
  protected readonly cheapest = computed(() =>
    [...(this.list()?.prices?.chains ?? [])].filter((c) => c.itemsPriced > 0).sort((a, b) => a.total - b.total)[0]?.chain);
  protected readonly mostComplete = computed(() =>
    [...(this.list()?.prices?.chains ?? [])].sort((a, b) => b.itemsPriced - a.itemsPriced)[0]?.chain);

  private readonly pricesByIngredient = computed(() => {
    const map = new Map<string, ItemPrice>();
    for (const p of this.list()?.prices?.items ?? []) {
      if (p.chain === this.chain()) map.set(p.ingredient, p);
    }
    return map;
  });

  constructor() {
    effect(() => {
      const weekStart = this.week.weekStart();
      this.loading.set(true);
      this.api.week(weekStart).subscribe((plan) =>
        this.api.shoppingList(plan.id).subscribe({
          next: (l) => {
            this.list.set(l);
            this.chain.set(l.prices?.chains?.[0]?.chain ?? null);
            this.checked.set(new Set(JSON.parse(localStorage.getItem(this.storageKey()) ?? '[]')));
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        }),
      );
    });
  }

  key(i: ShoppingItem): string {
    return `${i.name}|${i.unit}`;
  }

  priceOf(ingredient: string): ItemPrice | undefined {
    return this.pricesByIngredient().get(ingredient);
  }

  toggle(i: ShoppingItem): void {
    const next = new Set(this.checked());
    next.has(this.key(i)) ? next.delete(this.key(i)) : next.add(this.key(i));
    this.checked.set(next);
    localStorage.setItem(this.storageKey(), JSON.stringify([...next]));
  }

  shift(days: number): void {
    this.week.weekStart.set(addDays(this.week.weekStart(), days));
  }

  print(): void {
    window.print();
  }

  async copy(): Promise<void> {
    const text = this.grouped().map((g) => `*${g.label}*\n` + g.items
      .map((i) => `- ${i.name}: ${fmtQty(i.quantity, i.unit)}`).join('\n')).join('\n\n');
    await navigator.clipboard.writeText(`🛒 Lista de compras (semana ${this.week.weekStart()})\n\n${text}`);
    this.toasts.show('Lista copiada al portapapeles', 'ok');
  }

  private storageKey(): string {
    return `nutriplan.checked.${this.week.weekStart()}`;
  }
}
