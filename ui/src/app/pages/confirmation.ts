import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';
import { Order } from '../models';
import { errMsg } from '../util';
import { Loading } from '../components/loading';

@Component({
  selector: 'app-confirmation',
  imports: [RouterLink, CurrencyPipe, DatePipe, Loading],
  template: `
    @if (error) { <div class="alert error-box">{{ error }}</div> }
    @if (!order && !error) { <app-loading message="Loading your order..." /> }
    @if (order) {
      <div class="panel confirm">
        <div class="tick">✓</div>
        <h2>Your purchase is complete</h2>
        <p>Thank you! Order <strong>#{{ order.id }}</strong> has been placed.</p>

        <table class="table">
          <thead><tr><th>Book</th><th>Qty</th><th>Price</th><th>Delivery by</th></tr></thead>
          <tbody>
            @for (i of order.items; track i.productId) {
              <tr>
                <td>{{ i.title }}</td>
                <td>{{ i.quantity }}</td>
                <td>{{ i.unitPrice * i.quantity | currency:'INR' }}</td>
                <td>{{ i.expectedDelivery | date:'dd MMM yyyy' }}</td>
              </tr>
            }
          </tbody>
        </table>

        <div class="line"><span>Subtotal</span><span>{{ order.subtotal | currency:'INR' }}</span></div>
        <div class="line"><span>Gift points used</span><span>− {{ order.giftPointsUsed }}</span></div>
        <div class="line total"><span>Total paid</span><span>{{ order.total | currency:'INR' }}</span></div>

        <p class="muted">You can cancel this order until {{ order.cancellableUntil | date:'dd MMM yyyy, h:mm a' }} from My Orders.</p>
        <a class="btn" routerLink="/orders">View my orders</a>
        <a class="btn secondary" routerLink="/products">Continue shopping</a>
      </div>
    }
  `
})
export class Confirmation implements OnInit {
  private route = inject(ActivatedRoute);
  private api = inject(ApiService);
  order: Order | null = null;
  error = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('orderId'));
    this.api.order(id).subscribe({ next: o => this.order = o, error: e => this.error = errMsg(e) });
  }
}
