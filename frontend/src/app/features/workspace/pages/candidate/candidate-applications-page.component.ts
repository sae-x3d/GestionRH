import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { CandidateApplicationService } from '../../../public/services/candidate-application.service';

@Component({
  selector: 'app-candidate-applications-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './candidate-applications-page.component.html',
  styleUrl: './candidate-applications-page.component.scss'
})
export class CandidateApplicationsPageComponent implements OnInit {
  private readonly applications = inject(CandidateApplicationService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  applicationsList: Record<string, any>[] = [];
  selected: Record<string, any> | null = null;
  loading = true;
  busy = false;
  proposing = false;
  reglesAcceptees = false;
  dateNaissance = '';
  dateDebut = '';
  salaire = '';
  typeContrat = 'CDI_PERIODE_ESSAI';
  commentaire = '';

  ngOnInit(): void { this.load(); }

  open(item: Record<string, any>): void {
    this.applications.myApplicationDetails(Number(item['id'])).subscribe({
      next: (detail) => {
        this.selected = detail;
        this.proposing = false;
        this.reglesAcceptees = false;
        this.dateNaissance = '';
        this.dateDebut = String(detail['date_debut_finale'] ?? detail['date_debut_souhaitee'] ?? detail['date_souhaitee'] ?? '');
        this.salaire = String(detail['salaire_base_final'] ?? detail['salaire_base_souhaite'] ?? detail['budget_salaire_base'] ?? '');
        this.typeContrat = detail['type_contrat_final'] === 'CDI' ? 'CDI' : 'CDI_PERIODE_ESSAI';
        this.commentaire = '';
      }, error: () => undefined
    });
  }

  close(): void { this.selected = null; this.proposing = false; }

  answer(action: string): void {
    if (!this.selected || this.busy) return;
    const body: Record<string, unknown> = { action };
    if (action === 'SOUMETTRE_QUESTIONNAIRE') {
      if (!this.reglesAcceptees || !this.dateNaissance || !this.dateDebut || Number(this.salaire) <= 0) {
        this.notifications.error('Questionnaire incomplet', 'Confirmez les règles et renseignez votre date de naissance, votre début souhaité et votre salaire de base.');
        return;
      }
      body['reglesAcceptees'] = this.reglesAcceptees;
      body['dateNaissance'] = this.dateNaissance;
      body['dateDebut'] = this.dateDebut;
      body['salaireBase'] = Number(this.salaire);
      body['typeContrat'] = this.typeContrat;
    }
    if (action === 'PROPOSER_MODIFICATION') {
      if (!this.dateDebut || Number(this.salaire) <= 0) {
        this.notifications.error('Proposition incomplète', 'Renseignez un salaire de base et une date de début pour votre contre-proposition.');
        return;
      }
      body['dateDebut'] = this.dateDebut;
      body['salaireBase'] = Number(this.salaire);
      body['typeContrat'] = this.typeContrat;
      body['commentaire'] = this.commentaire;
    }
    this.busy = true;
    this.applications.candidateResponse(Number(this.selected['id']), body).pipe(finalize(() => this.busy = false)).subscribe({
      next: (result) => {
        if (result.statut === 'EMBAUCHEE') {
          this.notifications.success('Contrat accepté', 'Votre embauche est enregistrée. Vous disposez maintenant de votre espace employé.');
          this.auth.refreshCurrentSession().subscribe({
            next: () => { this.load(); this.open({ id: result.id }); },
            error: () => { this.load(); this.open({ id: result.id }); }
          });
        } else {
          const messages: Record<string, string> = {
            ENTRETIEN_ACCEPTE: 'Votre présence à l’entretien est confirmée.', ENTRETIEN_REFUSE: 'Votre réponse a été transmise aux RH.',
            QUESTIONNAIRE_REMPLI: 'Votre questionnaire a été envoyé aux RH.',
            CANDIDAT_PROPOSE_MODIFICATION: 'Votre proposition a été envoyée aux RH.',
            CONTRAT_REFUSE: 'Vous avez refusé la proposition de contrat.'
          };
          this.notifications.success('Réponse envoyée', messages[result.statut] ?? 'Votre réponse a été enregistrée.');
          this.load();
          this.open({ id: result.id });
        }
      }, error: () => undefined
    });
  }

  printContract(): void { window.print(); }

  private load(): void {
    this.loading = true;
    this.applications.myApplications().pipe(finalize(() => this.loading = false)).subscribe({
      next: (items) => this.applicationsList = items,
      error: () => this.applicationsList = []
    });
  }
}
