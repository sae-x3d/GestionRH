import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { EvaluationDetail, EvaluationService } from '../../services/evaluation.service';

@Component({
  selector: 'app-evaluation-detail-page',
  standalone: true,
  imports: [DatePipe, ButtonModule],
  templateUrl: './evaluation-detail-page.component.html',
  styleUrl: './evaluation-detail-page.component.scss'
})
export class EvaluationDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(EvaluationService);
  private readonly notifications = inject(NotificationService);
  readonly role = this.route.snapshot.data['evaluationRole'] as 'manager' | 'rh' | 'employee';
  private readonly evaluationId = Number(this.route.snapshot.paramMap.get('evaluationId'));
  detail: EvaluationDetail | null = null;
  loading = true;
  validating = false;

  ngOnInit(): void { this.load(); }

  back(): void {
    const base = this.role === 'manager' ? '/espace/manager/equipe/evaluations' : this.role === 'rh' ? '/espace/rh/evaluations' : '/espace/employe/evaluations';
    void this.router.navigate([base]);
  }

  validate(): void {
    if (this.validating || !this.detail) return;
    this.validating = true;
    this.service.validate(this.evaluationId).subscribe({
      next: (detail) => {
        this.detail = detail;
        this.validating = false;
        this.notifications.success('Evaluation validee', detail.message ?? 'Les besoins de formation ont ete crees.');
      },
      error: () => { this.validating = false; }
    });
  }

  private load(): void {
    this.loading = true;
    const request = this.role === 'manager'
      ? this.service.managerDetail(this.evaluationId)
      : this.role === 'rh'
        ? this.service.rhDetail(this.evaluationId)
        : this.service.employeeDetail(this.evaluationId);
    request.subscribe({
      next: (detail) => { this.detail = detail; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
