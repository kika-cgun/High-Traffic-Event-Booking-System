import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, map } from 'rxjs';
import { AuthRequest, AuthResponse, RegisterRequest } from '../../shared/models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'jwt_token';
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));

  readonly token = this._token.asReadonly();
  readonly isLoggedIn = computed(() => this._token() !== null);

  login(request: AuthRequest): Observable<void> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap(res => this.storeToken(res.token)),
      map(() => void 0)
    );
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<AuthResponse>('/api/auth/register', request).pipe(
      tap(res => this.storeToken(res.token)),
      map(() => void 0)
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this._token.set(null);
    this.router.navigate(['/login']);
  }

  private storeToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this._token.set(token);
  }
}
