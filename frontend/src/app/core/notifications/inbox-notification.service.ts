import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface InboxNotification {
  id: number;
  type: string;
  titre: string;
  resume: string;
  lien: string;
  lue: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class InboxNotificationService {
  private readonly baseUrl = '/api/notifications';
  private readonly unreadState = signal(0);
  readonly unreadCount = this.unreadState.asReadonly();
  constructor(private readonly http: HttpClient) { }

  list(): Observable<InboxNotification[]> { return this.http.get<InboxNotification[]>(this.baseUrl); }
  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/non-lues/count`).pipe(tap((count) => this.unreadState.set(count)));
  }
  markRead(id: number): Observable<InboxNotification> { return this.http.post<InboxNotification>(`${this.baseUrl}/${id}/lire`, {}); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/${id}`); }
  setUnreadCount(count: number): void { this.unreadState.set(Math.max(0, count)); }
  decrementUnreadCount(): void { this.unreadState.update((count) => Math.max(0, count - 1)); }
}
