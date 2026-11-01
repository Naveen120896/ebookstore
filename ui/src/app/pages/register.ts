import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { errMsg } from '../util';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="panel narrow">
      <h2>Create account</h2>
      <p class="muted">New members get 100 gift points to spend on their first order.</p>
      @if (error) { <div class="alert error-box">{{ error }}</div> }
      <label>Full name <input type="text" [(ngModel)]="fullName" name="fullName"></label>
      <label>Email <input type="email" [(ngModel)]="email" name="email"></label>
      <label>Password (min 8 characters) <input type="password" [(ngModel)]="password" name="password" (keyup.enter)="submit()"></label>
      <button class="btn full" (click)="submit()" [disabled]="busy">{{ busy ? 'Creating...' : 'Register' }}</button>
      <p class="muted">Already registered? <a routerLink="/login">Login</a></p>
    </div>
  `
})
export class Register {
  private auth = inject(AuthService);
  private cart = inject(CartService);
  private router = inject(Router);
  fullName = '';
  email = '';
  password = '';
  busy = false;
  error = '';

  submit(): void {
    if (!this.fullName.trim() || !this.email.trim() || this.password.length < 8) {
      this.error = 'Fill in all fields. Password must be at least 8 characters.';
      return;
    }
    this.busy = true;
    this.error = '';
    this.auth.register(this.fullName.trim(), this.email.trim(), this.password).subscribe({
      next: () => {
        // sign the new user in straight away
        this.auth.login(this.email.trim(), this.password).subscribe({
          next: () => { this.cart.refresh(); this.router.navigate(['/products']); },
          error: e => { this.busy = false; this.error = errMsg(e); }
        });
      },
      error: e => { this.busy = false; this.error = errMsg(e); }
    });
  }
}
