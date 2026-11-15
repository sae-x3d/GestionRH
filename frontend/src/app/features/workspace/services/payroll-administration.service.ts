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

export interface IrsaBracket {
  id?: number; dateDebut?: string; dateFin?: string | null; borneInferieure: number;
  borneSuperieure: number | null; taux: number; sourceRegle: string; motifModification?: string | null; modifiable?: boolean;
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

  getIrsa(): Observable<IrsaBracket[]> { return this.http.get<IrsaBracket[]>('/api/administration/paie/irsa'); }
  updateIrsa(dateEffet: string, motif: string, tranches: IrsaBracket[]): Observable<IrsaBracket[]> {
    return this.http.put<IrsaBracket[]>('/api/administration/paie/irsa', { dateEffet, motif, tranches: tranches.map(({ borneInferieure, borneSuperieure, taux, sourceRegle }) => ({ borneInferieure, borneSuperieure, taux, sourceRegle })) });
  }
}
