import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

/** Lazy loading: cada pantalla se descarga recién cuando se navega a ella (bundle inicial chico). */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home').then((m) => m.Home), title: 'NutriPlan' },
  {
    path: 'recetas',
    canActivate: [authGuard],
    loadComponent: () => import('./features/recipes/recipe-list').then((m) => m.RecipeList),
    title: 'Recetas · NutriPlan',
  },
  {
    path: 'recetas/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/recipes/recipe-detail').then((m) => m.RecipeDetailPage),
    title: 'Receta · NutriPlan',
  },
  {
    path: 'plan',
    canActivate: [authGuard],
    loadComponent: () => import('./features/planner/week-planner').then((m) => m.WeekPlanner),
    title: 'Mi semana · NutriPlan',
  },
  {
    path: 'estadisticas',
    canActivate: [authGuard],
    loadComponent: () => import('./features/stats/stats-page').then((m) => m.StatsPage),
    title: 'Estadísticas · NutriPlan',
  },
  {
    path: 'compras',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shopping/shopping-list').then((m) => m.ShoppingListPage),
    title: 'Lista de compras · NutriPlan',
  },
  {
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile-page').then((m) => m.ProfilePage),
    title: 'Mi perfil · NutriPlan',
  },
  { path: '**', redirectTo: '' },
];
