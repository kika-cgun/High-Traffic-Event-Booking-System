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
    req.flush({ token: 'test-jwt-token' });

    expect(localStorage.getItem('jwt_token')).toBe('test-jwt-token');
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.token()).toBe('test-jwt-token');
  });

  it('should store token and set isLoggedIn after register', () => {
    service.register({ username: 'u', email: 'u@x.com', password: 'password1' }).subscribe();
    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'register-token' });

    expect(localStorage.getItem('jwt_token')).toBe('register-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('should clear token and set isLoggedIn to false after logout', () => {
    localStorage.setItem('jwt_token', 'existing-token');
    TestBed.resetTestingModule();
    localStorage.setItem('jwt_token', 'existing-token');
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    service.logout();
    expect(localStorage.getItem('jwt_token')).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('should initialise isLoggedIn from localStorage on boot', () => {
    TestBed.resetTestingModule();
    localStorage.setItem('jwt_token', 'boot-token');
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const bootService = TestBed.inject(AuthService);
    expect(bootService.isLoggedIn()).toBeTrue();
    expect(bootService.token()).toBe('boot-token');
  });
});
