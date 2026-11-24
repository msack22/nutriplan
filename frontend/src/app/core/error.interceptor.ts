import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from './toast.service';

/**
 * Interceptor funcional: manejo centralizado de errores HTTP.
 * Muestra el "detail" del ProblemDetail (RFC 9457) que devuelve el backend.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toasts = inject(ToastService);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && req.url !== '/api/me') {
        window.location.href = '/oauth2/authorization/keycloak';
      } else if (err.status === 403) {
        toasts.show('No tenés permisos para esta acción', 'error');
      } else if (err.status >= 400 && req.url !== '/api/me') {
        toasts.show(err.error?.detail ?? 'Ocurrió un error. Probá de nuevo.', 'error');
      }
      return throwError(() => err);
    }),
  );
};
