import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface PayrollPeriod {
  id: number; dateDebut: string; dateFin: string; statutCode: string; statut: string;
  nombreBulletins: number; totalBrut: number; totalNet: number; totalCharges: number;
}
export interface PayrollPeriodBulletin {
  id: number; employeId: number; matricule: string; nom: string; fonction: string;
  salaireBase: number; brut: number; revenuImposable: number; retenues: number;
  salaireNet: number; charges: number; net: number; statutCode: string; statut: string;
}
export interface PayrollVariableOptions {
  employees: { id:number; matricule:string; nom:string }[];
  rubriques: { id:number; code:string; libelle:string; categorie:string }[];
  variables: { id:number; matricule:string; nom:string; rubrique:string; categorie:string; montant:number; sourceDetail:string; statutCode:string; statut:string; saisiParId:number; motifDecision:string|null }[];
  avances: { id:number; matricule:string; nom:string; dateAvance:string; montant:number; motif:string; statutCode:string; statut:string; saisiParId:number }[];
}

@Injectable({ providedIn: 'root' })
export class PayrollOperationsService {
  private readonly http = inject(HttpClient);
  periods(): Observable<PayrollPeriod[]> { return this.http.get<PayrollPeriod[]>('/api/rh/paie/periodes'); }
  create(dateDebut: string, dateFin: string): Observable<PayrollPeriod> { return this.http.post<PayrollPeriod>('/api/rh/paie/periodes', { dateDebut, dateFin }); }
  calculate(id: number): Observable<PayrollPeriod> { return this.http.post<PayrollPeriod>(`/api/rh/paie/periodes/${id}/calculer`, {}); }
  validate(id: number): Observable<PayrollPeriod> { return this.http.post<PayrollPeriod>(`/api/rh/paie/periodes/${id}/valider`, {}); }
  markPaid(id: number,reference:string): Observable<PayrollPeriod> { return this.http.post<PayrollPeriod>(`/api/rh/paie/periodes/${id}/payer`, {reference}); }
  close(id: number): Observable<PayrollPeriod> { return this.http.post<PayrollPeriod>(`/api/rh/paie/periodes/${id}/cloturer`, {}); }
  bulletins(id: number): Observable<PayrollPeriodBulletin[]> { return this.http.get<PayrollPeriodBulletin[]>(`/api/rh/paie/periodes/${id}/bulletins`); }
  payrollExcel(id: number): Observable<Blob> { return this.http.get(`/api/rh/paie/periodes/${id}/etat-excel`, { responseType: 'blob' }); }
  variableOptions(id: number): Observable<PayrollVariableOptions> { return this.http.get<PayrollVariableOptions>(`/api/rh/paie/periodes/${id}/variables`); }
  createVariable(id:number,employeId:number,rubriqueId:number,montant:number,justification:string):Observable<PayrollVariableOptions>{return this.http.post<PayrollVariableOptions>(`/api/rh/paie/periodes/${id}/variables`,{employeId,rubriqueId,montant,justification});}
  decideVariable(periodId:number,variableId:number,approuver:boolean,motif:string):Observable<PayrollVariableOptions>{return this.http.post<PayrollVariableOptions>(`/api/rh/paie/periodes/${periodId}/variables/${variableId}/decision`,{approuver,motif});}
  createAdvance(id:number,employeId:number,dateAvance:string,montant:number,motif:string):Observable<PayrollVariableOptions>{return this.http.post<PayrollVariableOptions>(`/api/rh/paie/periodes/${id}/avances`,{employeId,dateAvance,montant,motif});}
  decideAdvance(periodId:number,advanceId:number,approuver:boolean,motif:string):Observable<PayrollVariableOptions>{return this.http.post<PayrollVariableOptions>(`/api/rh/paie/periodes/${periodId}/avances/${advanceId}/decision`,{approuver,motif});}
  advanceOptions(id:number):Observable<PayrollVariableOptions>{return this.http.get<PayrollVariableOptions>(`/api/rh/paie/periodes/${id}/avances`);}
}
