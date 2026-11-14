import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface EmployeeSummary {
  id: number; matricule: string; nom: string; email?: string; date_embauche: string;
  departement_id: number; departement: string; poste_id: number; poste: string;
  provenance_code: string; provenance: string; statut_code: string; statut: string; photo_url?: string | null; responsables_departement?: string | null;
}
export interface EmployeeDirectory {
  content: EmployeeSummary[]; page: number; size: number; totalElements: number; totalPages: number;
  departements?: { id: number; nom: string }[];
  postes?: { id: number; intitule: string; departementId: number }[];
  statuts?: { code: string; libelle: string }[];
}

@Injectable({ providedIn: 'root' })
export class EmployeeDossierService {
  private readonly http = inject(HttpClient);
  directory(manager: boolean, filters: { q?: string; departementId?: number | null; posteId?: number | null; statut?: string; page?: number; size?: number; sortBy?: string; sortDirection?: string }): Observable<EmployeeDirectory> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) if (value !== undefined && value !== null && value !== '') params = params.set(key, String(value));
    return this.http.get<EmployeeDirectory>(manager ? '/api/manager/equipe/effectif' : '/api/rh/employes', { params });
  }
  dossier(id?: number): Observable<Record<string, any>> {
    return this.http.get<Record<string, any>>(id ? `/api/dossiers/employes/${id}` : '/api/dossiers/me');
  }
  documentTypes(employeeId: number): Observable<{ id: number; code: string; libelle: string; obligatoire: boolean }[]> {
    return this.http.get<{ id: number; code: string; libelle: string; obligatoire: boolean }[]>(`/api/dossiers/employes/${employeeId}/documents/types`);
  }
  uploadDocument(employeeId: number, form: FormData): Observable<Record<string, any>> {
    return this.http.post<Record<string, any>>(`/api/dossiers/employes/${employeeId}/documents`, form);
  }
  archiveDocument(employeeId: number, documentId: number): Observable<void> {
    return this.http.post<void>(`/api/rh/dossiers/employes/${employeeId}/documents/${documentId}/archiver`, {});
  }
  corrections(rh: boolean): Observable<Record<string, any>[]> {
    return this.http.get<Record<string, any>[]>(rh ? '/api/rh/dossiers/demandes' : '/api/dossiers/me/demandes');
  }
  submitCorrection(data: { champ: string; nouvelleValeur: string; motif: string }, attachment?: File): Observable<Record<string, any>> {
    if (attachment) {
      const form = new FormData();
      form.set('champ', data.champ); form.set('nouvelleValeur', data.nouvelleValeur); form.set('motif', data.motif); form.set('pieceJointe', attachment);
      return this.http.post<Record<string, any>>('/api/dossiers/me/demandes/avec-piece-jointe', form);
    }
    return this.http.post<Record<string, any>>('/api/dossiers/me/demandes', data);
  }
  cancelCorrection(id: number): Observable<Record<string, any>> { return this.http.delete<Record<string, any>>(`/api/dossiers/me/demandes/${id}`); }
  downloadCorrectionAttachment(id: number): Observable<Blob> { return this.http.get(`/api/dossiers/me/demandes/${id}/piece-jointe`, { responseType: 'blob' }); }
  decideCorrection(id: number, decision: 'APPROUVEE' | 'REFUSEE', commentaire: string): Observable<Record<string, any>> {
    return this.http.post<Record<string, any>>(`/api/rh/dossiers/demandes/${id}/decision`, { decision, commentaire });
  }
  download(employeeId: number, documentId: number): Observable<Blob> {
    return this.http.get(`/api/dossiers/employes/${employeeId}/documents/${documentId}/telecharger`, { responseType: 'blob' });
  }
  documentVersions(employeeId: number, documentId: number): Observable<Record<string, any>[]> {
    return this.http.get<Record<string, any>[]>(`/api/dossiers/employes/${employeeId}/documents/${documentId}/versions`);
  }
  downloadVersion(employeeId: number, documentId: number, versionId: number): Observable<Blob> {
    return this.http.get(`/api/dossiers/employes/${employeeId}/documents/${documentId}/versions/${versionId}/telecharger`, { responseType: 'blob' });
  }
  downloadRecruitmentCv(employeeId: number): Observable<Blob> {
    return this.http.get(`/api/dossiers/employes/${employeeId}/cv-recrutement`, { responseType: 'blob' });
  }
  downloadContract(employeeId: number, contractId: number): Observable<Blob> {
    return this.http.get(`/api/dossiers/employes/${employeeId}/contrats/${contractId}/document`, { responseType: 'blob' });
  }
  changeStatus(employeeId: number, data: { statut: string; dateEffet: string; motif: string }): Observable<Record<string, any>> {
    return this.http.patch<Record<string, any>>(`/api/rh/dossiers/employes/${employeeId}/statut`, data);
  }
  administrativeAlerts(): Observable<Record<string, Record<string, any>[]>> {
    return this.http.get<Record<string, Record<string, any>[]>>('/api/rh/dossiers/alertes');
  }
}
