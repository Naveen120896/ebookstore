import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { ProductCard } from '../components/product-card';
import { Brand, Category, Product } from '../models';
import { errMsg } from '../util';

@Component({
  selector: 'app-products',
  imports: [FormsModule, ProductCard],
  template: `
    <h2>Browse books</h2>

    <div class="filters">
      <input type="text" placeholder="Search title or author" [(ngModel)]="q" (keyup.enter)="apply()">
      <select [ngModel]="categoryId" (ngModelChange)="onCategory($event)">
        <option [ngValue]="null">All categories</option>
        @for (c of categories; track c.id) { <option [ngValue]="c.id">{{ c.name }}</option> }
      </select>
      <select [ngModel]="brandId" (ngModelChange)="onBrand($event)">
        <option [ngValue]="null">All brands</option>
        @for (b of brands; track b.id) { <option [ngValue]="b.id">{{ b.name }}</option> }
      </select>
      <button class="btn" (click)="apply()">Search</button>
      <button class="btn secondary" (click)="reset()">Clear</button>
    </div>

    @if (error) { <div class="alert error-box">{{ error }}</div> }
    @if (!loading && !error && products.length === 0) { <p class="muted">No books match your search.</p> }

    @if (loading) {
      <div class="grid" role="status" aria-label="Loading books">
        @for (n of skeletons; track n) {
          <div class="card skeleton-card">
            <div class="skeleton skeleton-img"></div>
            <div class="card-body">
              <div class="skeleton skeleton-line"></div>
              <div class="skeleton skeleton-line short"></div>
              <div class="skeleton skeleton-line short"></div>
            </div>
          </div>
        }
      </div>
    } @else {
      <div class="grid">
        @for (p of products; track p.id) { <app-product-card [product]="p" /> }
      </div>
    }

    @if (total > size) {
      <div class="pager">
        <button class="btn secondary" (click)="go(-1)" [disabled]="page === 0">Previous</button>
        <span>Page {{ page + 1 }} of {{ pages }}</span>
        <button class="btn secondary" (click)="go(1)" [disabled]="page + 1 >= pages">Next</button>
      </div>
    }

    @if (recs.length > 0) {
      <h2 class="section">Recommended for you</h2>
      <p class="muted">Based on books you have bought before.</p>
      <div class="grid">
        @for (p of recs; track p.id) { <app-product-card [product]="p" /> }
      </div>
    }
  `
})
export class Products implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  categories: Category[] = [];
  brands: Brand[] = [];
  products: Product[] = [];
  recs: Product[] = [];
  categoryId: number | null = null;
  brandId: number | null = null;
  q = '';
  page = 0;
  size = 8;
  total = 0;
  loading = false;
  error = '';
  skeletons = [1, 2, 3, 4, 5, 6, 7, 8];

  get pages(): number { return Math.max(1, Math.ceil(this.total / this.size)); }

  ngOnInit(): void {
    this.api.categories().subscribe({ next: c => this.categories = c, error: e => this.error = errMsg(e) });
    this.loadBrands();
    this.search();
    if (this.auth.isLoggedIn()) {
      this.api.recommendations().subscribe({ next: r => this.recs = r, error: () => {} });
    }
  }

  loadBrands(): void {
    this.api.brands(this.categoryId).subscribe({ next: b => this.brands = b, error: () => {} });
  }

  onCategory(id: number | null): void {
    this.categoryId = id;
    this.brandId = null;
    this.loadBrands();
    this.apply();
  }

  onBrand(id: number | null): void {
    this.brandId = id;
    this.apply();
  }

  apply(): void { this.page = 0; this.search(); }

  reset(): void {
    this.q = '';
    this.categoryId = null;
    this.brandId = null;
    this.loadBrands();
    this.apply();
  }

  go(delta: number): void { this.page += delta; this.search(); }

  search(): void {
    this.loading = true;
    this.error = '';
    this.api.products({ categoryId: this.categoryId, brandId: this.brandId, q: this.q, page: this.page, size: this.size })
      .subscribe({
        next: p => { this.products = p.content; this.total = p.totalElements; this.loading = false; },
        error: e => { this.error = errMsg(e); this.loading = false; }
      });
  }
}
