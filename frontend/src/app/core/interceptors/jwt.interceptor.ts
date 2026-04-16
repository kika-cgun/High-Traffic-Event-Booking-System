import { HttpInterceptorFn, HttpErrorResponse, HttpContextToken } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const RETRY_ONCE = new HttpContextToken<boolean>(() => false);

function isAuthEndpoint(url: string): boolean {
  return url.includes('/api/auth/login') || url.includes('/api/auth/register') || url.includes('/api/auth/refresh');
}

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token();
  const shouldAttachToken = token && !isAuthEndpoint(req.url);

  const authReq = shouldAttachToken
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(catchError((err: unknown) => {
    const canTryRefresh =
      err instanceof HttpErrorResponse &&
      err.status === 401 &&
      !isAuthEndpoint(req.url) &&
      !req.context.get(RETRY_ONCE);

    if (canTryRefresh) {
      return authService.refreshAccessToken().pipe(
        switchMap((newAccessToken) => {
          const retryReq = req.clone({
            setHeaders: { Authorization: `Bearer ${newAccessToken}` },
            context: req.context.set(RETRY_ONCE, true),
          });
          return next(retryReq);
        }),
        catchError((refreshError) => {
          authService.logout();
          return throwError(() => refreshError);
        })
      );
    }

    if (err instanceof HttpErrorResponse && err.status === 401) {
      authService.logout();
    }

    return throwError(() => err);
  }));
};
