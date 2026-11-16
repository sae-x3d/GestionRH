import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Competence {
  id: number;
  code: string;
  libelle: string;
  description: string | null;
  actif: boolean;
  criteresUtilises: number;
  createdAt: string;
  updatedAt: string;
}

export interface PosteOption {
  id: number;
  poste: string;
  codeMatricule: string;
  departement: string;
  niveauMin: number;
  niveauMax: number;
  actif: boolean;
}

export interface CriterePoste {
  id: number;
  competenceId: number;
  competenceCode: string;
  competence: string;
  niveauAttendu: number;
  seuilEcart: number;
  obligatoire: boolean;
  commentaire: string | null;
}

export interface CompetenceConfiguration {
  postes: PosteOption[];
  poste: PosteOption | null;
  criteres: CriterePoste[];
  competences: Competence[];
}

@Injectable({ providedIn: 'root' })
export class CompetenceManagementService {
  constructor(private readonly http: HttpClient) { }

  list(): Observable<Competence[]> {
    return this.http.get<Competence[]>('/api/rh/competences');
  }

  configuration(posteId?: number): Observable<CompetenceConfiguration> {
    const params = posteId ? new HttpParams().set('posteId', posteId) : undefined;
    return this.http.get<CompetenceConfiguration>('/api/rh/criteres-poste', { params });
  }

  create(data: { code: string; libelle: string; description: string }): Observable<{ id: number; message: string }> {
    return this.http.post<{ id: number; message: string }>('/api/rh/competences', data);
  }

  setActive(id: number, actif: boolean): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`/api/rh/competences/${id}/statut`, { actif });
  }

  saveCriterion(data: { posteId: number; competenceId: number; niveauAttendu: number; seuilEcart: number; obligatoire: boolean; commentaire: string }): Observable<CompetenceConfiguration> {
    return this.http.post<CompetenceConfiguration>('/api/rh/criteres-poste', data);
  }

  deleteCriterion(posteId: number, critereId: number): Observable<CompetenceConfiguration> {
    const params = new HttpParams().set('posteId', posteId);
    return this.http.delete<CompetenceConfiguration>(`/api/rh/criteres-poste/${critereId}`, { params });
  }

  updateBareme(posteId: number, niveauMin: number, niveauMax: number): Observable<CompetenceConfiguration> {
    return this.http.put<CompetenceConfiguration>(`/api/rh/postes/${posteId}/bareme`, { niveauMin, niveauMax });
  }
}
