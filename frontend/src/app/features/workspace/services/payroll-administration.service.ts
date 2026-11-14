import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface PayrollParameter {
  id: number;
  code: string;
  libelle: string;
  valeur: number;
  unite: string;
  regime: string | null;
  regimeId: number | null;
  dateDebut: string;
  dateFin: string | null;
  sourceRegle: string;
  motifModification: string | null;
  aVerifier: boolean;
  modifiable: boolean;
  enVigueur: boolean;
  nouvelleValeur?: number;
  dateEffet?: string;
  motif?: string;
}

@Injectable({ providedIn: 'root' })
export class PayrollAdministrationService {
  private readonly http = inject(HttpClient);

  getParameters(): Observable<PayrollParameter[]> {
    return this.http.get<PayrollParameter[]>('/api/administration/paie/parametres');
  }

  updateParameter(id: number, valeur: number, dateEffet: string, motif: string): Observable<PayrollParameter[]> {
    return this.http.put<PayrollParameter[]>(`/api/administration/paie/parametres/${id}`, { valeur, dateEffet, motif });
  }
}
