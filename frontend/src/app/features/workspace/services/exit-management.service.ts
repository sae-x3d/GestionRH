import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ExitMotif { code: string; libelle: string; }
export interface ExitEmployeeOption { employeeId: number; matricule: string; employeeName: string; contractId: number; contractNumber: string; poste: string; }
export interface ExitRequest {
  id: number; employeId: number; matricule: string; nomEmploye: string; contratId: number; numeroContrat: string;
  motifCode: string; motif: string; statutCode: string; statut: string; dateNotification: string;
  dernierJourTravaille: string; commentaire?: string; totalStcBrut?: number;
  totalRetenuesProvisoires?: number; referencePaiement?: string; datePaiement?: string;
  montantNetValide?: number; controleNetDetail?: string; documentsGeneres?: number;
  lignesStc?: Array<{code:string;libelle:string;quantite:number;montantUnitaire:number;montant:number;detail:string}>;
  historique?: Array<{date:string;commentaire:string;acteur:string;statut:string}>;
}
export interface CreateExit { employeeId:number; contractId:number; motifCode:string; dateNotification:string; dernierJourTravaille:string; commentaire:string; }
export interface CreateOwnResignation { contractId:number; dateNotification:string; dernierJourTravaille:string; commentaire:string; }

@Injectable({ providedIn: 'root' })
export class ExitManagementService {
  private readonly http = inject(HttpClient);
  motifs(): Observable<ExitMotif[]> { return this.http.get<ExitMotif[]>('/api/sorties/motifs'); }
  managerOptions(): Observable<ExitEmployeeOption[]> { return this.http.get<ExitEmployeeOption[]>('/api/manager/sorties/options'); }
  list(role:'manager'|'rh'): Observable<ExitRequest[]> { return this.http.get<ExitRequest[]>(`/api/${role}/sorties`); }
  employeeList(employeeId:number): Observable<ExitRequest[]> { return this.http.get<ExitRequest[]>(`/api/rh/employes/${employeeId}/sorties`); }
  ownList(): Observable<ExitRequest[]> { return this.http.get<ExitRequest[]>('/api/employe/sorties'); }
  resign(payload:CreateOwnResignation): Observable<ExitRequest> { return this.http.post<ExitRequest>('/api/employe/sorties/demission',payload); }
  dismiss(payload:CreateExit): Observable<ExitRequest> { return this.http.post<ExitRequest>('/api/rh/sorties/licenciement',payload); }
  create(payload:CreateExit): Observable<ExitRequest> { return this.http.post<ExitRequest>('/api/manager/sorties', payload); }
  prepare(id:number): Observable<ExitRequest> { return this.http.post<ExitRequest>(`/api/rh/sorties/${id}/preparer-stc`,{}); }
  validate(id:number,montantNet:number,justificationNet:string): Observable<ExitRequest> { return this.http.post<ExitRequest>(`/api/rh/sorties/${id}/valider`,{montantNet,justificationNet}); }
  confirmPayment(id:number,reference:string,date:string,montant:number): Observable<ExitRequest> { return this.http.post<ExitRequest>(`/api/rh/sorties/${id}/paiement`,{reference,date,montant}); }
  generateDocuments(id:number): Observable<ExitRequest> { return this.http.post<ExitRequest>(`/api/rh/sorties/${id}/generer-documents`,{}); }
  documentsRemitted(id:number): Observable<ExitRequest> { return this.http.post<ExitRequest>(`/api/rh/sorties/${id}/documents-remis`,{}); }
  finalize(id:number): Observable<ExitRequest> { return this.http.post<ExitRequest>(`/api/rh/sorties/${id}/finaliser`,{}); }
  cancel(id:number,motif:string): Observable<ExitRequest> { return this.http.post<ExitRequest>(`/api/rh/sorties/${id}/annuler`,{motif}); }
}
