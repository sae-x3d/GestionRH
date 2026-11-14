import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PublicOffer, PublicOffersService } from '../../services/public-offers.service';
import { CandidateApplicationOptions, CandidateApplicationService } from '../../services/candidate-application.service';

interface CandidatePathDraft {
  typeCode: string;
  niveauEtudeCode: string;
  intitule: string;
  organisme: string;
  description: string;
  dateDebut: string;
  dateFin: string;
  enCours: boolean;
}

@Component({
  selector: 'app-candidate-application-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './candidate-application-page.component.html',
  styleUrl: './candidate-application-page.component.scss'
})
export class CandidateApplicationPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly offersService = inject(PublicOffersService);
  private readonly applications = inject(CandidateApplicationService);
  private readonly notifications = inject(NotificationService);

  offer: PublicOffer | null = null;
  options: CandidateApplicationOptions | null = null;
  paths: CandidatePathDraft[] = [this.emptyPath()];
  cvFile: File | null = null;
  photoFile: File | null = null;
  photoPreview: string | null = null;
  loading = true;
  sending = false;
  step: 'form' | 'review' | 'sent' = 'form';
  applicationId: number | null = null;

  ngOnInit(): void {
    const offerId = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(offerId) || offerId < 1) { this.loading = false; return; }
    this.offersService.get(offerId).subscribe({
      next: (offer) => { this.offer = offer; this.loading = false; },
      error: () => { this.offer = null; this.loading = false; }
    });
    this.applications.options().subscribe({ next: (options) => this.options = options, error: () => this.options = null });
  }

  ngOnDestroy(): void { this.clearPhotoPreview(); }

  addPath(): void {
    if (this.paths.length >= 30) {
      this.notifications.error('Limite atteinte', 'Vous pouvez détailler jusqu’à 30 étapes de parcours.');
      return;
    }
    this.paths = [...this.paths, this.emptyPath()];
  }

  removePath(index: number): void {
    if (this.paths.length === 1) {
      this.notifications.error('Parcours obligatoire', 'Ajoutez au moins une étude, une formation ou une expérience.');
      return;
    }
    this.paths = this.paths.filter((_, current) => current !== index);
  }

  changeType(path: CandidatePathDraft): void {
    if (path.typeCode !== 'ETUDE') path.niveauEtudeCode = '';
  }

  typeLabel(code: string): string {
    return this.options?.typesParcours.find((option) => option.code === code)?.libelle ?? code;
  }

  levelLabel(code: string): string {
    return this.options?.niveauxEtude.find((option) => option.code === code)?.libelle ?? code;
  }

  selectCv(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.pdf') || file.size > 10 * 1024 * 1024) {
      (event.target as HTMLInputElement).value = '';
      this.notifications.error('CV invalide', 'Choisissez un fichier PDF de 10 Mo maximum.');
      return;
    }
    this.cvFile = file;
  }

  selectPhoto(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.png') || file.size > 4 * 1024 * 1024) {
      (event.target as HTMLInputElement).value = '';
      this.notifications.error('Photo invalide', 'Choisissez une image PNG de 4 Mo maximum.');
      return;
    }
    this.clearPhotoPreview();
    this.photoFile = file;
    this.photoPreview = URL.createObjectURL(file);
  }

  review(): void {
    if (!this.offer || !this.cvFile) {
      this.notifications.error('CV obligatoire', 'Ajoutez votre CV au format PDF pour continuer.');
      return;
    }
    for (let index = 0; index < this.paths.length; index++) {
      const item = this.paths[index];
      if (!item.typeCode || !item.intitule.trim() || !item.organisme.trim() || !item.dateDebut) {
        this.notifications.error('Parcours incomplet', `Complétez le type, le titre, l’organisme et la date de début de l’étape ${index + 1}.`);
        return;
      }
      if (item.typeCode === 'ETUDE' && !item.niveauEtudeCode) {
        this.notifications.error('Niveau d’étude obligatoire', `Choisissez le niveau d’étude pour l’étape ${index + 1}.`);
        return;
      }
      if (!item.enCours && !item.dateFin) {
        this.notifications.error('Durée incomplète', `Indiquez une date de fin ou cochez « En cours » pour l’étape ${index + 1}.`);
        return;
      }
      if (item.dateFin && item.dateFin < item.dateDebut) {
        this.notifications.error('Dates incohérentes', `La date de fin de l’étape ${index + 1} précède sa date de début.`);
        return;
      }
    }
    this.step = 'review';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  submit(): void {
    if (!this.offer || !this.cvFile || this.sending || this.step !== 'review') return;
    const form = new FormData();
    form.append('application', new Blob([JSON.stringify({
      offreId: this.offer.id,
      parcours: this.paths.map((item) => ({
        ...item,
        niveauEtudeCode: item.typeCode === 'ETUDE' ? item.niveauEtudeCode : null,
        dateFin: item.enCours ? null : item.dateFin
      }))
    })], { type: 'application/json' }));
    form.append('cv', this.cvFile, this.cvFile.name);
    if (this.photoFile) form.append('photo', this.photoFile, this.photoFile.name);
    this.sending = true;
    this.applications.submit(form).pipe(finalize(() => this.sending = false)).subscribe({
      next: (result) => {
        this.applicationId = result.id;
        this.step = 'sent';
        this.notifications.success('Candidature envoyée', 'Votre dossier a été transmis aux ressources humaines.');
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: () => undefined
    });
  }

  private emptyPath(): CandidatePathDraft {
    return { typeCode: '', niveauEtudeCode: '', intitule: '', organisme: '', description: '', dateDebut: '', dateFin: '', enCours: false };
  }

  private clearPhotoPreview(): void {
    if (this.photoPreview) URL.revokeObjectURL(this.photoPreview);
    this.photoPreview = null;
  }
}
