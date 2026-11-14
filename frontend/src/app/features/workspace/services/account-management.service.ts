import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ManagedUser {
  id: number;
  username: string;
  email: string;
  full_name: string;
  phone_number: string | null;
  region: string | null;
  photo_url: string | null;
  verification_requested_at: string | null;
  validation_status: string;
  validation_label: string;
  is_suspended: boolean;
  created_at: string;
  matricule: string | null;
  date_embauche: string | null;
  date_naissance: string | null;
  poste_id: number | null;
  poste: string | null;
  departement_id: number | null;
  departement: string | null;
  provenance: string | null;
  roles: string[];
}

export interface ManagedPost {
  id: number;
  intitule: string;
  departement: string | null;
  code_poste: string;
  code_departement: string;
}

@Injectable({ providedIn: 'root' })
export class AccountManagementService {
  private readonly baseUrl = '/api/gestionnaire/comptes';

  constructor(private readonly http: HttpClient) { }

  list(): Observable<ManagedUser[]> { return this.http.get<ManagedUser[]>(this.baseUrl); }
  employees(): Observable<ManagedUser[]> { return this.http.get<ManagedUser[]>(`${this.baseUrl}/employes`); }
  pending(): Observable<ManagedUser[]> { return this.http.get<ManagedUser[]>(`${this.baseUrl}/inscriptions`); }
  options(): Observable<{ roles: { code: string }[]; postes: ManagedPost[] }> {
    return this.http.get<{ roles: { code: string }[]; postes: ManagedPost[] }>(`${this.baseUrl}/options`);
  }
  create(account: Record<string, unknown>): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(this.baseUrl, account);
  }
  setRoles(id: number, roles: string[]): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${this.baseUrl}/${id}/roles`, { roles });
  }
  setSuspended(id: number, suspended: boolean): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.baseUrl}/${id}/suspension`, { suspended });
  }
  assignPost(id: number, posteId: number): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.baseUrl}/${id}/affectation`, { posteId });
  }
  decideRegistration(id: number, status: 'APPROVED' | 'REJECTED', comment: string): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.baseUrl}/${id}/inscription-decision`, { status, comment });
  }
}
