import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import {
  Competence,
  CompetenceConfiguration,
  CompetenceManagementService,
  PosteOption
} from '../../services/competence-management.service';

@Component({
  selector: 'app-competence-configuration-page',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputTextModule],
  templateUrl: './competence-configuration-page.component.html',
  styleUrl: './competence-configuration-page.component.scss'
})
export class CompetenceConfigurationPageComponent implements OnInit {
  private readonly service = inject(CompetenceManagementService);
  private readonly notifications = inject(NotificationService);
  data: CompetenceConfiguration | null = null;
  loading = true;
  saving = false;
  selectedPosteId: number | null = null;
  baremeMin = 0;
  baremeMax = 5;
  newCompetence = { code: '', libelle: '', description: '' };
  criterion = {
    competenceId: null as number | null,
    niveauAttendu: 1,
    seuilEcart: 1,
    obligatoire: true,
    commentaire: ''
  };

  ngOnInit(): void { this.load(); }

  selectPoste(): void {
    if (this.selectedPosteId) this.load(this.selectedPosteId);
  }

  activeCompetences(): Competence[] {
    return (this.data?.competences ?? []).filter((competence) => competence.actif);
  }

  createCompetence(): void {
    if (this.saving) return;
    if (!this.newCompetence.code.trim() || !this.newCompetence.libelle.trim()) {
      this.notifications.error('Competence incomplete', 'Le code et le libelle sont obligatoires.');
      return;
    }
    this.saving = true;
    this.service.create({
      code: this.newCompetence.code.trim(),
      libelle: this.newCompetence.libelle.trim(),
      description: this.newCompetence.description.trim()
    }).subscribe({
      next: (result) => {
        this.saving = false;
        this.newCompetence = { code: '', libelle: '', description: '' };
        this.notifications.success('Competence creee', result.message);
        this.load(this.selectedPosteId ?? undefined);
      },
      error: () => { this.saving = false; }
    });
  }

  toggleActive(competence: Competence): void {
    if (this.saving) return;
    this.saving = true;
    this.service.setActive(competence.id, !competence.actif).subscribe({
      next: (result) => {
        this.saving = false;
        this.notifications.success('Competence mise a jour', result.message);
        this.load(this.selectedPosteId ?? undefined);
      },
      error: () => { this.saving = false; }
    });
  }

  saveCriterion(): void {
    if (this.saving || !this.data?.poste || !this.criterion.competenceId) {
      this.notifications.error('Critere incomplet', 'Choisissez un poste et une competence active.');
      return;
    }
    this.saving = true;
    this.service.saveCriterion({
      posteId: this.data.poste.id,
      competenceId: this.criterion.competenceId,
      niveauAttendu: Number(this.criterion.niveauAttendu),
      seuilEcart: Number(this.criterion.seuilEcart),
      obligatoire: this.criterion.obligatoire,
      commentaire: this.criterion.commentaire
    }).subscribe({
      next: (data) => {
        this.data = data;
        this.saving = false;
        this.criterion = { competenceId: null, niveauAttendu: 1, seuilEcart: 1, obligatoire: true, commentaire: '' };
        this.notifications.success('Critere enregistre', 'Le bareme du poste a ete mis a jour.');
      },
      error: () => { this.saving = false; }
    });
  }

  deleteCriterion(critereId: number): void {
    if (this.saving || !this.data?.poste) return;
    if (!window.confirm('Supprimer ce critere du bareme du poste ?')) return;
    this.saving = true;
    this.service.deleteCriterion(this.data.poste.id, critereId).subscribe({
      next: (data) => {
        this.data = data;
        this.saving = false;
        this.notifications.success('Critere supprime', 'Le critere a ete retire du bareme.');
      },
      error: () => { this.saving = false; }
    });
  }

  saveBareme(): void {
    if (this.saving || !this.data?.poste) return;
    this.saving = true;
    this.service.updateBareme(this.data.poste.id, Number(this.baremeMin), Number(this.baremeMax)).subscribe({
      next: (data) => {
        this.data = data;
        this.syncBareme();
        this.saving = false;
        this.notifications.success('Bareme enregistre', 'Les niveaux minimum et maximum du poste ont ete mis a jour.');
      },
      error: () => { this.saving = false; }
    });
  }

  private load(posteId?: number): void {
    this.loading = true;
    this.service.configuration(posteId).subscribe({
      next: (data) => {
        this.data = data;
        this.selectedPosteId = data.poste?.id ?? null;
        this.syncBareme();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  private syncBareme(): void {
    const poste: PosteOption | null = this.data?.poste ?? null;
    this.baremeMin = poste?.niveauMin ?? 0;
    this.baremeMax = poste?.niveauMax ?? 5;
  }
}
