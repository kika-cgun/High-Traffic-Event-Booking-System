import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

describe('authGuard', () => {
  function setup(loggedIn: boolean) {
    localStorage.clear();
    if (loggedIn) localStorage.setItem('jwt_token', 'token');
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        AuthService,
      ],
    });
  }

  afterEach(() => localStorage.clear());

  it('should return true when user is logged in', () => {
    setup(true);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/tickets' } as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });

  it('should redirect to /login with returnUrl when not logged in', () => {
    setup(false);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/tickets' } as RouterStateSnapshot)
    ) as any;
    expect(result.toString()).toContain('/login');
  });
});
