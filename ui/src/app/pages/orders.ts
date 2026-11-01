import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { Order, OrderItem } from '../models';
import { errMsg } from '../util';
import { Loading } from '../components/loading';

@Component({
  selector: 'app-orders',
  imports: [RouterLink, CurrencyPipe, DatePipe, Loading],
  template: `
    <h2>My orders</h2>
    @if (error) { <div class="alert error-box">{{ error }}</div> }
    @if (message) { <div class="alert ok-box">{{ message }} <a routerLink="/cart">View cart</a></div> }
    @if (!loaded) { <app-loading message="Loading your orders..." /> }
    @if (loaded && orders.length === 0) {
      <div class="panel"><p>You have not placed any orders yet.</p><a class="btn" routerLink="/products">Browse books</a></div>
    }

    @for (o of orders; track o.id) {
      <div class="panel">
        <div class="order-head">
          <div>
            <strong>Order #{{ o.id }}</strong>
            <span class="muted"> · {{ o.createdAt | date:'dd MMM yyyy, h:mm a' }}</span>
          </div>
          <span class="status" [class]="'status ' + o.status.toLowerCase()">{{ o.status }}</span>
        </div>

        @for (i of o.items; track i.productId) {
          <div class="line">
            <span>{{ i.title }} × {{ i.quantity }}
              <span class="muted small"> · delivery by {{ i.expectedDelivery | date:'dd MMM' }}</span></span>
            <span>
              {{ i.unitPrice * i.quantity | currency:'INR' }}
              <button class="btn-link" (click)="buyAgain(i)" [disabled]="addingId !== null">
                {{ addingId === i.productId ? 'Adding...' : 'Buy it again' }}
              </button>
            </span>
          </div>
        }

        <div class="line total">
          <span>Total@if (o.giftPointsUsed > 0) { <span class="muted small"> (after {{ o.giftPointsUsed }} gift points)</span> }</span>
          <span>{{ o.total | currency:'INR' }}</span>
        </div>

        <div class="actions">
          @if (o.status === 'PLACED') { <a class="btn small" [routerLink]="['/payment', o.id]">Pay now</a> }
          @if (canCancel(o)) { <button class="btn small danger-btn" (click)="cancel(o)" [disabled]="cancellingId !== null">
              {{ cancellingId === o.id ? 'Cancelling...' : 'Cancel order' }}
            </button> }
          @if (!canCancel(o) && (o.status === 'PLACED' || o.status === 'PAID')) {
            <span class="muted small">Cancellation window (48 hours) has closed.</span>
          }
        </div>
      </div>
    }
  `
})
export class Orders implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private cart = inject(CartService);
  orders: Order[] = [];
  loaded = false;
  cancellingId: number | null = null;
  addingId: number | null = null;
  error = '';
  message = '';

  ngOnInit(): void {
    this.api.orders().subscribe({
      next: o => { this.orders = o; this.loaded = true; },
      error: e => { this.error = errMsg(e); this.loaded = true; }
    });
  }

  canCancel(o: Order): boolean {
    return (o.status === 'PLACED' || o.status === 'PAID') && new Date(o.cancellableUntil).getTime() > Date.now();
  }

  cancel(o: Order): void {
    if (!confirm(`Cancel order #${o.id}?`)) { return; }
    this.error = '';
    this.message = '';
    this.cancellingId = o.id;
    this.api.cancelOrder(o.id).subscribe({
      next: updated => {
        this.orders = this.orders.map(x => x.id === updated.id ? updated : x);
        this.auth.refreshUser();
        this.message = `Order #${o.id} was cancelled.`;
        this.cancellingId = null;
      },
      error: e => { this.error = errMsg(e); this.cancellingId = null; }
    });
  }

  buyAgain(item: OrderItem): void {
    this.error = '';
    this.message = '';
    this.addingId = item.productId;
    this.cart.addItem(item.productId, 1).subscribe({
      next: () => { this.message = `"${item.title}" was added to your cart.`; this.addingId = null; },
      error: e => { this.error = errMsg(e); this.addingId = null; }
    });
  }
}
