import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { Login } from './pages/login';
import { Register } from './pages/register';
import { Products } from './pages/products';
import { ProductDetail } from './pages/product-detail';
import { CartPage } from './pages/cart';
import { Checkout } from './pages/checkout';
import { PaymentPage } from './pages/payment';
import { Confirmation } from './pages/confirmation';
import { Orders } from './pages/orders';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'products', component: Products },
  { path: 'products/:id', component: ProductDetail },
  { path: 'cart', component: CartPage, canActivate: [authGuard] },
  { path: 'checkout', component: Checkout, canActivate: [authGuard] },
  { path: 'payment/:orderId', component: PaymentPage, canActivate: [authGuard] },
  { path: 'confirmation/:orderId', component: Confirmation, canActivate: [authGuard] },
  { path: 'orders', component: Orders, canActivate: [authGuard] },
  { path: '**', redirectTo: 'products' }
];
