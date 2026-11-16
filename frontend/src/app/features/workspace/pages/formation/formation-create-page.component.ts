import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { Competence, CompetenceManagementService } from '../../services/competence-management.service';
import { FormationService } from '../../services/formation.service';

@Component({
  selector: 'app-formation-create-page',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputTextModule],
  templateUrl: './formation-create-page.component.html',
  styleUrl: './formation-create-page.component.scss'
})
export class FormationCreatePageComponent implements OnInit {
  private readonly service = inject(FormationService);
  private readonly competencesService = inject(CompetenceManagementService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  competences: Competence[] = [];
  loading = true;
  saving = false;
  form = {
    competenceId: null as number | null,
    titre: '',
    description: '',
    organisme: '',
    dateDebut: this.today(),
    dateFin: '',
    niveauCible: null as number | null
  };

  ngOnInit(): void {
    this.competencesService.list().subscribe({
      next: (competences) => {
        this.competences = competences.filter((competence) => competence.actif);
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  submit(): void {
    if (this.saving) return;
    if (!this.form.competenceId || !this.form.titre.trim()) {
      this.notifications.error('Formation incomplete', 'Choisissez une competence et saisissez un titre.');
      return;
    }
    if (this.form.dateDebut && this.form.dateFin && this.form.dateFin < this.form.dateDebut) {
      this.notifications.error('Dates incoherentes', 'La date de fin doit suivre la date de debut.');
      return;
    }
    this.saving = true;
    this.service.create({
      competenceId: this.form.competenceId,
      titre: this.form.titre.trim(),
      description: this.form.description.trim(),
      organisme: this.form.organisme.trim(),
      dateDebut: this.form.dateDebut || null,
      dateFin: this.form.dateFin || null,
      niveauCible: this.form.niveauCible == null ? null : Number(this.form.niveauCible)
    }).subscribe({
      next: (result) => {
        this.saving = false;
        this.notifications.success('Formation creee', result.message ?? 'La formation est prete.');
        void this.router.navigate(['/espace/rh/formations', result.formation.id]);
      },
      error: () => { this.saving = false; }
    });
  }

  cancel(): void {
    void this.router.navigate(['/espace/rh/formations']);
  }

  private today(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }
}
