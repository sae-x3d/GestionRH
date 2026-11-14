import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PublicOffer {
  id: number;
  titre: string;
  description: string;
  competences: string | null;
  poste: string;
  departement: string;
  budgetSalaireBase: number | null;
  effectif: number;
  effectifRestant: number;
  datePublication: string | null;
  dateCloture: string;
}

@Injectable({ providedIn: 'root' })
export class PublicOffersService {
  constructor(private readonly http: HttpClient) { }

  list(): Observable<PublicOffer[]> { return this.http.get<PublicOffer[]>('/api/offres'); }
  get(id: number): Observable<PublicOffer> { return this.http.get<PublicOffer>(`/api/offres/${id}`); }
}
