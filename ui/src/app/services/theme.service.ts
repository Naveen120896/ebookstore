import { Injectable } from '@angular/core';

export type Theme = 'light' | 'dark';

/** Light/dark theme. The choice is saved in localStorage; first visit follows the OS setting. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  theme: Theme = 'light';

  constructor() {
    let saved: string | null = null;
    try { saved = localStorage.getItem('theme'); } catch { /* storage blocked */ }
    const prefersDark = typeof window !== 'undefined' && !!window.matchMedia
      && window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.apply(saved === 'dark' || saved === 'light' ? saved : (prefersDark ? 'dark' : 'light'));
  }

  toggle(): void {
    this.apply(this.theme === 'dark' ? 'light' : 'dark');
  }

  private apply(t: Theme): void {
    this.theme = t;
    document.documentElement.setAttribute('data-theme', t);
    try { localStorage.setItem('theme', t); } catch { /* storage blocked */ }
  }
}
