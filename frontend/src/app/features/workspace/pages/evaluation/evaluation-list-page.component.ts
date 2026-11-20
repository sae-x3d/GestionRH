import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { NotificationService } from '../../../../core/notifications/notification.service';
import {
  EvaluationEmployee,
  EvaluationRow,
  EvaluationService
} from '../../services/evaluation.service';

@Component({
  selector: 'app-evaluation-list-page',
  standalone: true,
  imports: [DatePipe, ButtonModule],
  templateUrl: './evaluation-list-page.component.html',
  styleUrl: './evaluation-list-page.component.scss'
})
export class EvaluationListPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(EvaluationService);
  private readonly notifications = inject(NotificationService);
  readonly role = this.route.snapshot.data['evaluationRole'] as 'manager' | 'rh' | 'employee';
  employees: EvaluationEmployee[] = [];
  evaluations: EvaluationRow[] = [];
  loading = true;
  actionId: number | null = null;

  ngOnInit(): void { this.load(); }

  startEvaluation(employeeId: number): void {
    void this.router.navigate(['/espace/manager/equipe/evaluations/nouvelle', employeeId]);
  }

  openEvaluation(id: number): void {
    const base = this.role === 'manager' ? '/espace/manager/equipe/evaluations' : this.role === 'rh' ? '/espace/rh/evaluations' : '/espace/employe/evaluations';
    void this.router.navigate([base, id]);
  }

  validate(evaluation: EvaluationRow): void {
    if (this.actionId !== null) return;
    this.actionId = evaluation.id;
    this.service.validate(evaluation.id).subscribe({
      next: (result) => {
        this.actionId = null;
        this.notifications.success('Evaluation validee', result.message ?? 'Les besoins de formation ont ete mis a jour.');
        this.load();
      },
      error: () => { this.actionId = null; }
    });
  }

  private load(): void {
    this.loading = true;
    if (this.role === 'manager') {
      this.service.managerOverview().subscribe({
        next: (data) => {
          this.employees = data.employes;
          this.evaluations = data.evaluations;
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
      return;
    }
    const request = this.role === 'rh' ? this.service.rhList() : this.service.employeeList();
    request.subscribe({
      next: (rows) => { this.evaluations = rows; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
