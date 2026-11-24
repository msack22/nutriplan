import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Me } from './models';

/**
 * Autenticación con patrón BFF: el navegador NO maneja tokens. Solo tiene una cookie de sesión
 * HttpOnly emitida por el Gateway. Login = navegar a /oauth2/authorization/keycloak
 * (Authorization Code + PKCE lo resuelve el Gateway con Keycloak).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly user = signal<Me | null>(null);
  readonly loaded = signal(false);
  readonly isLoggedIn = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.roles.includes('ADMIN') ?? false);

  /** Se llama al iniciar la app (provideAppInitializer). 401 = sin sesión. */
  async load(): Promise<void> {
    try {
      this.user.set(await firstValueFrom(this.http.get<Me>('/api/me')));
    } catch {
      this.user.set(null);
    } finally {
      this.loaded.set(true);
    }
  }

  login(): void {
    window.location.href = '/oauth2/authorization/keycloak';
  }

  /** POST /logout lleva el header X-XSRF-TOKEN automáticamente (HttpClient + cookie XSRF-TOKEN). */
  async logout(): Promise<void> {
    const res = await firstValueFrom(this.http.post<{ logoutUrl: string }>('/logout', null));
    this.user.set(null);
    window.location.href = res.logoutUrl ?? '/';
  }
}
