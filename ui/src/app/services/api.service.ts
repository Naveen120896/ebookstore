import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API } from '../api.config';
import { Address, Brand, Cart, Category, Order, Payment, PaymentRequest, Product, ProductPage } from '../models';

export interface ProductFilter {
  categoryId?: number | null; brandId?: number | null; q?: string; page?: number; size?: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  // ---- catalogue ----
  categories() { return this.http.get<Category[]>(`${API}/categories`); }

  brands(categoryId: number | null) {
    let params = new HttpParams();
    if (categoryId) { params = params.set('categoryId', categoryId); }
    return this.http.get<Brand[]>(`${API}/brands`, { params });
  }

  products(f: ProductFilter) {
    let params = new HttpParams();
    if (f.categoryId) { params = params.set('categoryId', f.categoryId); }
    if (f.brandId) { params = params.set('brandId', f.brandId); }
    if (f.q && f.q.trim()) { params = params.set('q', f.q.trim()); }
    params = params.set('page', f.page ?? 0).set('size', f.size ?? 8);
    return this.http.get<ProductPage>(`${API}/products`, { params });
  }

  product(id: number) { return this.http.get<Product>(`${API}/products/${id}`); }
  related(id: number) { return this.http.get<Product[]>(`${API}/products/${id}/related`); }
  recommendations() { return this.http.get<Product[]>(`${API}/products/recommendations`); }

  // ---- cart ----
  getCart() { return this.http.get<Cart>(`${API}/cart`); }
  addToCart(productId: number, quantity: number) {
    return this.http.post<Cart>(`${API}/cart/items`, { productId, quantity });
  }
  updateCart(productId: number, quantity: number) {
    return this.http.put<Cart>(`${API}/cart/items/${productId}`, { productId, quantity });
  }
  removeFromCart(productId: number) { return this.http.delete<Cart>(`${API}/cart/items/${productId}`); }

  // ---- addresses ----
  addresses() { return this.http.get<Address[]>(`${API}/addresses`); }
  addAddress(a: Address) { return this.http.post<Address>(`${API}/addresses`, a); }

  // ---- orders & payments ----
  checkout(addressId: number, giftPointsToRedeem: number) {
    return this.http.post<Order>(`${API}/orders`, { addressId, giftPointsToRedeem });
  }
  orders() { return this.http.get<Order[]>(`${API}/orders`); }
  order(id: number) { return this.http.get<Order>(`${API}/orders/${id}`); }
  cancelOrder(id: number) { return this.http.post<Order>(`${API}/orders/${id}/cancel`, {}); }
  pay(orderId: number, body: PaymentRequest) {
    return this.http.post<Payment>(`${API}/orders/${orderId}/payment`, body);
  }
}
