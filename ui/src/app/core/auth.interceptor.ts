import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { LoadingService } from '../services/loading.service';

/** Adds the JWT to every API call, tracks loading, and sends the user to login when the token expires. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const loading = inject(LoadingService);
  const isAuthCall = req.url.includes('/auth/');
  const token = auth.token;
  const request = token && !isAuthCall ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  loading.start();
  return next(request).pipe(
    catchError(err => {
      if (err.status === 401 && !isAuthCall) {
        auth.logout();
        router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => err);
    }),
    finalize(() => loading.stop())
  );
};
