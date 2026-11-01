import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { Order, PaymentRequest } from '../models';
import { errMsg } from '../util';
import { Loading } from '../components/loading';

@Component({
  selector: 'app-payment',
  imports: [FormsModule, RouterLink, CurrencyPipe, Loading],
  template: `
    <h2>Payment</h2>
    @if (error) { <div class="alert error-box">{{ error }}</div> }
    @if (!order && !error) { <app-loading message="Loading your order..." /> }

    @if (order && order.status !== 'PLACED') {
      <div class="panel">
        <p>This order is already <strong>{{ order.status }}</strong>.</p>
        <a class="btn" routerLink="/orders">Go to my orders</a>
      </div>
    }

    @if (order && order.status === 'PLACED') {
      <div class="panel narrow">
        <p>Order #{{ order.id }} · Amount due: <strong>{{ order.total | currency:'INR' }}</strong></p>

        @if (order.total === 0) {
          <p class="muted">Your gift points cover the full amount. No card needed.</p>
          <button class="btn full" (click)="pay()" [disabled]="busy">{{ busy ? 'Processing...' : 'Confirm purchase' }}</button>
        } @else {
          <label>Payment method
            <select [(ngModel)]="method" name="method">
              <option value="CREDIT_CARD">Credit card</option>
              <option value="DEBIT_CARD">Debit card</option>
            </select>
          </label>
          <label>Card number <input type="text" inputmode="numeric" placeholder="4111 1111 1111 1111" [(ngModel)]="card.cardNumber" name="cn"></label>
          <label>Name on card <input type="text" [(ngModel)]="card.cardHolder" name="ch"></label>
          <div class="form-grid two">
            <label>Expiry <input type="text" placeholder="MM/YY" [(ngModel)]="card.expiry" name="ex"></label>
            <label>CVV <input type="password" inputmode="numeric" maxlength="4" [(ngModel)]="card.cvv" name="cv"></label>
          </div>
          <button class="btn full" (click)="pay()" [disabled]="busy">{{ busy ? 'Processing...' : 'Pay ' + (order.total | currency:'INR') }}</button>
          <p class="muted small">Demo gateway: any valid-looking card works. A card number ending in 0000 is declined.</p>
        }
      </div>
    }
  `
})
export class PaymentPage implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private auth = inject(AuthService);

  order: Order | null = null;
  method = 'CREDIT_CARD';
  card = { cardNumber: '', cardHolder: '', expiry: '', cvv: '' };
  busy = false;
  error = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('orderId'));
    this.api.order(id).subscribe({ next: o => this.order = o, error: e => this.error = errMsg(e) });
  }

  pay(): void {
    if (!this.order) { return; }
    let body: PaymentRequest;
    if (this.order.total === 0) {
      body = { method: 'GIFT_POINTS' };
    } else {
      const digits = this.card.cardNumber.replace(/[\s-]/g, '');
      if (!/^\d{12,19}$/.test(digits)) { this.error = 'Enter a valid card number.'; return; }
      if (!/^\d{3,4}$/.test(this.card.cvv)) { this.error = 'Enter a valid CVV.'; return; }
      body = { method: this.method, ...this.card, cardNumber: digits };
    }
    this.busy = true;
    this.error = '';
    const orderId = this.order.id;
    this.api.pay(orderId, body).subscribe({
      next: () => { this.auth.refreshUser(); this.router.navigate(['/confirmation', orderId]); },
      error: e => { this.busy = false; this.error = errMsg(e); }
    });
  }
}
