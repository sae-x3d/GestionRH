import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { RecruitmentRequestDetail, RecruitmentRequestSummary } from '../../services/manager-recruitment.service';
import { RecruitmentWorkflowService } from '../../services/recruitment-workflow.service';

@Component({
  selector: 'app-administration-offer-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, ButtonModule, DialogModule],
  templateUrl: './administration-offer-page.component.html',
  styleUrl: './administration-offer-page.component.scss'
})
export class AdministrationOfferPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly workflow = inject(RecruitmentWorkflowService);
  private readonly notifications = inject(NotificationService);
  requests: RecruitmentRequestSummary[] = [];
  selected: RecruitmentRequestDetail | null = null;
  loading = true;
  detailLoading = false;
  dialogVisible = false;
  saving = false;

  readonly form = this.formBuilder.nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(10000)]],
    competences: ['', Validators.maxLength(5000)],
    dateCloture: ['', Validators.required]
  });

  get minClosingDate(): string {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return this.formatDate(tomorrow);
  }
  get maxClosingDate(): string | null {
    if (!this.selected?.dateSouhaitee) return null;
    const [year, month, day] = this.selected.dateSouhaitee.split('-').map(Number);
    return this.formatDate(new Date(year, month - 1, day - 1));
  }
  get closingDateRangeAvailable(): boolean {
    return !!this.maxClosingDate && this.maxClosingDate >= this.minClosingDate;
  }

  ngOnInit(): void { this.load(); }

  open(request: RecruitmentRequestSummary): void {
    this.dialogVisible = true;
    this.detailLoading = true;
    this.selected = null;
    this.workflow.detail('administration', request.id).subscribe({
      next: (detail) => {
        this.selected = detail;
        this.form.reset({ titre: '', description: '', competences: '', dateCloture: '' });
        this.detailLoading = false;
      },
      error: () => this.detailLoading = false
    });
  }

  createOffer(): void {
    if (!this.selected || this.saving) return;
    const dateCloture = this.form.controls.dateCloture.value;
    if (!this.selected.dateSouhaitee) {
      this.notifications.error('Date souhaitée manquante', 'La demande doit préciser une date souhaitée avant la création de son offre.');
      return;
    }
    if (dateCloture && (dateCloture <= this.todayDate() || dateCloture >= this.selected.dateSouhaitee)) {
      this.notifications.error('Date limite invalide', 'Elle doit être après la création de l’offre et avant la date souhaitée de recrutement.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notifications.error('Annonce incomplète', 'Renseignez le grand titre, le texte complet et la date limite de candidature.');
      return;
    }
    this.saving = true;
    this.workflow.createOffer(this.selected.id, this.form.getRawValue()).subscribe({
      next: (offer) => {
        this.saving = false;
        this.dialogVisible = false;
        this.notifications.success('Offre publiée', `L’offre « ${offer.titre} » est maintenant visible sur la page des offres.`);
        this.load();
      },
      error: () => this.saving = false
    });
  }

  private load(): void {
    this.loading = true;
    this.workflow.pending('administration').subscribe({
      next: (requests) => { this.requests = requests; this.loading = false; },
      error: () => this.loading = false
    });
  }

  private todayDate(): string { return this.formatDate(new Date()); }
  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
