import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, switchMap, takeWhile, timer } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { MEALS, MEAL_LABEL } from '../../core/labels';
import { Category, DraftJob, MealType, RecipeCommand, RecipeDetail } from '../../core/models';
import { ToastService } from '../../core/toast.service';

/**
 * Alta/edición de recetas propias.
 * - Panel "Generar con IA": pide un borrador (HTTP 202) y hace POLLING cada 3 s con RxJS
 *   (timer + switchMap + takeWhile) hasta que esté listo; el usuario puede seguir escribiendo.
 * - Formulario: ingredientes y pasos como texto, una línea por ítem (el backend los interpreta).
 */
@Component({
  selector: 'app-recipe-editor',
  imports: [FormsModule, RouterLink],
  template: `
    <a routerLink="/recetas" class="small">← Volver a recetas</a>
    <div class="page-head">
      <div>
        <h1>{{ id() ? 'Editar mi receta' : 'Crear mi receta' }}</h1>
        <p class="muted">Tus recetas son privadas: solo vos las ves, y el plan automático también puede usarlas.</p>
      </div>
    </div>

    <section class="card ai">
      <h2>✨ Generar con IA a partir de mis ingredientes</h2>
      <p class="muted small">Escribí lo que tenés en la heladera y la IA arma una receta saludable. Después la podés editar.</p>
      <div class="chips">
        @for (i of aiIngredients(); track i) {
          <span class="chip on">{{ i }} <button class="x" (click)="removeAiIngredient(i)" aria-label="Quitar">✕</button></span>
        }
        <input class="chip-input" [value]="aiInput()" (input)="aiInput.set($any($event.target).value)" (keydown)="onAiKey($event)"
               (blur)="commitAiInput()" placeholder="Ej: pollo, zapallito, queso… (Enter o coma para agregar)" [disabled]="generating()" />
      </div>
      <div class="row">
        <select [(ngModel)]="aiMeal" [disabled]="generating()">
          <option [ngValue]="null">Cualquier comida</option>
          @for (m of meals; track m) { <option [ngValue]="m">{{ mealLabel[m] }}</option> }
        </select>
        <label class="check">Porciones <input type="number" min="1" max="8" [(ngModel)]="aiServings" class="num" [disabled]="generating()" /></label>
        <input class="grow" [(ngModel)]="aiNotes" placeholder="Preferencias (opcional): al horno, sin lácteos, rápida…" maxlength="200" [disabled]="generating()" />
        <button class="btn primary" (click)="generate()" [disabled]="generating() || (aiIngredients().length === 0 && !aiInput().trim())">
          {{ generating() ? 'Generando…' : '✨ Generar receta' }}
        </button>
      </div>
      @if (generating()) {
        <div class="progress-row">
          <div class="progress"><span [style.width.%]="progressPct()"></span></div>
          <span class="muted small">La IA corre en tu computadora (sin GPU): suele tardar 2-5 minutos ({{ elapsed() }} s). Podés ir completando el formulario.</span>
        </div>
      }
    </section>

    <form class="card grid" (ngSubmit)="save()">
      <div class="fields">
        <label class="field wide">Título <input [(ngModel)]="title" name="title" required maxlength="120" /></label>
        <label class="field">Categoría
          <select [(ngModel)]="categorySlug" name="category" required>
            @for (c of categories(); track c.slug) { <option [ngValue]="c.slug">{{ c.emoji }} {{ c.name }}</option> }
          </select>
        </label>
        <label class="field">Rinde (porciones) <input type="number" min="1" max="24" [(ngModel)]="servings" name="servings" required /></label>
      </div>

      <div class="field">Para qué comidas sirve
        <div class="row">
          @for (m of meals; track m) {
            <label class="check"><input type="checkbox" [checked]="mealTypes().includes(m)" (change)="toggleMeal(m)" /> {{ mealLabel[m] }}</label>
          }
        </div>
      </div>

      <div class="two">
        <label class="field">Ingredientes (uno por línea)
          <textarea rows="10" [(ngModel)]="ingredientsText" name="ingredients" required
                    placeholder="200 g de pechuga de pollo&#10;1 zapallito&#10;1/2 taza de queso rallado&#10;Sal y pimienta al gusto"></textarea>
        </label>
        <label class="field">Preparación (un paso por línea)
          <textarea rows="10" [(ngModel)]="stepsText" name="steps"
                    placeholder="Cortar el pollo en cubos.&#10;Saltear con el zapallito.&#10;Gratinar con el queso."></textarea>
        </label>
      </div>

      <label class="field">Notas (opcional) <input [(ngModel)]="notes" name="notes" maxlength="2000" /></label>

      <div class="field">Nutrición por porción (opcional: si la dejás vacía la estimamos con IA)
        <div class="row">
          <label class="check">kcal <input type="number" min="0" [(ngModel)]="kcal" name="kcal" class="num" /></label>
          <label class="check">Proteínas g <input type="number" min="0" [(ngModel)]="proteinG" name="p" class="num" /></label>
          <label class="check">Carbohidratos g <input type="number" min="0" [(ngModel)]="carbsG" name="c" class="num" /></label>
          <label class="check">Grasas g <input type="number" min="0" [(ngModel)]="fatG" name="f" class="num" /></label>
        </div>
      </div>

      <div class="row">
        <button class="btn primary" type="submit" [disabled]="saving() || !canSave()">{{ saving() ? 'Guardando…' : 'Guardar receta' }}</button>
        <a class="btn ghost" routerLink="/recetas">Cancelar</a>
      </div>
    </form>
  `,
  styles: `
    .ai { margin-bottom: 1rem; display: grid; gap: .7rem; background: linear-gradient(135deg, var(--surface), var(--primary-soft)); }
    .chips { display: flex; flex-wrap: wrap; gap: .4rem; align-items: center; padding: .4rem; border: 1px solid var(--line);
      border-radius: 10px; background: var(--surface); }
    .chip .x { border: 0; background: none; cursor: pointer; color: inherit; padding: 0 0 0 .2rem; }
    .chip-input { border: 0; flex: 1; min-width: 220px; outline: none; }
    .grow { flex: 1; min-width: 220px; }
    .num { width: 90px; }
    .progress-row { display: grid; gap: .35rem; }
    .fields { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: .8rem; }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: .8rem; }
    textarea { width: 100%; resize: vertical; font: inherit; }
    @media (max-width: 800px) { .fields, .two { grid-template-columns: 1fr; } }
  `,
})
export class RecipeEditor implements OnInit {
  /** Viene de /recetas/:id/editar (undefined al crear). */
  readonly id = input<string>();

  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly meals = MEALS;
  protected readonly mealLabel = MEAL_LABEL;
  protected readonly categories = signal<Category[]>([]);
  protected readonly mealTypes = signal<MealType[]>(['LUNCH', 'DINNER']);
  protected readonly saving = signal(false);

  // ---- formulario
  protected title = '';
  protected categorySlug = 'comidas-principales';
  protected servings = 2;
  protected ingredientsText = '';
  protected stepsText = '';
  protected notes = '';
  protected kcal: number | null = null;
  protected proteinG: number | null = null;
  protected carbsG: number | null = null;
  protected fatG: number | null = null;

  // ---- IA
  protected readonly aiIngredients = signal<string[]>([]);
  protected readonly aiInput = signal('');
  protected aiMeal: MealType | null = null;
  protected aiServings = 2;
  protected aiNotes = '';
  protected readonly generating = signal(false);
  protected readonly elapsed = signal(0);
  protected readonly progressPct = computed(() => Math.min(95, (this.elapsed() / 180) * 100));
  private polling?: Subscription;

  ngOnInit(): void {
    this.api.categories().subscribe((c) => this.categories.set(c));
    const id = this.id();
    if (id) {
      this.api.recipe(Number(id)).subscribe((r) => this.fill(r));
    }
  }

  private fill(r: RecipeDetail): void {
    if (!r.editable) {
      this.toasts.show('Solo podés editar tus propias recetas', 'error');
      this.router.navigate(['/recetas', r.id]);
      return;
    }
    this.title = r.title;
    this.categorySlug = r.category.slug;
    this.servings = r.originalServings;
    this.mealTypes.set(r.mealTypes);
    this.ingredientsText = r.ingredients.map((i) => i.rawText).join('\n');
    this.stepsText = r.steps.join('\n');
    this.notes = r.notes ?? '';
    if (r.nutritionSource === 'MANUAL') {
      ({ kcal: this.kcal, proteinG: this.proteinG, carbsG: this.carbsG, fatG: this.fatG } = r.nutritionPerServing);
    }
  }

  onAiKey(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.commitAiInput();
      (event.target as HTMLInputElement).value = '';   // por si el binding no llegó a refrescarse
    } else if (event.key === 'Backspace' && this.aiInput() === '') {
      this.aiIngredients.update((list) => list.slice(0, -1));   // borrar el último chip
    }
  }

  /** Pasa lo escrito a chips (acepta varios separados por coma). */
  commitAiInput(): void {
    const values = this.aiInput().split(',').map((v) => v.trim()).filter((v) => v.length > 0 && v.length <= 60);
    if (values.length) {
      this.aiIngredients.update((list) => [...new Set([...list, ...values])].slice(0, 15));
    }
    this.aiInput.set('');
  }

  removeAiIngredient(value: string): void {
    this.aiIngredients.update((list) => list.filter((x) => x !== value));
  }

  toggleMeal(m: MealType): void {
    this.mealTypes.update((list) => (list.includes(m) ? list.filter((x) => x !== m) : [...list, m]));
  }

  generate(): void {
    this.commitAiInput();
    this.generating.set(true);
    this.elapsed.set(0);
    const started = Date.now();
    this.api.startDraft({ ingredients: this.aiIngredients(), mealType: this.aiMeal, servings: this.aiServings, notes: this.aiNotes })
      .subscribe({
        next: (job) => this.poll(job.id, started),
        error: () => this.generating.set(false),
      });
  }

  /** Polling: consulta cada 3 s hasta que el trabajo termine (takeWhile corta la suscripción). */
  private poll(jobId: string, started: number): void {
    this.polling?.unsubscribe();
    this.polling = timer(0, 3000)
      .pipe(
        switchMap(() => {
          this.elapsed.set(Math.round((Date.now() - started) / 1000));
          return this.api.draft(jobId);
        }),
        takeWhile((job) => job.status === 'PENDING', true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (job) => this.onDraft(job),
        error: () => this.generating.set(false),
      });
  }

  private onDraft(job: DraftJob): void {
    if (job.status === 'PENDING') return;
    this.generating.set(false);
    if (job.status === 'FAILED' || !job.draft) {
      this.toasts.show(job.error ?? 'No se pudo generar la receta', 'error');
      return;
    }
    const d = job.draft;
    this.title = d.title;
    this.servings = d.servings;
    this.ingredientsText = d.ingredients.join('\n');
    this.stepsText = d.steps.join('\n');
    if (this.aiMeal) this.mealTypes.set([this.aiMeal]);
    if (d.nutritionPerServing) {
      ({ kcal: this.kcal, proteinG: this.proteinG, carbsG: this.carbsG, fatG: this.fatG } = d.nutritionPerServing);
    }
    this.toasts.show('¡Receta generada! Revisala y guardala.', 'ok');
  }

  canSave(): boolean {
    return this.title.trim().length > 0 && this.lines(this.ingredientsText).length > 0;
  }

  save(): void {
    const hasNutrition = [this.kcal, this.proteinG, this.carbsG, this.fatG].some((v) => v != null && `${v}` !== '');
    const cmd: RecipeCommand = {
      title: this.title.trim(),
      categorySlug: this.categorySlug,
      servings: Number(this.servings) || 1,
      mealTypes: this.mealTypes(),
      ingredients: this.lines(this.ingredientsText),
      steps: this.lines(this.stepsText),
      notes: this.notes.trim() || null,
      nutritionPerServing: hasNutrition
        ? { kcal: this.num(this.kcal), proteinG: this.num(this.proteinG), carbsG: this.num(this.carbsG), fatG: this.num(this.fatG) }
        : null,
    };
    this.saving.set(true);
    const id = this.id();
    const request = id ? this.api.updateRecipe(Number(id), cmd) : this.api.createRecipe(cmd);
    request.subscribe({
      next: (r) => {
        this.toasts.show(id ? 'Receta actualizada' : 'Receta creada', 'ok');
        this.router.navigate(['/recetas', r.id]);
      },
      error: () => this.saving.set(false),
    });
  }

  private lines(text: string): string[] {
    return text.split('\n').map((l) => l.replace(/^[-•*]\s*/, '').trim()).filter((l) => l.length > 0);
  }

  private num(v: number | null): number | null {
    return v == null || `${v}` === '' ? null : Number(v);
  }
}
