import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CandidateApplicationService } from '../../../public/services/candidate-application.service';
import { ExitManagementService, ExitRequest } from '../../services/exit-management.service';
import { NotificationService } from '../../../../core/notifications/notification.service';

@Component({
  selector: 'app-employee-contract-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './employee-contract-page.component.html',
  styleUrl: './employee-contract-page.component.scss'
})
export class EmployeeContractPageComponent implements OnInit {
  private readonly applications = inject(CandidateApplicationService);
  private readonly exitsService = inject(ExitManagementService);
  private readonly notifications = inject(NotificationService);
  contract: Record<string, any> | null = null;
  exits: ExitRequest[] = [];
  loading = true;
  submittingResignation = false;
  dateNotification = this.today();
  dernierJourTravaille = '';
  commentaire = '';

  ngOnInit(): void {
    this.applications.employeeContract().subscribe({
      next: (contract) => { this.contract = contract; this.loading = false; this.loadExits(); },
      error: () => { this.contract = null; this.loading = false; }
    });
  }
  get activeExit(): ExitRequest | undefined { return this.exits.find(exit => !['FINALISEE','ANNULEE'].includes(exit.statutCode)); }
  submitResignation(): void {
    if (!this.contract || this.contract['statut_code'] !== 'ACTIF' || this.submittingResignation || this.activeExit) return;
    if (!this.dernierJourTravaille || this.dernierJourTravaille < this.dateNotification) {
      this.notifications.error('Dates à vérifier', 'Le dernier jour travaillé doit être égal ou postérieur à la date de notification.'); return;
    }
    if (!window.confirm('Transmettre votre demande de démission aux ressources humaines ? Votre contrat ne sera pas clôturé avant le traitement du dossier.')) return;
    this.submittingResignation = true;
    this.exitsService.resign({ contractId: Number(this.contract['contrat_id']), dateNotification: this.dateNotification,
      dernierJourTravaille: this.dernierJourTravaille, commentaire: this.commentaire.trim() }).subscribe({
      next: () => { this.submittingResignation = false; this.dernierJourTravaille = ''; this.commentaire = ''; this.loadExits();
        this.notifications.success('Demande transmise', 'Les RH ont été avisées et traiteront le décompte de sortie, le paiement et les documents.'); },
      error: (error) => { this.submittingResignation = false; this.notifications.error('Demande non envoyée', error.error?.message ?? 'Le contrat est peut-être déjà concerné par un dossier de sortie.'); }
    });
  }
  private loadExits(): void { this.exitsService.ownList().subscribe({ next: rows => this.exits = rows, error: () => this.exits = [] }); }
  private today(): string { const now=new Date(); return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`; }
}
