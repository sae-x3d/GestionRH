import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface StaffingRuleView {
  posteId: number;
  departement: string;
  codeDepartement: string;
  poste: string;
  effectifAutorise: number;
  effectifActif: number;
  effectifMinimal: number;
  minimumDateDebut: string | null;
  postesARecruter: number;
  nouveauMinimum?: number;
  motif?: string;
}

export interface LeaveTypeRuleView {
  typeId: number;
  code: string;
  libelle: string;
  justificatifRequis: boolean;
  actif: boolean;
  acquisitionActive: boolean;
  joursParMois: number | null;
  dateDebut: string | null;
  nouveauTaux?: number | null;
}

export interface LeaveConfiguration {
  effectifs: StaffingRuleView[];
  typesConge: LeaveTypeRuleView[];
}

export interface LeaveBalance { typeId: number; code: string; type: string; solde: number; acquisitionActive: boolean; }
export interface LeaveTypeOption { id: number; code: string; libelle: string; justificatifRequis: boolean; acquisitionActive: boolean; }
export interface LeaveRequestRow {
  id: number; employeId: number; userId: number; username: string; employe: string; departement: string; poste: string;
  type: string; dateDebut: string; dateFin: string; jours: number; motif: string | null; statutCode: string; statut: string; soumiseAt: string;
  justificatifRequis?: boolean; justificatifPresent?: boolean;
}
export interface LeaveEmployeeOverview { soldes: LeaveBalance[]; types: LeaveTypeOption[]; demandes: LeaveRequestRow[]; }
export interface LeaveDecisionResult { statut: string; alerteEffectif: string | null; message: string; }
export interface RecordedAbsence { id: number; matricule: string; employe: string; departement: string; type: string; dateDebut: string; dateFin: string; motif: string; justifiee: boolean; justificatifPresent: boolean; }

@Injectable({ providedIn: 'root' })
export class LeaveManagementService {
  private readonly configUrl = '/api/rh/conges/parametres';
  constructor(private readonly http: HttpClient) { }

  getConfiguration(): Observable<LeaveConfiguration> {
    return this.http.get<LeaveConfiguration>(this.configUrl);
  }

  setMinimum(posteId: number, minimumPresence: number, dateDebut: string, motif: string): Observable<LeaveConfiguration> {
    return this.http.post<LeaveConfiguration>(`${this.configUrl}/effectifs/${posteId}`, { minimumPresence, dateDebut, motif });
  }

  setAccrual(typeId: number, joursParMois: number, dateDebut: string): Observable<LeaveConfiguration> {
    return this.http.post<LeaveConfiguration>(`${this.configUrl}/acquisition/${typeId}`, { joursParMois, dateDebut });
  }

  setLeaveTypePolicy(typeId: number, actif: boolean, justificatifRequis: boolean): Observable<LeaveConfiguration> {
    return this.http.post<LeaveConfiguration>(`${this.configUrl}/types/${typeId}`, { actif, justificatifRequis });
  }

  getEmployeeOverview(): Observable<LeaveEmployeeOverview> {
    return this.http.get<LeaveEmployeeOverview>('/api/employe/conges');
  }

  submitLeave(request: { typeCongeId: number; dateDebut: string; dateFin: string; motif: string }): Observable<{ id: number; type: string; jours: number; statut: string }> {
    return this.http.post<{ id: number; type: string; jours: number; statut: string }>('/api/employe/conges', request);
  }

  attachLeaveJustification(id: number, file: File): Observable<{ demandeId: number; nomFichier: string; message: string }> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<{ demandeId: number; nomFichier: string; message: string }>(`/api/employe/conges/${id}/justificatif`, body);
  }

  getApprovalQueue(role: 'manager' | 'rh'): Observable<LeaveRequestRow[]> {
    return this.http.get<LeaveRequestRow[]>(`/api/${role}/conges/demandes`);
  }

  decideLeave(role: 'manager' | 'rh', id: number, decision: 'APPROUVEE' | 'REFUSEE', commentaire: string): Observable<LeaveDecisionResult> {
    return this.http.post<LeaveDecisionResult>(`/api/${role}/conges/demandes/${id}/decision`, { decision, commentaire });
  }

  listAbsences(debut: string, fin: string): Observable<RecordedAbsence[]> {
    return this.http.get<RecordedAbsence[]>('/api/rh/conges/absences', { params: { debut, fin } });
  }

  getAbsenceTypes(): Observable<{ code: string; libelle: string; justificatifRequis: boolean }[]> {
    return this.http.get<{ code: string; libelle: string; justificatifRequis: boolean }[]>('/api/rh/conges/types-absence');
  }

  recordAbsence(data: { employeId: number; typeCode: string; dateDebut: string; dateFin: string; motif: string; justifiee: boolean }, file?: File): Observable<{ id: number; message: string; alerteEffectif: string | null }> {
    const form = new FormData();
    for (const [key, value] of Object.entries(data)) form.set(key, String(value));
    if (file) form.set('justificatif', file);
    return this.http.post<{ id: number; message: string; alerteEffectif: string | null }>('/api/rh/conges/absences', form);
  }
}
