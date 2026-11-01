import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, tap } from 'rxjs';
import { API } from '../api.config';
import { AuthResponse, User } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private userSubject = new BehaviorSubject<User | null>(this.readUser());
  user$ = this.userSubject.asObservable();

  get token(): string | null { return localStorage.getItem('token'); }
  get currentUser(): User | null { return this.userSubject.value; }
  isLoggedIn(): boolean { return !!this.token; }

  register(fullName: string, email: string, password: string) {
    return this.http.post<User>(`${API}/auth/register`, { fullName, email, password });
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${API}/auth/login`, { email, password }).pipe(
      tap(res => { localStorage.setItem('token', res.token); this.setUser(res.user); })
    );
  }

  /** Re-reads the profile so the gift point balance in the header stays current. */
  refreshUser(): void {
    if (!this.isLoggedIn()) { return; }
    this.http.get<User>(`${API}/users/me`).subscribe({ next: u => this.setUser(u), error: () => {} });
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.userSubject.next(null);
  }

  private setUser(u: User): void {
    localStorage.setItem('user', JSON.stringify(u));
    this.userSubject.next(u);
  }

  private readUser(): User | null {
    try { return JSON.parse(localStorage.getItem('user') ?? 'null'); } catch { return null; }
  }
}
