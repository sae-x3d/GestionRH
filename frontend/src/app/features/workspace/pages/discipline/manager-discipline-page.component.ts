import { Component, OnInit, inject } from '@angular/core';
import { hrToday } from '../../../../core/date/hr-date';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { EmployeeDossierService, EmployeeSummary } from '../../services/employee-dossier.service';
import { DisciplineIncident, DisciplineService } from '../../services/discipline.service';

@Component({
  selector: 'app-manager-discipline-page', standalone: true,
  imports: [DatePipe, FormsModule, ButtonModule, DialogModule, InputTextModule],
  templateUrl: './manager-discipline-page.component.html', styleUrl: './manager-discipline-page.component.scss'
})
export class ManagerDisciplinePageComponent implements OnInit {
  private readonly service = inject(DisciplineService);
  private readonly dossier = inject(EmployeeDossierService);
  private readonly notifications = inject(NotificationService);

  incidents: DisciplineIncident[] = [];
  employees: EmployeeSummary[] = [];
  loadingIncidents = true;
  loadingEmployees = true;

  declareVisible = false;
  declaring = false;
  declareForm = { employeId: null as number | null, graviteCode: 'LEGERE', titre: '', description: '', dateFaits: this.today() };

  readonly gravites = [
    { code: 'LEGERE', libelle: 'Légère' },
    { code: 'MOYENNE', libelle: 'Moyenne' },
    { code: 'GRAVE', libelle: 'Grave' }
  ];

  ngOnInit(): void {
    this.loadIncidents();
    this.loadEmployees();
  }

  loadIncidents(): void {
    this.loadingIncidents = true;
    this.service.managerIncidents().subscribe({
      next: (rows) => { this.incidents = rows; this.loadingIncidents = false; },
      error: () => this.loadingIncidents = false
    });
  }

  loadEmployees(): void {
    this.loadingEmployees = true;
    this.dossier.directory(true, { size: 500, sortBy: 'nom', sortDirection: 'asc' }).subscribe({
      next: (directory) => { this.employees = directory.content; this.loadingEmployees = false; },
      error: () => this.loadingEmployees = false
    });
  }

  openDeclare(): void {
    this.declareForm = { employeId: null, graviteCode: 'LEGERE', titre: '', description: '', dateFaits: this.today() };
    this.declareVisible = true;
  }

  submitDeclare(): void {
    const form = this.declareForm;
    if (form.employeId == null) { this.notifications.error('Employé requis', 'Sélectionnez un membre de votre équipe.'); return; }
    if (!form.titre.trim() || !form.description.trim()) { this.notifications.error('Champs requis', 'Renseignez le titre et la description de l’incident.'); return; }
    this.declaring = true;
    this.service.declareIncidentManager({
      employeId: form.employeId, graviteCode: form.graviteCode, titre: form.titre.trim(),
      description: form.description.trim(), dateFaits: form.dateFaits
    }).subscribe({
      next: () => {
        this.declaring = false;
        this.declareVisible = false;
        this.notifications.success('Incident déclaré', 'Les RH ont été informées de l’incident.');
        this.loadIncidents();
      },
      error: () => this.declaring = false
    });
  }

  private today(): string { return hrToday(); }
}
