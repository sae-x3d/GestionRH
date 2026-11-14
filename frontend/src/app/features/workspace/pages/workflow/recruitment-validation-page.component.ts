import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { RecruitmentRequestDetail, RecruitmentRequestSummary } from '../../services/manager-recruitment.service';
import { RecruitmentDecision, RecruitmentWorkflowService, WorkflowArea } from '../../services/recruitment-workflow.service';

@Component({
  selector: 'app-recruitment-validation-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, ButtonModule, DialogModule],
  templateUrl: './recruitment-validation-page.component.html',
  styleUrl: './recruitment-validation-page.component.scss'
})
export class RecruitmentValidationPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly workflow = inject(RecruitmentWorkflowService);
  private readonly notifications = inject(NotificationService);
  readonly area = this.route.snapshot.data['workflowArea'] as Exclude<WorkflowArea, 'administration'>;
  readonly isFinance = this.area === 'finance';
  requests: RecruitmentRequestSummary[] = [];
  selected: RecruitmentRequestDetail | null = null;
  loading = true;
  detailLoading = false;
  detailVisible = false;
  busy = false;
  rejectMode = false;
  comment = '';

  ngOnInit(): void { this.load(); }

  open(request: RecruitmentRequestSummary): void {
    this.detailVisible = true;
    this.detailLoading = true;
    this.selected = null;
    this.rejectMode = false;
    this.comment = '';
    this.workflow.detail(this.area, request.id).subscribe({
      next: (detail) => { this.selected = detail; this.detailLoading = false; },
      error: () => this.detailLoading = false
    });
  }

  approve(): void { this.decide('APPROUVEE'); }

  beginReject(): void { this.rejectMode = true; }

  reject(): void {
    if (!this.comment.trim()) {
      this.notifications.error('Commentaire obligatoire', 'Précisez le motif du refus pour informer le manager.');
      return;
    }
    this.decide('REFUSEE');
  }

  private decide(decision: RecruitmentDecision): void {
    if (!this.selected || this.busy) return;
    const request = this.selected;
    this.busy = true;
    this.workflow.decide(this.area, request.id, decision, this.comment.trim()).subscribe({
      next: (updated) => {
        this.busy = false;
        this.notifications.success(decision === 'APPROUVEE' ? 'Demande validée' : 'Demande refusée',
          decision === 'APPROUVEE' ? `La demande n°${request.id} passe à l’étape suivante.` : `Le manager a été informé du refus de la demande n°${request.id}.`);
        this.detailVisible = false;
        this.selected = updated;
        this.rejectMode = false;
        this.load();
      },
      error: () => this.busy = false
    });
  }

  private load(): void {
    this.loading = true;
    this.workflow.pending(this.area).subscribe({
      next: (requests) => { this.requests = requests; this.loading = false; },
      error: () => this.loading = false
    });
  }
}
