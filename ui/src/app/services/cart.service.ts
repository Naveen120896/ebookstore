import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, tap } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { Cart } from '../models';

/** Keeps the cart badge in the header in sync. */
@Injectable({ providedIn: 'root' })
export class CartService {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private count = new BehaviorSubject<number>(0);
  count$ = this.count.asObservable();

  refresh(): void {
    if (!this.auth.isLoggedIn()) { this.count.next(0); return; }
    this.api.getCart().subscribe({ next: c => this.setFromCart(c), error: () => {} });
  }

  setFromCart(c: Cart): void {
    this.count.next(c.items.reduce((sum, i) => sum + i.quantity, 0));
  }

  addItem(productId: number, quantity: number) {
    return this.api.addToCart(productId, quantity).pipe(tap(c => this.setFromCart(c)));
  }
}
