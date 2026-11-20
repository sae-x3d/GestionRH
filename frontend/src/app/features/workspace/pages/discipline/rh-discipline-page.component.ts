import { Component, OnInit, inject } from '@angular/core';
import { hrToday } from '../../../../core/date/hr-date';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { EmployeeDossierService, EmployeeSummary } from '../../services/employee-dossier.service';
import {
  DecisionPayload, DisciplineIncident, DisciplineProcedureDetail, DisciplineProcedureRow, DisciplineService
} from '../../services/discipline.service';

@Component({
  selector: 'app-rh-discipline-page', standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, ButtonModule, DialogModule, InputTextModule],
  templateUrl: './rh-discipline-page.component.html', styleUrl: './rh-discipline-page.component.scss'
})
export class RhDisciplinePageComponent implements OnInit {
  private readonly service = inject(DisciplineService);
  private readonly dossier = inject(EmployeeDossierService);
  private readonly notifications = inject(NotificationService);

  procedures: DisciplineProcedureRow[] = [];
  incidents: DisciplineIncident[] = [];
  employees: EmployeeSummary[] = [];
  statutFilter = '';
  loadingProcedures = true;
  loadingIncidents = true;
  loadingEmployees = true;

  detail: DisciplineProcedureDetail | null = null;
  detailVisible = false;
  detailLoading = false;

  declareVisible = false;
  declaring = false;
  declareForm = { employeId: null as number | null, graviteCode: 'LEGERE', titre: '', description: '', dateFaits: this.today() };

  explainVisible = false;
  explaining = false;
  explainProcedure: DisciplineProcedureRow | null = null;
  explainForm = { message: '', dateLimite: '' };

  decideVisible = false;
  deciding = false;
  decideProcedure: DisciplineProcedureRow | null = null;
  decideForm = { decision: 'AVERTISSEMENT', motif: '', dateDebut: '', dateFin: '' };

  busyIncidentId: number | null = null;
  readonly minDate = this.today();

  readonly gravites = [
    { code: 'LEGERE', libelle: 'Légère' },
    { code: 'MOYENNE', libelle: 'Moyenne' },
    { code: 'GRAVE', libelle: 'Grave' }
  ];
  readonly decisions = [
    { code: 'SANS_SUITE', libelle: 'Sans suite' },
    { code: 'AVERTISSEMENT', libelle: 'Avertissement' },
    { code: 'BLAME', libelle: 'Blâme' },
    { code: 'MISE_A_PIED', libelle: 'Mise à pied' }
  ];
  readonly statuts = [
    { code: '', libelle: 'Tous les statuts' },
    { code: 'OUVERTE', libelle: 'Ouverte' },
    { code: 'EXPLICATION_DEMANDEE', libelle: 'Explication demandée' },
    { code: 'EXPLICATION_RECUE', libelle: 'Explication reçue' },
    { code: 'DECIDEE', libelle: 'Décidée' },
    { code: 'SANS_SUITE', libelle: 'Sans suite' },
    { code: 'CLOTUREE', libelle: 'Clôturée' }
  ];

  ngOnInit(): void {
    this.loadProcedures();
    this.loadIncidents();
    this.loadEmployees();
  }

  get isMiseAPied(): boolean { return this.decideForm.decision === 'MISE_A_PIED'; }

  loadProcedures(): void {
    this.loadingProcedures = true;
    this.service.rhProcedures(this.statutFilter || undefined).subscribe({
      next: (rows) => { this.procedures = rows; this.loadingProcedures = false; },
      error: () => this.loadingProcedures = false
    });
  }

  loadIncidents(): void {
    this.loadingIncidents = true;
    this.service.rhIncidents(true).subscribe({
      next: (rows) => { this.incidents = rows; this.loadingIncidents = false; },
      error: () => this.loadingIncidents = false
    });
  }

  loadEmployees(): void {
    this.loadingEmployees = true;
    this.dossier.directory(false, { size: 500, sortBy: 'nom', sortDirection: 'asc' }).subscribe({
      next: (directory) => { this.employees = directory.content; this.loadingEmployees = false; },
      error: () => this.loadingEmployees = false
    });
  }

  openDetail(procedure: DisciplineProcedureRow): void {
    this.detailVisible = true;
    this.detail = null;
    this.detailLoading = true;
    this.service.rhProcedure(procedure.id).subscribe({
      next: (detail) => { this.detail = detail; this.detailLoading = false; },
      error: () => this.detailLoading = false
    });
  }

  openDeclare(): void {
    this.declareForm = { employeId: null, graviteCode: 'LEGERE', titre: '', description: '', dateFaits: this.today() };
    this.declareVisible = true;
  }

  submitDeclare(): void {
    const form = this.declareForm;
    if (form.employeId == null) { this.notifications.error('Employé requis', 'Sélectionnez l’employé concerné.'); return; }
    if (!form.titre.trim() || !form.description.trim()) { this.notifications.error('Champs requis', 'Renseignez le titre et la description de l’incident.'); return; }
    this.declaring = true;
    this.service.declareIncidentRh({
      employeId: form.employeId, graviteCode: form.graviteCode, titre: form.titre.trim(),
      description: form.description.trim(), dateFaits: form.dateFaits
    }).subscribe({
      next: () => {
        this.declaring = false;
        this.declareVisible = false;
        this.notifications.success('Incident déclaré', 'L’incident a été enregistré.');
        this.loadIncidents();
        this.loadProcedures();
      },
      error: () => this.declaring = false
    });
  }

  openProcedure(incident: DisciplineIncident): void {
    if (this.busyIncidentId !== null) return;
    this.busyIncidentId = incident.id;
    this.service.openProcedure(incident.id).subscribe({
      next: () => {
        this.busyIncidentId = null;
        this.notifications.success('Procédure ouverte', 'Vous pouvez demander une explication à l’employé.');
        this.loadIncidents();
        this.loadProcedures();
      },
      error: () => this.busyIncidentId = null
    });
  }

  openExplain(procedure: DisciplineProcedureRow): void {
    this.explainProcedure = procedure;
    this.explainForm = { message: '', dateLimite: '' };
    this.explainVisible = true;
  }

  submitExplain(): void {
    if (!this.explainProcedure) return;
    if (!this.explainForm.message.trim()) { this.notifications.error('Message requis', 'Rédigez la demande d’explication.'); return; }
    this.explaining = true;
    this.service.requestExplanation(this.explainProcedure.id, this.explainForm.message.trim(), this.explainForm.dateLimite || null).subscribe({
      next: (result) => {
        this.explaining = false;
        this.explainVisible = false;
        this.notifications.success('Demande envoyée', `Réponse attendue avant le ${this.formatDate(result.dateLimite)}.`);
        this.loadProcedures();
      },
      error: () => this.explaining = false
    });
  }

  openDecide(procedure: DisciplineProcedureRow): void {
    this.decideProcedure = procedure;
    this.decideForm = { decision: 'AVERTISSEMENT', motif: '', dateDebut: '', dateFin: '' };
    this.decideVisible = true;
  }

  submitDecide(): void {
    if (!this.decideProcedure) return;
    const form = this.decideForm;
    if (form.decision !== 'SANS_SUITE' && !form.motif.trim()) {
      this.notifications.error('Motif requis', 'Le motif de la sanction est obligatoire.'); return;
    }
    if (this.isMiseAPied && (!form.dateDebut || !form.dateFin)) {
      this.notifications.error('Période requise', 'Indiquez la période de la mise à pied.'); return;
    }
    const payload: DecisionPayload = { decision: form.decision as DecisionPayload['decision'] };
    if (form.decision !== 'SANS_SUITE') payload.motif = form.motif.trim();
    if (this.isMiseAPied) { payload.dateDebut = form.dateDebut; payload.dateFin = form.dateFin; }
    this.deciding = true;
    this.service.decide(this.decideProcedure.id, payload).subscribe({
      next: (result) => {
        this.deciding = false;
        this.decideVisible = false;
        this.notifications.success('Décision enregistrée', 'La décision a été appliquée.');
        if (result.retenues) this.notifications.success('Retenue de salaire', `${result.retenues} retenue(s) transmise(s) à la paie.`);
        for (const warning of result.avertissements ?? []) this.notifications.error('Retenue non intégrée', warning);
        this.loadProcedures();
      },
      error: () => this.deciding = false
    });
  }

  pieceUrl(pieceId: number): string { return this.service.downloadRhPiece(pieceId); }

  private today(): string { return hrToday(); }

  private formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('fr-FR');
  }
}
