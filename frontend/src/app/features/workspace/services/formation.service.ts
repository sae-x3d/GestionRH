import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Competence } from './competence-management.service';

export interface FormationNeed {
  id: number;
  employeId: number;
  matricule: string;
  employe: string;
  departement: string;
  poste: string;
  competenceCode: string;
  competence: string;
  statut: 'OUVERT' | 'PLANIFIE' | 'CLOTURE' | 'ANNULE';
  niveauAttendu: number;
  niveauAcquisInitial: number;
  ecartInitial: number;
  seuilEcart: number;
  createdAt: string;
}

export interface FormationRow {
  id: number;
  titre: string;
  competenceId: number;
  competenceCode: string;
  competence: string;
  organisme: string | null;
  dateDebut: string | null;
  dateFin: string | null;
  niveauCible: number | null;
  statut: 'PLANIFIEE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE';
  nbParticipants: number;
  nbTermines: number;
}

export interface FormationParticipant {
  participationId: number;
  employeId: number;
  matricule: string;
  employe: string;
  statut: 'INSCRIT' | 'TERMINE' | 'ABANDONNE';
  inscritAt: string;
  termineAt: string | null;
  besoinId: number;
  niveauAttendu: number;
  niveauAcquisInitial: number;
  mesureId: number | null;
  niveauAvant: number | null;
  niveauApres: number | null;
  manque: number | null;
  acquis: number | null;
  pourcentage: number | null;
  mesureAt: string | null;
  commentaire: string | null;
}

export interface EligibleNeed {
  id: number;
  employeId: number;
  matricule: string;
  employe: string;
  niveauAttendu: number;
  niveauAcquisInitial: number;
  ecartInitial: number;
}

export interface FormationDetail {
  formation: {
    id: number; titre: string; competenceId: number; competenceCode: string; competence: string;
    description: string | null; organisme: string | null; dateDebut: string | null; dateFin: string | null;
    niveauCible: number | null; statut: string; creePar: string; createdAt: string;
  };
  participants: FormationParticipant[];
  besoinsEligibles: EligibleNeed[];
  competences: Competence[];
  message?: string;
}

export interface MyFormation {
  participationId: number;
  formationId: number;
  titre: string;
  competenceCode: string;
  competence: string;
  organisme: string | null;
  dateDebut: string | null;
  dateFin: string | null;
  statutFormation: string;
  statutParticipation: string;
  niveauAttendu: number;
  niveauAcquisInitial: number;
  ecartInitial: number;
  niveauAvant: number | null;
  niveauApres: number | null;
  pourcentage: number | null;
  mesureAt: string | null;
  commentaireMesure: string | null;
}

@Injectable({ providedIn: 'root' })
export class FormationService {
  constructor(private readonly http: HttpClient) { }

  managerNeeds(): Observable<FormationNeed[]> {
    return this.http.get<FormationNeed[]>('/api/manager/formations/besoins');
  }

  rhNeeds(): Observable<FormationNeed[]> {
    return this.http.get<FormationNeed[]>('/api/rh/formations/besoins');
  }

  list(): Observable<FormationRow[]> {
    return this.http.get<FormationRow[]>('/api/rh/formations');
  }

  detail(id: number): Observable<FormationDetail> {
    return this.http.get<FormationDetail>(`/api/rh/formations/${id}`);
  }

  create(data: { competenceId: number; titre: string; description: string; organisme: string; dateDebut: string | null; dateFin: string | null; niveauCible: number | null }): Observable<FormationDetail> {
    return this.http.post<FormationDetail>('/api/rh/formations', data);
  }

  enroll(formationId: number, besoinId: number): Observable<FormationDetail> {
    return this.http.post<FormationDetail>(`/api/rh/formations/${formationId}/participants`, { besoinId });
  }

  measure(formationId: number, data: { participationId: number; niveauAvant: number; niveauApres: number; commentaire: string }): Observable<FormationDetail> {
    return this.http.post<FormationDetail>(`/api/rh/formations/${formationId}/mesures`, data);
  }

  myFormations(): Observable<MyFormation[]> {
    return this.http.get<MyFormation[]>('/api/employe/formations');
  }
}
