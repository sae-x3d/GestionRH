import { HttpInterceptorFn } from '@angular/common/http';

const API_PREFIX = '/api/';
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export const xsrfInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(API_PREFIX)) {
    return next(request);
  }

  const token = readCookie('XSRF-TOKEN');
  const headers = token && UNSAFE_METHODS.has(request.method)
    ? request.headers.set('X-XSRF-TOKEN', token)
    : request.headers;

  return next(request.clone({ headers, withCredentials: true }));
};

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') {
    return null;
  }

  const prefix = `${name}=`;
  const cookie = document.cookie.split(';').map((part) => part.trim()).find((part) => part.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null;
}
