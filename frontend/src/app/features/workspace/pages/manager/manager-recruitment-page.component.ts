import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import {
  ManagerPost, ManagerRecruitmentOptions, ManagerRecruitmentService,
  RecruitmentRequestDetail, RecruitmentRequestSummary
} from '../../services/manager-recruitment.service';

@Component({
  selector: 'app-manager-recruitment-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule],
  templateUrl: './manager-recruitment-page.component.html',
  styleUrl: './manager-recruitment-page.component.scss'
})
export class ManagerRecruitmentPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly recruitment = inject(ManagerRecruitmentService);
  private readonly notifications = inject(NotificationService);

  options: ManagerRecruitmentOptions | null = null;
  requests: RecruitmentRequestSummary[] = [];
  selectedRequest: RecruitmentRequestDetail | null = null;
  loading = true;
  requestsLoading = true;
  submitting = false;
  detailLoading = false;
  cancelling = false;
  cancelConfirmation = false;
  detailVisible = false;

  readonly form = this.formBuilder.group({
    posteId: [null as number | null, Validators.required],
    motifId: [null as number | null, Validators.required],
    effectif: [1, [Validators.required, Validators.min(1)]],
    budget: [null as number | null, [Validators.required, Validators.min(0.01)]],
    justification: ['', [Validators.required, Validators.maxLength(5000)]],
    dateSouhaitee: ['', Validators.required]
  });

  ngOnInit(): void {
    this.loadOptions();
    this.loadRequests();
  }

  get selectedPost(): ManagerPost | undefined {
    const postId = this.form.controls.posteId.value;
    return this.options?.postes.find((post) => post.id === postId);
  }

  get selectedReasonCode(): string | undefined {
    const reasonId = this.form.controls.motifId.value;
    return this.options?.motifs.find((reason) => reason.id === reasonId)?.code;
  }

  get minDesiredDate(): string {
    const minDate = new Date();
    minDate.setDate(minDate.getDate() + 2);
    return `${minDate.getFullYear()}-${String(minDate.getMonth() + 1).padStart(2, '0')}-${String(minDate.getDate()).padStart(2, '0')}`;
  }

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid || !this.selectedPost) {
      this.form.markAllAsTouched();
      this.notifications.error('Formulaire incomplet', 'Renseignez le poste, le motif, l’effectif, le budget, la date souhaitée et la justification.');
      return;
    }

    const value = this.form.getRawValue();
    const post = this.selectedPost;
    if (value.posteId === null || value.motifId === null || value.effectif === null || value.budget === null) return;
    if (post.salaireBaseMin <= 0 || post.salaireBaseMax < post.salaireBaseMin) {
      this.notifications.error('Fourchette de salaire de base indisponible', 'Le poste sélectionné n’a pas de fourchette de salaire de base valide. Contactez les RH.');
      return;
    }
    if (value.budget < post.salaireBaseMin || value.budget > post.salaireBaseMax) {
      this.notifications.error('Budget hors fourchette', `Le budget de salaire de base par personne doit être compris entre ${post.salaireBaseMin} et ${post.salaireBaseMax}.`);
      return;
    }

    if (this.selectedReasonCode === 'POSTE_VACANT' && value.effectif > post.postesVacantsDisponibles) {
      this.notifications.error('Postes vacants insuffisants', `Ce poste ne compte que ${post.postesVacantsDisponibles} place(s) disponible(s) au regard de la cible autorisée. Choisissez le motif « Augmentation de l’effectif » pour demander des postes supplémentaires.`);
      return;
    }

    this.submitting = true;
    this.recruitment.submitRequest({
      posteId: value.posteId,
      motifId: value.motifId,
      effectif: value.effectif,
      budgetSalaireBase: value.budget,
      justification: value.justification?.trim() ?? '',
      dateSouhaitee: value.dateSouhaitee || null
    }).subscribe({
      next: (request) => {
        this.submitting = false;
        this.notifications.success('Demande envoyée', `La demande n°${request.id} a été enregistrée. Statut : ${request.statut}.`);
        this.form.reset({ posteId: null, motifId: null, effectif: 1, budget: null, justification: '', dateSouhaitee: '' });
        this.loadRequests();
      },
      error: () => this.submitting = false
    });
  }

  openDetails(request: RecruitmentRequestSummary): void {
    this.detailVisible = true;
    this.detailLoading = true;
    this.selectedRequest = null;
    this.cancelConfirmation = false;
    this.recruitment.getRequest(request.id).subscribe({
      next: (detail) => {
        this.selectedRequest = detail;
        this.detailLoading = false;
      },
      error: () => this.detailLoading = false
    });
  }

  cancelRequest(): void {
    const request = this.selectedRequest;
    if (!request?.annulable || this.cancelling) return;
    this.cancelling = true;
    this.recruitment.cancelRequest(request.id).subscribe({
      next: () => {
        this.cancelling = false;
        this.cancelConfirmation = false;
        this.notifications.success('Demande annulée', `La demande n°${request.id} a été annulée.`);
        this.detailVisible = false;
        this.selectedRequest = null;
        this.loadRequests();
      },
      error: () => this.cancelling = false
    });
  }

  closeDetails(): void {
    this.detailVisible = false;
    this.cancelConfirmation = false;
  }

  private loadOptions(): void {
    this.loading = true;
    this.recruitment.getOptions().subscribe({
      next: (options) => {
        this.options = options;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  private loadRequests(): void {
    this.requestsLoading = true;
    this.recruitment.getRequests().subscribe({
      next: (requests) => {
        this.requests = requests;
        this.requestsLoading = false;
      },
      error: () => this.requestsLoading = false
    });
  }
}
