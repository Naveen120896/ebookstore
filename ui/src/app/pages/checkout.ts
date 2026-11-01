import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { Address, Cart } from '../models';
import { errMsg } from '../util';
import { Loading } from '../components/loading';

@Component({
  selector: 'app-checkout',
  imports: [FormsModule, RouterLink, CurrencyPipe, Loading],
  template: `
    <h2>Checkout</h2>
    @if (error) { <div class="alert error-box">{{ error }}</div> }
    @if (loading) { <app-loading message="Preparing your checkout..." /> }

    @if (!loading && cart && cart.items.length === 0) {
      <div class="panel"><p>Your cart is empty.</p><a class="btn" routerLink="/products">Browse books</a></div>
    }

    @if (!loading && cart && cart.items.length > 0) {
      <div class="two-col">
        <div>
          <div class="panel">
            <h3>1. Delivery address</h3>
            @for (a of addresses; track a.id) {
              <label class="radio">
                <input type="radio" name="addr" [value]="a.id" [(ngModel)]="selectedId">
                <span>{{ a.line1 }}@if (a.line2) {, {{ a.line2 }} }, {{ a.city }}@if (a.state) {, {{ a.state }} } {{ a.postalCode }}, {{ a.country }}</span>
              </label>
            }
            @if (addresses.length === 0 && !showForm) { <p class="muted">No saved addresses yet.</p> }
            <button class="btn-link" (click)="showForm = !showForm">{{ showForm ? 'Cancel' : '+ Add new address' }}</button>

            @if (showForm) {
              <div class="form-grid">
                <input placeholder="Address line 1 *" [(ngModel)]="newAddr.line1" name="l1">
                <input placeholder="Address line 2" [(ngModel)]="newAddr.line2" name="l2">
                <input placeholder="City *" [(ngModel)]="newAddr.city" name="city">
                <input placeholder="State" [(ngModel)]="newAddr.state" name="state">
                <input placeholder="Postal code *" [(ngModel)]="newAddr.postalCode" name="pin">
                <input placeholder="Country *" [(ngModel)]="newAddr.country" name="country">
                <button class="btn" (click)="saveAddress()" [disabled]="busy">{{ busy ? 'Saving...' : 'Save address' }}</button>
              </div>
            }
          </div>

          <div class="panel">
            <h3>2. Gift points</h3>
            <p class="muted">You have {{ auth.currentUser?.giftPoints ?? 0 }} points (1 point = ₹1). You can use up to {{ maxPoints }} on this order.</p>
            <input type="number" min="0" [max]="maxPoints" [(ngModel)]="points" name="points" class="qty">
            <button class="btn-link" (click)="points = maxPoints">Use maximum</button>
          </div>
        </div>

        <div class="panel summary-box">
          <h3>Order summary</h3>
          @for (item of cart.items; track item.product.id) {
            <div class="line"><span>{{ item.product.title }} × {{ item.quantity }}</span><span>{{ item.lineTotal | currency:'INR' }}</span></div>
          }
          <hr>
          <div class="line"><span>Subtotal</span><span>{{ cart.subtotal | currency:'INR' }}</span></div>
          <div class="line"><span>Gift points</span><span>− {{ points || 0 }}</span></div>
          <div class="line total"><span>Total to pay</span><span>{{ total | currency:'INR' }}</span></div>
          <button class="btn full" (click)="placeOrder()" [disabled]="busy || !selectedId">{{ busy ? 'Placing your order...' : 'Place order &amp; continue to payment' }}</button>
          @if (!selectedId) { <p class="muted small">Select or add a delivery address to continue.</p> }
        </div>
      </div>
    }
  `
})
export class Checkout implements OnInit {
  private api = inject(ApiService);
  private cartSvc = inject(CartService);
  private router = inject(Router);
  auth = inject(AuthService);

  cart: Cart | null = null;
  addresses: Address[] = [];
  selectedId: number | null = null;
  points = 0;
  showForm = false;
  busy = false;
  cartLoaded = false;
  addrLoaded = false;
  error = '';
  newAddr: Address = { line1: '', line2: '', city: '', state: '', postalCode: '', country: 'India' };

  get maxPoints(): number {
    const pts = this.auth.currentUser?.giftPoints ?? 0;
    const sub = Math.floor(this.cart?.subtotal ?? 0);
    return Math.max(0, Math.min(pts, sub));
  }

  get loading(): boolean { return !this.cartLoaded || !this.addrLoaded; }

  get total(): number { return (this.cart?.subtotal ?? 0) - (Number(this.points) || 0); }

  ngOnInit(): void {
    this.auth.refreshUser();
    this.api.getCart().subscribe({
      next: c => { this.cart = c; this.cartLoaded = true; },
      error: e => { this.error = errMsg(e); this.cartLoaded = true; }
    });
    this.api.addresses().subscribe({
      next: list => {
        this.addresses = list;
        this.selectedId = list.find(a => a.isDefault)?.id ?? list[0]?.id ?? null;
        this.showForm = list.length === 0;
        this.addrLoaded = true;
      },
      error: e => { this.error = errMsg(e); this.addrLoaded = true; }
    });
  }

  saveAddress(): void {
    const a = this.newAddr;
    if (!a.line1.trim() || !a.city.trim() || !a.postalCode.trim() || !a.country.trim()) {
      this.error = 'Address line 1, city, postal code and country are required.';
      return;
    }
    this.busy = true;
    this.error = '';
    this.api.addAddress(a).subscribe({
      next: saved => {
        this.addresses = [...this.addresses, saved];
        this.selectedId = saved.id ?? null;
        this.showForm = false;
        this.busy = false;
        this.newAddr = { line1: '', line2: '', city: '', state: '', postalCode: '', country: 'India' };
      },
      error: e => { this.busy = false; this.error = errMsg(e); }
    });
  }

  placeOrder(): void {
    if (!this.selectedId) { return; }
    const pts = Math.floor(Number(this.points) || 0);
    if (pts < 0 || pts > this.maxPoints) {
      this.error = `Gift points must be between 0 and ${this.maxPoints}.`;
      return;
    }
    this.busy = true;
    this.error = '';
    this.api.checkout(this.selectedId, pts).subscribe({
      next: order => {
        this.cartSvc.refresh();
        this.auth.refreshUser();
        this.router.navigate(['/payment', order.id]);
      },
      error: e => { this.busy = false; this.error = errMsg(e); }
    });
  }
}
