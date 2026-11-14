import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface PlanningEmployee { employe_id: number; matricule: string; nom: string; poste: string; departement: string; }
export interface PlanningDay {
  id: number; employeId: number; date: string; type_code: string; type_jour: string;
  debut: string | null; fin: string | null; pauseDebut: string | null; pauseFin: string | null;
  observation: string | null; pointe?: number;
}
export interface PlanningWeek {
  departement?: { id: number; code: string; nom: string };
  semaineDebut: string; semaineFin: string; planning?: { id: number; statut_code: string; statut: string; version: number; publieAt: string | null } | null;
  employes?: PlanningEmployee[]; jours?: PlanningDay[];
  compteurs?: Array<{ date: string; effectif_actif: number; planifies: number; repos: number; absents: number; arrives: number; nonPlanifies: number }>;
  horairesParDefaut?: { heureDebut: string; heureFin: string; pauseDebut: string; pauseFin: string };
  joursOuvrables?: Array<{ numero_iso: number }>;
  typesJour?: Array<{ id: number; code: string; libelle: string }>;
  departements?: PlanningWeek[]; modifiable?: boolean;
}
export interface PlanningAssignment {
  employeIds: number[]; dates: string[]; typeJourId: number;
  heureDebut: string | null; heureFin: string | null; pauseDebut: string | null; pauseFin: string | null;
  observation: string | null;
}

@Injectable({ providedIn: 'root' })
export class TimeManagementService {
  private readonly http = inject(HttpClient);

  myPlanning(week: string): Observable<PlanningWeek> {
    return this.http.get<PlanningWeek>('/api/planning/mon', { params: new HttpParams().set('semaine', week) });
  }
  managerPlanning(week: string): Observable<PlanningWeek> {
    return this.http.get<PlanningWeek>('/api/manager/planning/equipe', { params: new HttpParams().set('semaine', week) });
  }
  globalPlanning(week: string, area: 'rh' | 'administration'): Observable<PlanningWeek> {
    return this.http.get<PlanningWeek>(`/api/${area}/planning/equipe`, { params: new HttpParams().set('semaine', week) });
  }
  teamPresence(date: string, area: 'manager' | 'rh' | 'administration'): Observable<any> {
    const endpoint = area === 'manager' ? '/api/manager/presence/equipe' : `/api/${area}/presence/equipe`;
    return this.http.get(endpoint, { params: new HttpParams().set('date', date) });
  }
  assign(request: PlanningAssignment): Observable<PlanningWeek> {
    return this.http.post<PlanningWeek>('/api/manager/planning/affectations', request);
  }
  publish(id: number): Observable<PlanningWeek> { return this.http.post<PlanningWeek>(`/api/manager/planning/${id}/publier`, {}); }
  reopen(id: number): Observable<PlanningWeek> { return this.http.post<PlanningWeek>(`/api/manager/planning/${id}/rouvrir`, {}); }
  punchDay(): Observable<any> { return this.http.get('/api/planning/pointage'); }
  punch(action: string, idempotencyKey: string): Observable<any> {
    return this.http.post('/api/planning/pointage', { action, idempotencyKey });
  }
}
