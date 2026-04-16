import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  function setup(token: string | null, refreshToken: string | null = null) {
    localStorage.clear();
    if (token) localStorage.setItem('jwt_token', token);
    if (refreshToken) localStorage.setItem('refresh_token', refreshToken);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        AuthService,
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should add Authorization header when token is present', () => {
    setup('my-jwt-token');
    http.get('/api/events').subscribe();
    const req = httpMock.expectOne('/api/events');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt-token');
    req.flush([]);
  });

  it('should NOT add Authorization header when no token', () => {
    setup(null);
    http.get('/api/events').subscribe();
    const req = httpMock.expectOne('/api/events');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('should refresh token on 401 and retry request once', () => {
    setup('expired-access-token', 'valid-refresh-token');

    let responseBody: unknown;
    http.get('/api/protected').subscribe((res) => {
      responseBody = res;
    });

    const firstReq = httpMock.expectOne('/api/protected');
    expect(firstReq.request.headers.get('Authorization')).toBe('Bearer expired-access-token');
    firstReq.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne('/api/auth/refresh');
    expect(refreshReq.request.headers.has('Authorization')).toBeFalse();
    expect(refreshReq.request.body).toEqual({ refreshToken: 'valid-refresh-token' });
    refreshReq.flush({ token: 'new-access-token', refreshToken: 'new-refresh-token' });

    const retriedReq = httpMock.expectOne('/api/protected');
    expect(retriedReq.request.headers.get('Authorization')).toBe('Bearer new-access-token');
    retriedReq.flush({ ok: true });

    expect(responseBody).toEqual({ ok: true });
  });
});
