import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { errMsg } from '../util';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="panel narrow">
      <h2>Login</h2>
      @if (error) { <div class="alert error-box">{{ error }}</div> }
      <label>Email <input type="email" [(ngModel)]="email" name="email" (keyup.enter)="submit()"></label>
      <label>Password <input type="password" [(ngModel)]="password" name="password" (keyup.enter)="submit()"></label>
      <button class="btn full" (click)="submit()" [disabled]="busy">{{ busy ? 'Signing in...' : 'Login' }}</button>
      <p class="muted">New here? <a routerLink="/register">Create an account</a></p>
    </div>
  `
})
export class Login {
  private auth = inject(AuthService);
  private cart = inject(CartService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  email = '';
  password = '';
  busy = false;
  error = '';

  submit(): void {
    if (!this.email || !this.password) { this.error = 'Enter your email and password.'; return; }
    this.busy = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.cart.refresh();
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/products';
        this.router.navigateByUrl(returnUrl);
      },
      error: e => { this.busy = false; this.error = errMsg(e); }
    });
  }
}
