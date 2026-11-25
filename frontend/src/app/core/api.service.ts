import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Category, DayOfWeek, DraftJob, RecipeCommand, GenerateRequest, MealType, Page, Plan, Profile, Progress, RecipeDetail, RecipeSummary,
  ScoredRecipe, ShoppingList, WeeklyStats, WeightEntry,
} from './models';

/** Cliente de la API. Todas las URLs son relativas => pasan por el Gateway (mismo origen, cookie de sesión). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // ---------------------------------------------------------------- recetas
  categories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  recipes(filters: Record<string, string | number | null | undefined>, page = 0): Observable<Page<RecipeSummary>> {
    let params = new HttpParams().set('page', page).set('size', 24);
    for (const [k, v] of Object.entries(filters)) {
      if (v !== null && v !== undefined && v !== '') params = params.set(k, v);
    }
    return this.http.get<Page<RecipeSummary>>('/api/recipes', { params });
  }

  semanticSearch(query: string, mealType?: MealType | null, limit = 12): Observable<ScoredRecipe[]> {
    let params = new HttpParams().set('query', query).set('limit', limit);
    if (mealType) params = params.set('mealType', mealType);
    return this.http.get<ScoredRecipe[]>('/api/recipes/semantic-search', { params });
  }

  recipe(id: number, servings?: number): Observable<RecipeDetail> {
    const params = servings ? new HttpParams().set('servings', servings) : undefined;
    return this.http.get<RecipeDetail>(`/api/recipes/${id}`, { params });
  }

  createRecipe(cmd: RecipeCommand): Observable<RecipeDetail> {
    return this.http.post<RecipeDetail>('/api/recipes', cmd);
  }

  updateRecipe(id: number, cmd: RecipeCommand): Observable<RecipeDetail> {
    return this.http.put<RecipeDetail>(`/api/recipes/${id}`, cmd);
  }

  deleteRecipe(id: number): Observable<void> {
    return this.http.delete<void>(`/api/recipes/${id}`);
  }

  /** Borrador con IA: devuelve 202 + id; después se consulta con draft(id) hasta que termine. */
  startDraft(body: { ingredients: string[]; mealType: MealType | null; servings: number; notes: string }): Observable<DraftJob> {
    return this.http.post<DraftJob>('/api/recipes/drafts', body);
  }

  draft(jobId: string): Observable<DraftJob> {
    return this.http.get<DraftJob>(`/api/recipes/drafts/${jobId}`);
  }

  similar(id: number): Observable<ScoredRecipe[]> {
    return this.http.get<ScoredRecipe[]>(`/api/recipes/${id}/similar`, { params: { limit: 6 } });
  }

  // ---------------------------------------------------------------- perfil
  profile(): Observable<Profile> {
    return this.http.get<Profile>('/api/profile');
  }

  updateProfile(body: Partial<Profile>): Observable<Profile> {
    return this.http.put<Profile>('/api/profile', body);
  }

  // ---------------------------------------------------------------- planes
  week(date?: string): Observable<Plan> {
    return this.http.get<Plan>('/api/plans/week', { params: date ? { date } : {} });
  }

  assign(planId: number, day: DayOfWeek, meal: MealType, recipeId: number, servings?: number): Observable<Plan> {
    return this.http.put<Plan>(`/api/plans/${planId}/meals/${day}/${meal}`, { recipeId, servings });
  }

  removeMeal(planId: number, day: DayOfWeek, meal: MealType): Observable<Plan> {
    return this.http.delete<Plan>(`/api/plans/${planId}/meals/${day}/${meal}`);
  }

  markEaten(planId: number, day: DayOfWeek, meal: MealType, value: boolean): Observable<Plan> {
    return this.http.patch<Plan>(`/api/plans/${planId}/meals/${day}/${meal}/eaten`, null, { params: { value } });
  }

  people(planId: number, value: number): Observable<Plan> {
    return this.http.patch<Plan>(`/api/plans/${planId}/people`, null, { params: { value } });
  }

  clear(planId: number): Observable<Plan> {
    return this.http.post<Plan>(`/api/plans/${planId}/clear`, null);
  }

  copyPrevious(planId: number): Observable<Plan> {
    return this.http.post<Plan>(`/api/plans/${planId}/copy-previous`, null);
  }

  generate(date: string, body: GenerateRequest): Observable<Plan> {
    return this.http.post<Plan>('/api/plans/generate', body, { params: { date } });
  }

  stats(planId: number): Observable<WeeklyStats> {
    return this.http.get<WeeklyStats>(`/api/plans/${planId}/stats`);
  }

  shoppingList(planId: number): Observable<ShoppingList> {
    return this.http.get<ShoppingList>(`/api/plans/${planId}/shopping-list`);
  }

  // ---------------------------------------------------------------- progreso
  progress(weeks = 12): Observable<Progress> {
    return this.http.get<Progress>('/api/progress', { params: { weeks } });
  }

  logWeight(entry: WeightEntry): Observable<WeightEntry[]> {
    return this.http.post<WeightEntry[]>('/api/progress/weights', entry);
  }
}
