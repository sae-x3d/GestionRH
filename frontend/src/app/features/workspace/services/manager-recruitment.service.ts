import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ManagerDepartment {
  id: number;
  code: string;
  nom: string;
}

export interface ManagerPost {
  id: number;
  intitule: string;
  salaireBaseMin: number;
  salaireBaseMax: number;
  effectifAutorise: number;
  effectifActif: number;
  postesVacantsDisponibles: number;
  effectifMinimal: number;
}

export interface ReferenceOption {
  id: number;
  code: string;
  libelle: string;
}

export interface ManagerRecruitmentOptions {
  departement: ManagerDepartment;
  postes: ManagerPost[];
  motifs: ReferenceOption[];
}

export interface CreateRecruitmentRequest {
  posteId: number;
  motifId: number;
  effectif: number;
  budgetSalaireBase: number;
  justification: string;
  dateSouhaitee: string | null;
}

export interface RecruitmentRequestSummary {
  id: number;
  poste: string;
  motif: string;
  effectif: number;
  budgetSalaireBase: number;
  dateSouhaitee: string | null;
  statut: string;
  soumiseAt: string;
  departement: string;
  demandeur: string;
}

export interface RecruitmentRequestDetail extends RecruitmentRequestSummary {
  departement: string;
  justification: string;
  annulable: boolean;
  actionnable: boolean;
  historique: RecruitmentValidationHistory[];
}

export interface RecruitmentValidationHistory {
  etape: string;
  decision: string;
  validateur: string;
  commentaire: string | null;
  decideeAt: string;
}

@Injectable({ providedIn: 'root' })
export class ManagerRecruitmentService {
  private readonly baseUrl = '/api/manager/recrutement';

  constructor(private readonly http: HttpClient) { }

  getOptions(): Observable<ManagerRecruitmentOptions> {
    return this.http.get<ManagerRecruitmentOptions>(`${this.baseUrl}/options`);
  }

  submitRequest(request: CreateRecruitmentRequest): Observable<RecruitmentRequestSummary> {
    return this.http.post<RecruitmentRequestSummary>(`${this.baseUrl}/demandes`, request);
  }

  getRequests(): Observable<RecruitmentRequestSummary[]> {
    return this.http.get<RecruitmentRequestSummary[]>(`${this.baseUrl}/demandes`);
  }

  getRequest(id: number): Observable<RecruitmentRequestDetail> {
    return this.http.get<RecruitmentRequestDetail>(`${this.baseUrl}/demandes/${id}`);
  }

  cancelRequest(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/demandes/${id}/annuler`, {});
  }
}
