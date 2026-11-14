import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RecruitmentRequestDetail, RecruitmentRequestSummary } from './manager-recruitment.service';

export type WorkflowArea = 'rh' | 'finance' | 'administration';
export type RecruitmentDecision = 'APPROUVEE' | 'REFUSEE';

export interface OfferDraft {
  titre: string;
  description: string;
  competences: string;
  dateCloture: string;
}

export interface CreatedOffer {
  id: number;
  titre: string;
  statut: string;
}

@Injectable({ providedIn: 'root' })
export class RecruitmentWorkflowService {
  constructor(private readonly http: HttpClient) { }

  pending(area: WorkflowArea): Observable<RecruitmentRequestSummary[]> {
    return this.http.get<RecruitmentRequestSummary[]>(`/api/${area}/recrutement/demandes`);
  }

  detail(area: WorkflowArea, id: number): Observable<RecruitmentRequestDetail> {
    return this.http.get<RecruitmentRequestDetail>(`/api/${area}/recrutement/demandes/${id}`);
  }

  decide(area: Exclude<WorkflowArea, 'administration'>, id: number, decision: RecruitmentDecision, commentaire: string): Observable<RecruitmentRequestDetail> {
    return this.http.post<RecruitmentRequestDetail>(`/api/${area}/recrutement/demandes/${id}/decision`, { decision, commentaire });
  }

  createOffer(id: number, offer: OfferDraft): Observable<CreatedOffer> {
    return this.http.post<CreatedOffer>(`/api/administration/recrutement/demandes/${id}/offre`, offer);
  }
}
