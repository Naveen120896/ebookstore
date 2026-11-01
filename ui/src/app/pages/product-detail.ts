import { Component, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { ProductCard } from '../components/product-card';
import { Product } from '../models';
import { errMsg } from '../util';
import { Loading } from '../components/loading';

@Component({
  selector: 'app-product-detail',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, ProductCard, Loading],
  template: `
    <a routerLink="/products" class="muted">← Back to books</a>
    @if (error) { <div class="alert error-box">{{ error }}</div> }
    @if (!product && !error) { <app-loading message="Loading book details..." /> }

    @if (product) {
      <div class="detail">
        <img [src]="product.imageUrl" [alt]="product.title">
        <div>
          <h2>{{ product.title }}</h2>
          <div class="muted">by {{ product.author }}</div>
          <div class="muted small">{{ product.category.name }}@if (product.brand) { · {{ product.brand.name }} }</div>
          <div class="price big">{{ product.price | currency:'INR' }}</div>
          <p>{{ product.description }}</p>
          <p class="delivery">🚚 Tentative delivery by <strong>{{ product.tentativeDeliveryDate | date:'fullDate' }}</strong></p>
          <p class="muted">{{ product.stock > 0 ? product.stock + ' in stock' : 'Out of stock' }}</p>

          @if (product.stock > 0) {
            <div class="row">
              <label class="inline">Qty
                <input type="number" min="1" [max]="product.stock" [(ngModel)]="qty" class="qty">
              </label>
              <button class="btn" (click)="add()" [disabled]="busy">{{ busy ? 'Adding...' : 'Add to cart' }}</button>
            </div>
          }
          @if (message) { <div class="alert ok-box">{{ message }} <a routerLink="/cart">View cart</a></div> }
        </div>
      </div>

      @if (related.length > 0) {
        <h2 class="section">Related books</h2>
        <div class="grid">
          @for (r of related; track r.id) { <app-product-card [product]="r" /> }
        </div>
      }
    }
  `
})
export class ProductDetail {
  private route = inject(ActivatedRoute);
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private cart = inject(CartService);
  private router = inject(Router);

  product: Product | null = null;
  related: Product[] = [];
  qty = 1;
  busy = false;
  message = '';
  error = '';

  constructor() {
    // re-load when the user clicks a related book (same component, new id)
    this.route.paramMap.subscribe(pm => this.load(Number(pm.get('id'))));
  }

  private load(id: number): void {
    this.product = null;
    this.related = [];
    this.error = '';
    this.message = '';
    this.qty = 1;
    this.api.product(id).subscribe({ next: p => this.product = p, error: e => this.error = errMsg(e) });
    this.api.related(id).subscribe({ next: r => this.related = r, error: () => this.related = [] });
  }

  add(): void {
    if (!this.product) { return; }
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    const qty = Math.max(1, Math.floor(Number(this.qty) || 1));
    this.busy = true;
    this.error = '';
    this.message = '';
    this.cart.addItem(this.product.id, qty).subscribe({
      next: () => { this.busy = false; this.message = 'Added to your cart.'; },
      error: e => { this.busy = false; this.error = errMsg(e); }
    });
  }
}
