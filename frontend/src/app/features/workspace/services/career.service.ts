import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CareerEmployeeSummary {
  id: number;
  matricule: string;
  nom: string;
  date_embauche: string;
  poste_id: number;
  poste: string;
  departement_id: number;
  departement: string;
  statut_code: string;
  statut: string;
  salaire_base?: number;
  derniere_affectation: string | null;
  dernier_salaire: string | null;
}

export interface CareerEmployee {
  id: number;
  matricule: string;
  nom: string;
  dateEmbauche: string;
  anciennete: string;
  posteId: number;
  poste: string;
  departementId: number;
  departement: string;
  statutCode: string;
  statut: string;
  salaireBase?: number;
  contratNumero?: string;
}

export interface CareerPostOption {
  id: number;
  intitule: string;
  departement_id: number;
  departement: string;
  salaire_base_min: number;
  salaire_base_max: number;
}

export interface CareerAffectation {
  id: number;
  dateEffet: string;
  motif: string | null;
  type: string;
  typeLibelle: string;
  ancienPosteId: number | null;
  ancienPoste: string | null;
  ancienDepartementId: number | null;
  ancienDepartement: string | null;
  nouveauPosteId: number;
  nouveauPoste: string;
  nouveauDepartementId: number;
  nouveauDepartement: string;
  acteur: string;
}

export interface CareerSalaire {
  id: number;
  dateEffet: string;
  motif: string | null;
  type: string;
  typeLibelle: string;
  ancienSalaireBase: number | null;
  nouveauSalaireBase: number;
  contratNumero: string | null;
  acteur: string;
}

export interface CareerPromotion {
  id: number;
  dateEffet: string;
  motif: string | null;
  ancienPoste: string;
  nouveauPoste: string;
  departement: string;
  ancienSalaireBase?: number;
  nouveauSalaireBase?: number;
  decidePar: string;
}

export interface CareerMutation {
  id: number;
  dateEffet: string;
  motif: string | null;
  ancienPoste: string;
  nouveauPoste: string;
  ancienDepartement: string;
  nouveauDepartement: string;
  ancienSalaireBase?: number;
  nouveauSalaireBase?: number;
  decidePar: string;
}

export interface CareerSituation {
  employeId: number;
  matricule: string;
  nom: string;
  date: string;
  dateEmbauche: string;
  anciennete: string;
  salaireVisible: boolean;
  affectation: CareerAffectation;
  posteId: number;
  poste: string;
  departementId: number;
  departement: string;
  statutCode: string;
  statut: string;
  salaireBase?: number;
  remuneration?: CareerSalaire;
}

export interface CareerFile {
  employe: CareerEmployee;
  salaireVisible: boolean;
  situationActuelle: CareerSituation;
  historiqueAffectations: CareerAffectation[];
  historiqueSalaires?: CareerSalaire[];
  promotions: CareerPromotion[];
  mutations: CareerMutation[];
  postesDisponibles?: CareerPostOption[];
  typesMouvement?: { code: string; libelle: string; categorie: string }[];
}

export interface CareerMovementRequest {
  employeId: number;
  nouveauPosteId?: number;
  nouveauSalaireBase?: number;
  dateEffet: string;
  motif: string;
}

@Injectable({ providedIn: 'root' })
export class CareerService {
  private readonly http = inject(HttpClient);

  rhEmployees(query?: string): Observable<CareerEmployeeSummary[]> {
    return this.http.get<CareerEmployeeSummary[]>('/api/rh/carriere/employes', { params: this.params(query) });
  }

  rhCareer(employeeId: number): Observable<CareerFile> {
    return this.http.get<CareerFile>(`/api/rh/carriere/employes/${employeeId}`);
  }

  rhSituation(employeeId: number, date?: string): Observable<CareerSituation> {
    return this.http.get<CareerSituation>(`/api/rh/carriere/employes/${employeeId}/situation`, { params: this.params(undefined, date) });
  }

  managerTeam(query?: string): Observable<CareerEmployeeSummary[]> {
    return this.http.get<CareerEmployeeSummary[]>('/api/manager/carriere/equipe', { params: this.params(query) });
  }

  managerCareer(employeeId: number): Observable<CareerFile> {
    return this.http.get<CareerFile>(`/api/manager/carriere/equipe/${employeeId}`);
  }

  managerSituation(employeeId: number, date?: string): Observable<CareerSituation> {
    return this.http.get<CareerSituation>(`/api/manager/carriere/equipe/${employeeId}/situation`, { params: this.params(undefined, date) });
  }

  ownCareer(): Observable<CareerFile> {
    return this.http.get<CareerFile>('/api/carriere/me');
  }

  ownSituation(date?: string): Observable<CareerSituation> {
    return this.http.get<CareerSituation>('/api/carriere/me/situation', { params: this.params(undefined, date) });
  }

  promotion(request: CareerMovementRequest): Observable<CareerFile> {
    return this.http.post<CareerFile>('/api/rh/carriere/promotions', request);
  }

  mutation(request: CareerMovementRequest): Observable<CareerFile> {
    return this.http.post<CareerFile>('/api/rh/carriere/mutations', request);
  }

  changementPoste(request: CareerMovementRequest): Observable<CareerFile> {
    return this.http.post<CareerFile>('/api/rh/carriere/changements-poste', request);
  }

  changementSalaire(request: CareerMovementRequest): Observable<CareerFile> {
    return this.http.post<CareerFile>('/api/rh/carriere/changements-salaire', request);
  }

  private params(query?: string, date?: string): HttpParams {
    let params = new HttpParams();
    if (query?.trim()) params = params.set('q', query.trim());
    if (date) params = params.set('date', date);
    return params;
  }
}
