import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CandidateOption { code: string; libelle: string; }
export interface CandidateApplicationOptions { typesParcours: CandidateOption[]; niveauxEtude: CandidateOption[]; }
export interface CandidateApplicationResult { id: number; statut: string; envoyeeAt: string; }
export interface ReceivedCandidature {
  id: number;
  offreId: number;
  candidat: string;
  email: string;
  photoUrl: string | null;
  titreOffre: string;
  poste: string;
  departement: string;
  statutCode: string;
  statut: string;
  envoyeeAt: string;
}

export interface RhOfferCandidates {
  id: number; titre: string; poste: string; departement: string;
  effectif: number; effectifRecrute: number; effectifRestant: number;
  nombreCandidatures: number; statutCode: string; statut: string; dateCloture: string;
}

@Injectable({ providedIn: 'root' })
export class CandidateApplicationService {
  constructor(private readonly http: HttpClient) { }

  options(): Observable<CandidateApplicationOptions> {
    return this.http.get<CandidateApplicationOptions>('/api/candidatures/options');
  }

  submit(form: FormData): Observable<CandidateApplicationResult> {
    return this.http.post<CandidateApplicationResult>('/api/candidatures', form);
  }

  receivedByRh(): Observable<ReceivedCandidature[]> {
    return this.http.get<ReceivedCandidature[]>('/api/rh/candidatures');
  }

  rhOffers(): Observable<RhOfferCandidates[]> {
    return this.http.get<RhOfferCandidates[]>('/api/rh/candidatures/offres');
  }

  rhDetails(id: number): Observable<Record<string, any>> {
    return this.http.get<Record<string, any>>(`/api/rh/candidatures/${id}`);
  }

  rhAction(id: number, action: Record<string, unknown>): Observable<{ id: number; statut: string }> {
    return this.http.post<{ id: number; statut: string }>(`/api/rh/candidatures/${id}/actions`, action);
  }

  myApplications(): Observable<Record<string, any>[]> {
    return this.http.get<Record<string, any>[]>('/api/candidatures/mes');
  }

  myApplicationDetails(id: number): Observable<Record<string, any>> {
    return this.http.get<Record<string, any>>(`/api/candidatures/${id}/suivi`);
  }

  candidateResponse(id: number, response: Record<string, unknown>): Observable<{ id: number; statut: string }> {
    return this.http.post<{ id: number; statut: string }>(`/api/candidatures/${id}/reponse`, response);
  }

  employeeContract(): Observable<Record<string, any>> {
    return this.http.get<Record<string, any>>('/api/employe/contrat');
  }

  downloadCv(id: number): Observable<Blob> {
    return this.http.get(`/api/rh/candidatures/${id}/cv`, { responseType: 'blob' });
  }
}
