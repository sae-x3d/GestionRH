import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface PayrollBulletin {
  id: number; periodeId: number; matricule: string; nom: string; fonction: string;
  dateDebut: string; dateFin: string; statut: string; statutCode: string;
  salaireBrut: number; netAPayer: number; salaireNet: number; categorie?: string;
}

@Injectable({ providedIn: 'root' })
export class PayrollPayslipsService {
  private readonly http = inject(HttpClient);
  own(): Observable<PayrollBulletin[]> { return this.http.get<PayrollBulletin[]>('/api/employe/paie/bulletins'); }
  rh(): Observable<PayrollBulletin[]> { return this.http.get<PayrollBulletin[]>('/api/rh/paie/bulletins'); }
  ownPdf(id: number): Observable<Blob> { return this.http.get(`/api/employe/paie/bulletins/${id}/pdf`, { responseType: 'blob' }); }
  rhPdf(id: number): Observable<Blob> { return this.http.get(`/api/rh/paie/bulletins/${id}/pdf`, { responseType: 'blob' }); }
}
