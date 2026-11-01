import { Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { CartService } from './services/cart.service';
import { LoadingService } from './services/loading.service';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe],
  template: `
    @if (loading.active$ | async) { <div class="progress-bar"></div> }
    <header class="nav">
      <a class="brand" routerLink="/products">📚 E-Bookstore</a>
      <nav>
        <a routerLink="/products" routerLinkActive="active">Books</a>
        @if (auth.user$ | async; as user) {
          <a routerLink="/orders" routerLinkActive="active">My Orders</a>
          <a routerLink="/cart" routerLinkActive="active">Cart <span class="badge-count">{{ cart.count$ | async }}</span></a>
          <span class="user">👤 {{ user.fullName }} · 🎁 {{ user.giftPoints }} pts</span>
          <button class="btn-link" (click)="logout()">Logout</button>
        } @else {
          <a routerLink="/login" routerLinkActive="active">Login</a>
          <a routerLink="/register" routerLinkActive="active">Register</a>
        }
        <button class="theme-toggle" (click)="theme.toggle()"
                [attr.aria-label]="theme.theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'"
                [title]="theme.theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'">
          {{ theme.theme === 'dark' ? '☀️' : '🌙' }}
        </button>
      </nav>
    </header>
    <main class="container">
      <router-outlet />
    </main>
  `
})
export class App {
  auth = inject(AuthService);
  cart = inject(CartService);
  loading = inject(LoadingService);
  theme = inject(ThemeService);
  private router = inject(Router);

  constructor() {
    if (this.auth.isLoggedIn()) {
      this.cart.refresh();
      this.auth.refreshUser();
    }
  }

  logout(): void {
    this.auth.logout();
    this.cart.refresh();
    this.router.navigate(['/login']);
  }
}
