import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CriterePoste } from './competence-management.service';

export interface EvaluationEmployee {
  employeId: number;
  matricule: string;
  employe: string;
  username: string;
  poste: string;
  niveauMin: number;
  niveauMax: number;
  derniereEvaluation: string | null;
  nbCriteres: number;
}

export interface EvaluationRow {
  id: number;
  employeId: number;
  employe: string;
  matricule?: string;
  poste: string;
  departement?: string;
  dateEvaluation: string;
  statut: 'SOUMISE' | 'VALIDEE';
  soumiseAt: string;
  valideeAt: string | null;
  nbCriteres: number;
  nbEcarts: number;
  nbBesoins?: number;
}

export interface EvaluationForm {
  employe: EvaluationEmployee & { posteId: number; departementId: number; dateEmbauche: string; statutCode: string };
  criteres: CriterePoste[];
  dateEvaluation: string;
}

export interface EvaluationLine {
  id: number;
  competenceId: number;
  competenceCode: string;
  competence: string;
  niveauAttendu: number;
  niveauAcquis: number;
  seuilEcart: number;
  ecart: number;
  obligatoire: boolean;
  commentaire: string | null;
}

export interface TrainingNeed {
  id: number;
  competenceId: number;
  competence: string;
  statut: 'OUVERT' | 'PLANIFIE' | 'CLOTURE' | 'ANNULE';
  niveauAttendu: number;
  niveauAcquisInitial: number;
  seuilEcart: number;
  ecartInitial: number;
  createdAt: string;
}

export interface EvaluationDetail {
  evaluation: {
    id: number; employeId: number; posteId: number; employe: string; username: string;
    poste: string; departement: string; dateEvaluation: string; statut: 'SOUMISE' | 'VALIDEE';
    commentaire: string | null; baremeMin: number; baremeMax: number; soumiseAt: string;
    valideeAt: string | null; evaluateur: string; valideur: string | null;
  };
  lignes: EvaluationLine[];
  besoins: TrainingNeed[];
  message?: string;
  besoinsCrees?: number;
}

export interface ManagerEvaluationOverview {
  employes: EvaluationEmployee[];
  evaluations: EvaluationRow[];
}

@Injectable({ providedIn: 'root' })
export class EvaluationService {
  constructor(private readonly http: HttpClient) { }

  managerOverview(): Observable<ManagerEvaluationOverview> {
    return this.http.get<ManagerEvaluationOverview>('/api/manager/evaluations');
  }

  managerForm(employeId: number): Observable<EvaluationForm> {
    return this.http.get<EvaluationForm>('/api/manager/evaluations/formulaire', { params: { employeId } });
  }

  create(data: { employeId: number; dateEvaluation: string; commentaire: string; notes: Record<number, number> }): Observable<EvaluationDetail> {
    return this.http.post<EvaluationDetail>('/api/manager/evaluations', data);
  }

  managerDetail(id: number): Observable<EvaluationDetail> {
    return this.http.get<EvaluationDetail>(`/api/manager/evaluations/${id}`);
  }

  rhList(): Observable<EvaluationRow[]> {
    return this.http.get<EvaluationRow[]>('/api/rh/evaluations');
  }

  rhDetail(id: number): Observable<EvaluationDetail> {
    return this.http.get<EvaluationDetail>(`/api/rh/evaluations/${id}`);
  }

  validate(id: number): Observable<EvaluationDetail> {
    return this.http.post<EvaluationDetail>(`/api/rh/evaluations/${id}/valider`, {});
  }

  employeeList(): Observable<EvaluationRow[]> {
    return this.http.get<EvaluationRow[]>('/api/employe/evaluations');
  }

  employeeDetail(id: number): Observable<EvaluationDetail> {
    return this.http.get<EvaluationDetail>(`/api/employe/evaluations/${id}`);
  }
}
