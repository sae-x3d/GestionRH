import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, switchMap, tap } from 'rxjs';

export interface AuthSession {
  userId: number;
  username: string;
  email: string;
  roles: string[];
  accountValidationStatus: string;
  departmentCodes: string[];
  photoUrl: string | null;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegistrationData {
  fullName: string;
  username: string;
  email: string;
  password: string;
}

export interface RegistrationResult {
  username: string;
  email: string;
  status: string;
}

const AUTH_API = '/api/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly session = signal<AuthSession | null>(null);
  private readonly initialized = signal(false);
  private restoreRequest: Observable<AuthSession | null> | null = null;
  readonly currentSession = this.session.asReadonly();
  readonly sessionInitialized = this.initialized.asReadonly();

  constructor(private readonly http: HttpClient) { }

  login(credentials: LoginCredentials): Observable<AuthSession> {
    return this.ensureCurrentSession().pipe(
      switchMap(() => this.loadCsrfToken()),
      switchMap(() => this.http.post<AuthSession>(`${AUTH_API}/login`, credentials, { withCredentials: true })),
      tap((session) => {
        this.session.set(session);
        this.initialized.set(true);
      })
    );
  }

  register(data: RegistrationData): Observable<RegistrationResult> {
    return this.loadCsrfToken().pipe(
      switchMap(() => this.http.post<RegistrationResult>(`${AUTH_API}/register`, data, { withCredentials: true }))
    );
  }

  restoreCurrentSession(): void {
    this.ensureCurrentSession().subscribe();
  }

  refreshCurrentSession(): Observable<AuthSession | null> {
    return this.loadCsrfToken().pipe(
      switchMap(() => this.http.get<AuthSession | null>(`${AUTH_API}/me`, { withCredentials: true })),
      tap((session) => { this.session.set(session); this.initialized.set(true); })
    );
  }

  ensureCurrentSession(): Observable<AuthSession | null> {
    if (this.initialized()) {
      return of(this.session());
    }
    if (this.restoreRequest) {
      return this.restoreRequest;
    }

    this.restoreRequest = this.loadCsrfToken().pipe(
      switchMap(() => this.http.get<AuthSession | null>(`${AUTH_API}/me`, { withCredentials: true })),
      catchError(() => of(null)),
      tap((session) => {
        this.session.set(session);
        this.initialized.set(true);
      }),
      finalize(() => this.restoreRequest = null),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    return this.restoreRequest;
  }

  logout(): Observable<void> {
    return this.loadCsrfToken().pipe(
      switchMap(() => this.http.post<void>(`${AUTH_API}/logout`, {}, { withCredentials: true })),
      tap(() => this.session.set(null)),
      catchError(() => {
        this.session.set(null);
        return of(undefined);
      })
    ).pipe(tap(() => this.initialized.set(true)));
  }

  clearLocalSession(): void {
    this.session.set(null);
    this.initialized.set(true);
  }

  private loadCsrfToken(): Observable<void> {
    return this.http.get<{ token: string }>(`${AUTH_API}/csrf`, { withCredentials: true }).pipe(map(() => undefined));
  }
}
