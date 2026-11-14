import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { EmployeeDossierService } from '../../services/employee-dossier.service';

type AlertKey = 'documentsManquants' | 'documentsExpirants' | 'contratsEcheance' | 'informationsIncompletes' | 'correctionsEnAttente';
type AlertItem = Record<string, any>;

@Component({
  selector: 'app-employee-alerts-page', standalone: true, imports: [DatePipe],
  templateUrl: './employee-alerts-page.component.html', styleUrl: './employee-alerts-page.component.scss'
})
export class EmployeeAlertsPageComponent implements OnInit {
  private readonly service = inject(EmployeeDossierService);
  private readonly router = inject(Router);
  private readonly toast = inject(NotificationService);
  loading = true;
  data: Record<AlertKey, AlertItem[]> = {
    documentsManquants: [], documentsExpirants: [], contratsEcheance: [],
    informationsIncompletes: [], correctionsEnAttente: []
  };

  get sections(): { key: AlertKey; title: string; description: string; icon: string; items: AlertItem[] }[] {
    return [
      { key: 'documentsManquants', title: 'Documents obligatoires manquants', description: 'Pièces requises absentes des dossiers actifs.', icon: 'pi pi-file-excel', items: this.data.documentsManquants },
      { key: 'documentsExpirants', title: 'Documents expirés ou à échéance', description: 'Documents arrivés à expiration ou expirant dans les 30 jours.', icon: 'pi pi-clock', items: this.data.documentsExpirants },
      { key: 'contratsEcheance', title: 'Contrats à échéance', description: 'Contrats se terminant dans les 60 jours.', icon: 'pi pi-calendar-times', items: this.data.contratsEcheance },
      { key: 'informationsIncompletes', title: 'Informations personnelles incomplètes', description: 'Nom, téléphone ou région à compléter.', icon: 'pi pi-user-edit', items: this.data.informationsIncompletes },
      { key: 'correctionsEnAttente', title: 'Corrections à traiter', description: 'Demandes de modification du dossier en attente de décision RH.', icon: 'pi pi-inbox', items: this.data.correctionsEnAttente }
    ];
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.service.administrativeAlerts().pipe(finalize(() => this.loading = false)).subscribe({
      next: (data) => this.data = { ...this.data, ...data },
      error: () => this.toast.error('Alertes indisponibles', 'Impossible de charger les alertes administratives RH.')
    });
  }

  openEmployee(employeeId: unknown): void {
    const id = Number(employeeId);
    if (Number.isInteger(id) && id > 0) void this.router.navigateByUrl(`/espace/rh/personnel/employes/${id}`);
  }
}
