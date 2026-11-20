import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { CriterePoste } from '../../services/competence-management.service';
import { EvaluationForm, EvaluationService } from '../../services/evaluation.service';

@Component({
  selector: 'app-evaluation-form-page',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputTextModule],
  templateUrl: './evaluation-form-page.component.html',
  styleUrl: './evaluation-form-page.component.scss'
})
export class EvaluationFormPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(EvaluationService);
  private readonly notifications = inject(NotificationService);
  private readonly employeeId = Number(this.route.snapshot.paramMap.get('employeId'));
  form: EvaluationForm | null = null;
  notes: Record<number, number> = {};
  dateEvaluation = '';
  commentaire = '';
  loading = true;
  saving = false;

  ngOnInit(): void { this.load(); }

  gap(critere: CriterePoste): number {
    const acquired = this.notes[critere.id];
    return Math.max(0, critere.niveauAttendu - (acquired ?? 0));
  }

  submit(): void {
    if (this.saving || !this.form) return;
    if (!this.dateEvaluation) {
      this.notifications.error('Date requise', 'Choisissez la date de l evaluation.');
      return;
    }
    for (const critere of this.form.criteres) {
      const note = this.notes[critere.id];
      if (note == null || note < this.form.employe.niveauMin || note > this.form.employe.niveauMax) {
        this.notifications.error('Notes incompletes', 'Renseignez toutes les notes dans le bareme du poste.');
        return;
      }
    }
    this.saving = true;
    this.service.create({
      employeId: this.employeeId,
      dateEvaluation: this.dateEvaluation,
      commentaire: this.commentaire,
      notes: this.notes
    }).subscribe({
      next: (result) => {
        this.saving = false;
        this.notifications.success('Evaluation soumise', result.message ?? 'Les RH vont maintenant la valider.');
        void this.router.navigate(['/espace/manager/equipe/evaluations', result.evaluation.id]);
      },
      error: () => { this.saving = false; }
    });
  }

  cancel(): void {
    void this.router.navigate(['/espace/manager/equipe/evaluations']);
  }

  private load(): void {
    this.loading = true;
    this.service.managerForm(this.employeeId).subscribe({
      next: (form) => {
        this.form = form;
        this.dateEvaluation = form.dateEvaluation;
        for (const critere of form.criteres) {
          this.notes[critere.id] = critere.niveauAttendu;
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }
}
