import { Component, computed, effect, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ChartConfiguration } from 'chart.js';
import { ApiService } from '../../core/api.service';
import { DAY_LABEL, addDays } from '../../core/labels';
import { Progress, WeeklyStats } from '../../core/models';
import { WeekStore } from '../../core/week.store';
import { ChartComponent } from '../../shared/chart.component';

const css = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

@Component({
  selector: 'app-stats-page',
  imports: [DecimalPipe, ChartComponent],
  template: `
    <div class="page-head">
      <div>
        <h1>Estadísticas</h1>
        <div class="row">
          <button class="btn icon ghost" (click)="shift(-7)">‹</button>
          <b>Semana del {{ week.weekStart().slice(8) }}/{{ week.weekStart().slice(5, 7) }}</b>
          <button class="btn icon ghost" (click)="shift(7)">›</button>
        </div>
      </div>
    </div>

    @if (stats(); as s) {
      <div class="grid kpis">
        <div class="card kpi">
          <span class="label">Promedio diario</span>
          <span class="value">{{ s.dailyAverage.kcal | number: '1.0-0' }} kcal</span>
          @if (s.targets) { <span class="muted small">Objetivo {{ s.targets.dailyKcal | number }} kcal ({{ s.kcalAdherencePct }}%)</span> }
        </div>
        <div class="card kpi">
          <span class="label">Proteína diaria</span>
          <span class="value">{{ s.dailyAverage.proteinG | number: '1.0-0' }} g</span>
          @if (s.targets) { <span class="muted small">Objetivo {{ s.targets.proteinG }} g ({{ s.proteinAdherencePct }}%)</span> }
        </div>
        <div class="card kpi">
          <span class="label">Cambio proyectado</span>
          <span class="value" [class.good]="(s.projectedWeeklyChangeKg ?? 0) < 0">
            {{ s.projectedWeeklyChangeKg != null ? (s.projectedWeeklyChangeKg > 0 ? '+' : '') + s.projectedWeeklyChangeKg + ' kg' : '—' }}
          </span>
          <span class="muted small">por semana si seguís el plan</span>
        </div>
        <div class="card kpi">
          <span class="label">Datos nutricionales</span>
          <span class="value">{{ s.dataCoveragePct }}%</span>
          <span class="muted small">de {{ s.mealsPlanned }} comidas planificadas</span>
        </div>
      </div>

      @if (s.dataCoveragePct < 80 && s.mealsPlanned > 0) {
        <p class="card warn">⚠️ Algunas recetas todavía no tienen datos nutricionales, así que los totales pueden quedar por debajo de lo real.</p>
      }

      <div class="grid cols-2">
        <section class="card">
          <h2>Calorías por día</h2>
          <app-chart [config]="kcalChart()!" />
        </section>
        <section class="card">
          <h2>Distribución de macros</h2>
          <app-chart [config]="macroChart()!" />
          <p class="muted small">Recomendado en déficit: proteína alta (~25-30% de las kcal).</p>
        </section>
      </div>
    } @else {
      <div class="skeleton" style="height: 300px"></div>
    }

    @if (progress(); as p) {
      <h2 class="section">Progreso</h2>
      <div class="grid cols-2">
        <section class="card">
          <h3>Peso</h3>
          @if (p.weights.length) {
            <app-chart [config]="weightChart()!" />
            @if (p.lostKg != null) { <p class="muted small">Llevás {{ p.lostKg > 0 ? 'bajados ' + p.lostKg : 'aumentados ' + (-p.lostKg) }} kg desde tu primer registro.</p> }
          } @else {
            <p class="muted">Registrá tu peso en el perfil para ver la evolución.</p>
          }
        </section>
        <section class="card">
          <h3>Semanas anteriores</h3>
          @if (p.weeks.length) {
            <app-chart [config]="weeksChart()!" />
          } @else {
            <p class="muted">Todavía no hay semanas planificadas.</p>
          }
        </section>
      </div>
    }
  `,
  styles: `
    .kpis { grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); margin-bottom: 1rem; }
    .good { color: var(--primary-strong); }
    .warn { background: #fff8ea; border-color: #f4dfb6; color: #5c4300; margin-bottom: 1rem; }
    .section { margin-top: 2rem; }
  `,
})
export class StatsPage {
  private readonly api = inject(ApiService);
  protected readonly week = inject(WeekStore);
  protected readonly stats = signal<WeeklyStats | null>(null);
  protected readonly progress = signal<Progress | null>(null);

  protected readonly kcalChart = computed<ChartConfiguration | null>(() => {
    const s = this.stats();
    if (!s) return null;
    return {
      type: 'bar',
      data: {
        labels: s.days.map((d) => DAY_LABEL[d.dayOfWeek].slice(0, 3)),
        datasets: [
          { label: 'kcal', data: s.days.map((d) => d.total.kcal ?? 0), backgroundColor: css('--primary'), borderRadius: 6 },
          ...(s.targets ? [{ type: 'line' as const, label: 'Objetivo', data: s.days.map(() => s.targets!.dailyKcal),
            borderColor: css('--accent'), borderDash: [6, 4], pointRadius: 0 }] : []),
        ],
      },
      options: { plugins: { legend: { display: true } }, scales: { y: { beginAtZero: true } } },
    } as ChartConfiguration;
  });

  protected readonly macroChart = computed<ChartConfiguration | null>(() => {
    const a = this.stats()?.dailyAverage;
    if (!a) return null;
    return {
      type: 'doughnut',
      data: {
        labels: ['Proteínas', 'Carbohidratos', 'Grasas'],
        datasets: [{
          data: [(a.proteinG ?? 0) * 4, (a.carbsG ?? 0) * 4, (a.fatG ?? 0) * 9].map(Math.round),
          backgroundColor: [css('--protein'), css('--carbs'), css('--fat')],
        }],
      },
      options: { plugins: { tooltip: { callbacks: { label: (c) => `${c.label}: ${c.parsed} kcal` } } } },
    } as ChartConfiguration;
  });

  protected readonly weightChart = computed<ChartConfiguration | null>(() => {
    const p = this.progress();
    if (!p) return null;
    return {
      type: 'line',
      data: {
        labels: p.weights.map((w) => w.date.slice(5)),
        datasets: [
          { label: 'Peso (kg)', data: p.weights.map((w) => w.weightKg), borderColor: css('--primary'), tension: .3 },
          ...(p.targetWeightKg ? [{ label: 'Objetivo', data: p.weights.map(() => p.targetWeightKg), borderColor: css('--accent'),
            borderDash: [6, 4], pointRadius: 0 }] : []),
        ],
      },
    } as ChartConfiguration;
  });

  protected readonly weeksChart = computed<ChartConfiguration | null>(() => {
    const p = this.progress();
    if (!p) return null;
    return {
      type: 'line',
      data: {
        labels: p.weeks.map((w) => w.weekStart.slice(5)),
        datasets: [
          { label: 'kcal/día', data: p.weeks.map((w) => w.avgDailyKcal ?? 0), borderColor: css('--primary'), yAxisID: 'y' },
          { label: 'Proteína g/día', data: p.weeks.map((w) => w.avgDailyProteinG ?? 0), borderColor: css('--protein'), yAxisID: 'y1' },
        ],
      },
      options: { scales: { y: { position: 'left' }, y1: { position: 'right', grid: { drawOnChartArea: false } } } },
    } as ChartConfiguration;
  });

  constructor() {
    effect(() => {
      this.stats.set(null);
      this.api.week(this.week.weekStart()).subscribe((plan) => this.api.stats(plan.id).subscribe((s) => this.stats.set(s)));
    });
    this.api.progress(16).subscribe((p) => this.progress.set(p));
  }

  shift(days: number): void {
    this.week.weekStart.set(addDays(this.week.weekStart(), days));
  }
}
