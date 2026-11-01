import { Component, inject, input } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Product } from '../models';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { errMsg } from '../util';

@Component({
  selector: 'app-product-card',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    <div class="card">
      <a [routerLink]="['/products', product().id]">
        <img [src]="product().imageUrl" [alt]="product().title" loading="lazy">
      </a>
      <div class="card-body">
        <a class="title" [routerLink]="['/products', product().id]">{{ product().title }}</a>
        <div class="muted">{{ product().author }}</div>
        <div class="price">{{ product().price | currency:'INR' }}</div>
        <div class="muted small">Delivery by {{ product().tentativeDeliveryDate | date:'dd MMM yyyy' }}</div>
        <button class="btn" (click)="add()" [disabled]="product().stock === 0 || busy">
          {{ product().stock === 0 ? 'Out of stock' : (busy ? 'Adding...' : (added ? 'Added ✓' : 'Add to cart')) }}
        </button>
        @if (error) { <div class="error small">{{ error }}</div> }
      </div>
    </div>
  `
})
export class ProductCard {
  product = input.required<Product>();
  private cart = inject(CartService);
  private auth = inject(AuthService);
  private router = inject(Router);
  busy = false;
  added = false;
  error = '';

  add(): void {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.busy = true;
    this.error = '';
    this.cart.addItem(this.product().id, 1).subscribe({
      next: () => { this.busy = false; this.added = true; setTimeout(() => this.added = false, 1500); },
      error: e => { this.busy = false; this.error = errMsg(e); }
    });
  }
}
