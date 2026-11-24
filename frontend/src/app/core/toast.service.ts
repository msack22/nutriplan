import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  text: string;
  kind: 'ok' | 'error' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private seq = 0;

  show(text: string, kind: Toast['kind'] = 'info'): void {
    const toast = { id: ++this.seq, text, kind };
    this.toasts.update((t) => [...t, toast]);
    setTimeout(() => this.toasts.update((t) => t.filter((x) => x.id !== toast.id)), 4000);
  }
}
