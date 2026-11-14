import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { CandidateApplicationService, ReceivedCandidature, RhOfferCandidates } from '../../../public/services/candidate-application.service';

@Component({
  selector: 'app-rh-candidatures-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './rh-candidatures-page.component.html',
  styleUrl: './rh-candidatures-page.component.scss'
})
export class RhCandidaturesPageComponent implements OnInit {
  private readonly applications = inject(CandidateApplicationService);
  private readonly notifications = inject(NotificationService);
  offers: RhOfferCandidates[] = [];
  candidatures: ReceivedCandidature[] = [];
  loading = true;
  busy = false;
  downloadingId: number | null = null;
  selectedOffer: RhOfferCandidates | null = null;
  selectedStatus = '';
  candidate: Record<string, any> | null = null;
  entretienAt = '';
  typeEntretien = 'ENTRETIEN';
  note = '';
  rejectionReason = '';
  salaire = '';
  dateDebut = '';
  typeContrat = 'CDI_PERIODE_ESSAI';

  ngOnInit(): void { this.load(); }

  get offerCandidatures(): ReceivedCandidature[] {
    return this.candidatures.filter((item) => item.offreId === this.selectedOffer?.id);
  }

  get filteredCandidatures(): ReceivedCandidature[] {
    return this.offerCandidatures.filter((item) => !this.selectedStatus || item.statutCode === this.selectedStatus);
  }

  get statuses(): { code: string; libelle: string; count: number }[] {
    const groups = new Map<string, { code: string; libelle: string; count: number }>();
    for (const item of this.offerCandidatures) {
      const group = groups.get(item.statutCode) ?? { code: item.statutCode, libelle: item.statut, count: 0 };
      group.count++;
      groups.set(item.statutCode, group);
    }
    return [...groups.values()];
  }

  canReject(statusCode: string): boolean {
    return !['REFUSEE', 'CONTRAT_REFUSE', 'EMBAUCHEE'].includes(statusCode);
  }

  openOffer(offer: RhOfferCandidates): void { this.selectedOffer = offer; this.selectedStatus = ''; }
  closeOffer(): void { this.selectedOffer = null; this.selectedStatus = ''; }
  closeCandidate(): void { this.candidate = null; this.note = ''; }

  openCandidate(item: ReceivedCandidature): void {
    this.rejectionReason = '';
    this.note = '';
    this.applications.rhDetails(item.id).subscribe({
      next: (detail) => {
        this.candidate = detail;
        const date = new Date(Date.now() + 86400000);
        date.setMinutes(0, 0, 0);
        this.entretienAt = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}T09:00`;
        this.salaire = String(detail['salaire_base_propose_candidat'] ?? detail['salaire_base_souhaite'] ?? detail['budget_salaire_base'] ?? '');
        this.dateDebut = String(detail['date_debut_proposee_candidat'] ?? detail['date_debut_souhaitee'] ?? detail['date_souhaitee'] ?? '');
        this.typeContrat = 'CDI_PERIODE_ESSAI';
      },
      error: () => undefined
    });
  }

  reloadCandidate(): void {
    const id = Number(this.candidate?.['id']);
    if (id) this.applications.rhDetails(id).subscribe({ next: (detail) => this.candidate = detail, error: () => undefined });
  }

  perform(action: string): void {
    if (!this.candidate || this.busy) return;
    const body: Record<string, unknown> = { action, commentaire: action === 'REJETER' ? this.rejectionReason : this.note };
    if (action === 'PLANIFIER_ENTRETIEN') {
      if (!this.entretienAt) { this.notifications.error('Horaire requis', 'Choisissez la date et l’heure proposées au candidat.'); return; }
      body['entretienAt'] = this.entretienAt;
      body['typeEntretien'] = this.typeEntretien;
    }
    if (['PROPOSER_CONTRAT', 'CONTRE_PROPOSER'].includes(action)) {
      body['salaireBase'] = Number(this.salaire);
      body['dateDebut'] = this.dateDebut;
      body['typeContrat'] = this.typeContrat;
    }
    if (action === 'REJETER' && !this.rejectionReason.trim()) {
      this.notifications.error('Motif obligatoire', 'Précisez pourquoi la candidature est recalée pour garder une trace claire.');
      return;
    }
    this.busy = true;
    this.applications.rhAction(Number(this.candidate['id']), body).pipe(finalize(() => this.busy = false)).subscribe({
      next: () => {
        const messages: Record<string, string> = {
          PRESELECTIONNER: 'Le candidat a été présélectionné.', REJETER: 'La candidature a été recalée.',
          PLANIFIER_ENTRETIEN: 'La proposition d’entretien a été envoyée.', MARQUER_ENTRETIEN_EFFECTUE: 'L’entretien est marqué comme effectué.',
          RETENIR_APRES_ENTRETIEN: 'Le candidat retenu peut maintenant remplir son questionnaire.',
          PROPOSER_CONTRAT: 'La proposition de contrat a été transmise.', ACCEPTER_MODIFICATION: 'La modification du candidat est acceptée.',
          CONTRE_PROPOSER: 'La contre-proposition a été envoyée.'
        };
        this.notifications.success('Étape mise à jour', messages[action] ?? 'Le dossier a été mis à jour.');
        this.note = '';
        this.rejectionReason = '';
        this.load();
        this.reloadCandidate();
      },
      error: () => undefined
    });
  }

  downloadCv(): void {
    const id = Number(this.candidate?.['id']);
    if (!id || this.downloadingId !== null) return;
    this.downloadingId = id;
    this.applications.downloadCv(id).pipe(finalize(() => this.downloadingId = null)).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url; link.download = `cv-candidature-${id}.pdf`; link.click(); URL.revokeObjectURL(url);
      }, error: () => undefined
    });
  }

  private load(): void {
    this.loading = true;
    forkJoin({ offers: this.applications.rhOffers(), candidatures: this.applications.receivedByRh() })
      .pipe(finalize(() => this.loading = false)).subscribe({
        next: (result) => { this.offers = result.offers; this.candidatures = result.candidatures; },
        error: () => { this.offers = []; this.candidatures = []; }
      });
  }
}
