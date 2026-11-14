import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  fullName: string | null;
  phoneNumber: string | null;
  region: string | null;
  photoUrl: string | null;
  status: string;
  statusLabel: string;
  verificationRequestedAt: string | null;
  editable: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  private readonly http = inject(HttpClient);
  profile(): Observable<UserProfile> { return this.http.get<UserProfile>('/api/profil/me'); }
  update(data: FormData): Observable<UserProfile> { return this.http.post<UserProfile>('/api/profil/me', data); }
  requestVerification(): Observable<UserProfile> { return this.http.post<UserProfile>('/api/profil/me/demande-verification', {}); }
}
