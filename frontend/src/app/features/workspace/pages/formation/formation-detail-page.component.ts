import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import {
  EligibleNeed,
  FormationDetail,
  FormationParticipant,
  FormationService
} from '../../services/formation.service';

@Component({
  selector: 'app-formation-detail-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, ButtonModule, InputTextModule],
  templateUrl: './formation-detail-page.component.html',
  styleUrl: './formation-detail-page.component.scss'
})
export class FormationDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(FormationService);
  private readonly notifications = inject(NotificationService);
  private readonly formationId = Number(this.route.snapshot.paramMap.get('formationId'));
  detail: FormationDetail | null = null;
  loading = true;
  actionId: number | null = null;
  measureTarget: FormationParticipant | null = null;
  measureForm = { niveauAvant: 0, niveauApres: 0, commentaire: '' };
  measuring = false;

  ngOnInit(): void { this.load(); }

  back(): void { void this.router.navigate(['/espace/rh/formations']); }

  goEvolution(): void { void this.router.navigate(['/espace/rh/formations/evolution']); }

  enroll(need: EligibleNeed): void {
    if (this.actionId !== null) return;
    this.actionId = need.id;
    this.service.enroll(this.formationId, need.id).subscribe({
      next: (detail) => {
        this.actionId = null;
        this.detail = detail;
        this.notifications.success('Participant inscrit', detail.message ?? 'Le besoin est maintenant planifie.');
      },
      error: () => { this.actionId = null; }
    });
  }

  openMeasure(participant: FormationParticipant): void {
    this.measureTarget = participant;
    const before = participant.niveauAvant ?? participant.niveauAcquisInitial;
    this.measureForm = {
      niveauAvant: before,
      niveauApres: participant.niveauApres ?? before,
      commentaire: participant.commentaire ?? ''
    };
  }

  cancelMeasure(): void { this.measureTarget = null; }

  submitMeasure(): void {
    if (!this.measureTarget || this.measuring) return;
    if (this.measureForm.niveauApres < this.measureForm.niveauAvant) {
      this.notifications.error('Niveaux incoherents', 'Le niveau apres formation doit etre superieur ou egal au niveau avant.');
      return;
    }
    this.measuring = true;
    this.service.measure(this.formationId, {
      participationId: this.measureTarget.participationId,
      niveauAvant: Number(this.measureForm.niveauAvant),
      niveauApres: Number(this.measureForm.niveauApres),
      commentaire: this.measureForm.commentaire.trim()
    }).subscribe({
      next: (detail) => {
        this.measuring = false;
        this.measureTarget = null;
        this.detail = detail;
        this.notifications.success('Mesure enregistree', detail.message ?? 'Le pourcentage acquis a ete calcule.');
      },
      error: () => { this.measuring = false; }
    });
  }

  private load(): void {
    this.loading = true;
    this.service.detail(this.formationId).subscribe({
      next: (detail) => { this.detail = detail; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
