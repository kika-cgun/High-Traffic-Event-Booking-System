import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, map, throwError, finalize, shareReplay } from 'rxjs';
import { AuthRequest, AuthResponse, RegisterRequest } from '../../shared/models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'jwt_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));
  private readonly _refreshToken = signal<string | null>(localStorage.getItem(this.REFRESH_TOKEN_KEY));
  private refreshInFlight$: Observable<string> | null = null;

  readonly token = this._token.asReadonly();
  readonly refreshToken = this._refreshToken.asReadonly();
  readonly isLoggedIn = computed(() => this._token() !== null);

  login(request: AuthRequest): Observable<void> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap((res) => this.storeTokens(res.token, res.refreshToken)),
      map(() => void 0)
    );
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<AuthResponse>('/api/auth/register', request).pipe(
      tap((res) => this.storeTokens(res.token, res.refreshToken)),
      map(() => void 0)
    );
  }

  refreshAccessToken(): Observable<string> {
    const refreshToken = this._refreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('Missing refresh token'));
    }

    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    this.refreshInFlight$ = this.http
      .post<AuthResponse>('/api/auth/refresh', { refreshToken })
      .pipe(
        tap((res) => this.storeTokens(res.token, res.refreshToken)),
        map((res) => res.token),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay(1)
      );

    return this.refreshInFlight$;
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    this._token.set(null);
    this._refreshToken.set(null);
    this.router.navigate(['/login']);
  }

  private storeTokens(token: string, refreshToken: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    this._token.set(token);
    this._refreshToken.set(refreshToken);
  }
}
