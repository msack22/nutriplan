import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { ToastService } from './core/toast.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly toasts = inject(ToastService);
  protected readonly menuOpen = signal(false);

  protected readonly links = [
    { path: '/plan', label: 'Mi semana', icon: '🗓️' },
    { path: '/recetas', label: 'Recetas', icon: '🥗' },
    { path: '/estadisticas', label: 'Estadísticas', icon: '📊' },
    { path: '/compras', label: 'Compras', icon: '🛒' },
    { path: '/perfil', label: 'Perfil', icon: '👤' },
  ];
}
