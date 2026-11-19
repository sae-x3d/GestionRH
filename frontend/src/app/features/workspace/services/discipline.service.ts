import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface DisciplineIncident {
  id: number;
  titre: string;
  description: string;
  dateFaits: string;
  createdAt: string;
  employeId: number;
  matricule: string;
  employe: string;
  poste: string;
  departement: string;
  graviteCode: string;
  gravite: string;
  procedureId: number | null;
  statutCode: string | null;
  statut: string | null;
}

export interface DisciplineProcedureRow {
  id: number;
  ouverteAt: string;
  clotureeAt: string | null;
  statutCode: string;
  statut: string;
  incidentId: number;
  titre: string;
  dateFaits: string;
  graviteCode: string;
  gravite: string;
  employeId: number;
  matricule: string;
  employe: string;
  poste: string;
  departement: string;
  demandeId: number | null;
  dateLimite: string | null;
  reponseRecue: number;
  sanctionId: number | null;
  sanctionCode: string | null;
  sanction: string | null;
}

export interface DisciplinePiece {
  id: number;
  nomFichier: string;
  typeMime: string;
  tailleOctets: number;
  reponseId: number | null;
  ajouteeAt: string;
}

export interface DisciplineRetenue {
  id: number;
  dateDebut: string;
  dateFin: string;
  nombreJours: number;
  tauxJournalier: number;
  montant: number;
  variablePaieId: number | null;
}

export interface DisciplineProcedureDetail {
  id: number;
  incidentId: number;
  ouverteAt: string;
  clotureeAt: string | null;
  statutCode: string;
  statut: string;
  titre: string;
  description: string;
  dateFaits: string;
  declareAt: string;
  graviteCode: string;
  gravite: string;
  employeId: number;
  matricule: string;
  employe: string;
  poste: string;
  departement: string;
  demandeId: number | null;
  demandeMessage: string | null;
  dateLimite: string | null;
  demandeeAt: string | null;
  reponseId: number | null;
  reponseTexte: string | null;
  repondueAt: string | null;
  sanctionId: number | null;
  sanctionMotif: string | null;
  sanctionDebut: string | null;
  sanctionFin: string | null;
  sanctionJours: number | null;
  sanctionDecideeAt: string | null;
  sanctionCode: string | null;
  sanction: string | null;
  pieces: DisciplinePiece[];
  retenues: DisciplineRetenue[];
}

export interface EmployeeDisciplineProcedure {
  procedureId: number;
  ouverteAt: string;
  clotureeAt: string | null;
  statutCode: string;
  statut: string;
  incidentId: number;
  titre: string;
  description: string;
  dateFaits: string;
  graviteCode: string;
  gravite: string;
  demandeId: number | null;
  demandeMessage: string | null;
  dateLimite: string | null;
  demandeeAt: string | null;
  reponseId: number | null;
  reponseTexte: string | null;
  repondueAt: string | null;
  sanctionId: number | null;
  sanctionCode: string | null;
  sanction: string | null;
  sanctionMotif: string | null;
  sanctionDebut: string | null;
  sanctionFin: string | null;
  nombrePieces: number;
  pieces: DisciplinePiece[];
}

export interface DeclareIncidentPayload {
  employeId: number;
  graviteCode: string;
  titre: string;
  description: string;
  dateFaits: string;
}

export interface DecisionPayload {
  decision: 'SANS_SUITE' | 'AVERTISSEMENT' | 'BLAME' | 'MISE_A_PIED';
  motif?: string;
  dateDebut?: string;
  dateFin?: string;
}

export interface DecisionResult {
  procedureId: number;
  decision: string;
  sanctionId?: number;
  retenues?: number;
  avertissements?: string[];
}

@Injectable({ providedIn: 'root' })
export class DisciplineService {
  constructor(private readonly http: HttpClient) { }

  rhIncidents(sansProcedure = false): Observable<DisciplineIncident[]> {
    return this.http.get<DisciplineIncident[]>('/api/rh/discipline/incidents', {
      params: new HttpParams().set('sansProcedure', String(sansProcedure))
    });
  }

  rhProcedures(statut?: string): Observable<DisciplineProcedureRow[]> {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http.get<DisciplineProcedureRow[]>('/api/rh/discipline/procedures', { params });
  }

  rhProcedure(id: number): Observable<DisciplineProcedureDetail> {
    return this.http.get<DisciplineProcedureDetail>(`/api/rh/discipline/procedures/${id}`);
  }

  declareIncidentRh(payload: DeclareIncidentPayload): Observable<DisciplineIncident> {
    return this.http.post<DisciplineIncident>('/api/rh/discipline/incidents', payload);
  }

  openProcedure(incidentId: number): Observable<DisciplineProcedureRow> {
    return this.http.post<DisciplineProcedureRow>(`/api/rh/discipline/incidents/${incidentId}/procedure`, {});
  }

  requestExplanation(procedureId: number, message: string, dateLimite: string | null): Observable<{ procedureId: number; demandeId: number; dateLimite: string }> {
    return this.http.post<{ procedureId: number; demandeId: number; dateLimite: string }>(
      `/api/rh/discipline/procedures/${procedureId}/demande-explication`, { message, dateLimite });
  }

  decide(procedureId: number, payload: DecisionPayload): Observable<DecisionResult> {
    return this.http.post<DecisionResult>(`/api/rh/discipline/procedures/${procedureId}/decision`, payload);
  }

  downloadRhPiece(pieceId: number): string {
    return `/api/rh/discipline/pieces/${pieceId}`;
  }

  managerIncidents(): Observable<DisciplineIncident[]> {
    return this.http.get<DisciplineIncident[]>('/api/manager/discipline/incidents');
  }

  declareIncidentManager(payload: DeclareIncidentPayload): Observable<DisciplineIncident> {
    return this.http.post<DisciplineIncident>('/api/manager/discipline/incidents', payload);
  }

  employeeProcedures(): Observable<EmployeeDisciplineProcedure[]> {
    return this.http.get<EmployeeDisciplineProcedure[]>('/api/employe/discipline');
  }

  respond(demandeId: number, texte: string, pieces: File[]): Observable<{ demandeId: number; reponseId: number; pieces: number }> {
    const form = new FormData();
    form.set('texte', texte);
    for (const file of pieces) form.append('pieces', file);
    return this.http.post<{ demandeId: number; reponseId: number; pieces: number }>(
      `/api/employe/discipline/demandes/${demandeId}/reponse`, form);
  }

  downloadEmployeePiece(pieceId: number): string {
    return `/api/employe/discipline/pieces/${pieceId}`;
  }
}
