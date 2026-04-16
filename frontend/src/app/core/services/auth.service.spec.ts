import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should store token and set isLoggedIn after login', () => {
    service.login({ username: 'user1', password: 'password1' }).subscribe();
    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'test-jwt-token', refreshToken: 'test-refresh-token' });

    expect(localStorage.getItem('jwt_token')).toBe('test-jwt-token');
    expect(localStorage.getItem('refresh_token')).toBe('test-refresh-token');
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.token()).toBe('test-jwt-token');
    expect(service.refreshToken()).toBe('test-refresh-token');
  });

  it('should store token and set isLoggedIn after register', () => {
    service.register({ username: 'u', email: 'u@x.com', password: 'password1' }).subscribe();
    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'register-token', refreshToken: 'register-refresh-token' });

    expect(localStorage.getItem('jwt_token')).toBe('register-token');
    expect(localStorage.getItem('refresh_token')).toBe('register-refresh-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('should clear token and set isLoggedIn to false after logout', () => {
    localStorage.setItem('jwt_token', 'existing-token');
    localStorage.setItem('refresh_token', 'existing-refresh-token');
    TestBed.resetTestingModule();
    localStorage.setItem('jwt_token', 'existing-token');
    localStorage.setItem('refresh_token', 'existing-refresh-token');
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    service.logout();
    expect(localStorage.getItem('jwt_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('should initialise isLoggedIn from localStorage on boot', () => {
    TestBed.resetTestingModule();
    localStorage.setItem('jwt_token', 'boot-token');
    localStorage.setItem('refresh_token', 'boot-refresh-token');
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const bootService = TestBed.inject(AuthService);
    expect(bootService.isLoggedIn()).toBeTrue();
    expect(bootService.token()).toBe('boot-token');
    expect(bootService.refreshToken()).toBe('boot-refresh-token');
  });

  it('should refresh access token and update tokens in storage', () => {
    localStorage.setItem('refresh_token', 'old-refresh-token');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    let refreshedAccessToken = '';
    service.refreshAccessToken().subscribe((token) => {
      refreshedAccessToken = token;
    });

    const req = httpMock.expectOne('/api/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'old-refresh-token' });
    req.flush({ token: 'new-access-token', refreshToken: 'new-refresh-token' });

    expect(refreshedAccessToken).toBe('new-access-token');
    expect(localStorage.getItem('jwt_token')).toBe('new-access-token');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh-token');
  });
});
