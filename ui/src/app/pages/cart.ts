import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';
import { CartService } from '../services/cart.service';
import { Cart } from '../models';
import { Loading } from '../components/loading';
import { errMsg } from '../util';

@Component({
  selector: 'app-cart',
  imports: [RouterLink, CurrencyPipe, Loading],
  template: `
    <h2>Your cart</h2>
    @if (error) { <div class="alert error-box">{{ error }}</div> }
    @if (loading) { <app-loading message="Loading your cart..." /> }

    @if (cart && cart.items.length === 0) {
      <div class="panel"><p>Your cart is empty.</p><a class="btn" routerLink="/products">Browse books</a></div>
    }

    @if (cart && cart.items.length > 0) {
      <table class="table">
        <thead><tr><th>Book</th><th>Price</th><th>Quantity</th><th>Total</th><th></th></tr></thead>
        <tbody>
          @for (item of cart.items; track item.product.id) {
            <tr>
              <td>
                <a [routerLink]="['/products', item.product.id]">{{ item.product.title }}</a>
                <div class="muted small">{{ item.product.author }}</div>
              </td>
              <td>{{ item.product.price | currency:'INR' }}</td>
              <td>
                <button class="btn small secondary" (click)="change(item.product.id, item.quantity - 1)" [disabled]="busy || item.quantity <= 1">−</button>
                <span class="qty-text">{{ item.quantity }}</span>
                <button class="btn small secondary" (click)="change(item.product.id, item.quantity + 1)" [disabled]="busy || item.quantity >= item.product.stock">+</button>
              </td>
              <td>{{ item.lineTotal | currency:'INR' }}</td>
              <td><button class="btn-link danger" (click)="remove(item.product.id)" [disabled]="busy">Remove</button></td>
            </tr>
          }
        </tbody>
      </table>
      <div class="summary">
        <div>Subtotal: <strong>{{ cart.subtotal | currency:'INR' }}</strong></div>
        <a class="btn" routerLink="/checkout">Proceed to checkout</a>
      </div>
    }
  `
})
export class CartPage implements OnInit {
  private api = inject(ApiService);
  private cartSvc = inject(CartService);
  cart: Cart | null = null;
  loading = true;
  busy = false;
  error = '';

  ngOnInit(): void {
    this.api.getCart().subscribe({
      next: c => { this.set(c); this.loading = false; },
      error: e => { this.error = errMsg(e); this.loading = false; }
    });
  }

  change(productId: number, qty: number): void {
    this.error = '';
    this.busy = true;
    this.api.updateCart(productId, qty).subscribe({
      next: c => { this.set(c); this.busy = false; },
      error: e => { this.error = errMsg(e); this.busy = false; }
    });
  }

  remove(productId: number): void {
    this.error = '';
    this.busy = true;
    this.api.removeFromCart(productId).subscribe({
      next: c => { this.set(c); this.busy = false; },
      error: e => { this.error = errMsg(e); this.busy = false; }
    });
  }

  private set(c: Cart): void {
    this.cart = c;
    this.cartSvc.setFromCart(c);
  }
}
