import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { DisciplineService, EmployeeDisciplineProcedure } from '../../services/discipline.service';

@Component({
  selector: 'app-employee-discipline-page', standalone: true,
  imports: [DatePipe, FormsModule, ButtonModule, DialogModule, InputTextModule],
  templateUrl: './employee-discipline-page.component.html', styleUrl: './employee-discipline-page.component.scss'
})
export class EmployeeDisciplinePageComponent implements OnInit {
  private readonly service = inject(DisciplineService);
  private readonly notifications = inject(NotificationService);

  procedures: EmployeeDisciplineProcedure[] = [];
  loading = true;

  answering: EmployeeDisciplineProcedure | null = null;
  answerText = '';
  answerFiles: File[] = [];
  respondVisible = false;
  submitting = false;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.service.employeeProcedures().subscribe({
      next: (rows) => { this.procedures = rows; this.loading = false; },
      error: () => this.loading = false
    });
  }

  canRespond(procedure: EmployeeDisciplineProcedure): boolean {
    return procedure.demandeId != null && !procedure.reponseId && procedure.statutCode === 'EXPLICATION_DEMANDEE';
  }

  pendingAnswers(): number {
    return this.procedures.filter((procedure) => this.canRespond(procedure)).length;
  }

  openRespond(procedure: EmployeeDisciplineProcedure): void {
    this.answering = procedure;
    this.answerText = '';
    this.answerFiles = [];
    this.respondVisible = true;
  }

  onFiles(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.answerFiles = input.files ? Array.from(input.files) : [];
  }

  submitAnswer(): void {
    if (!this.answering || this.submitting) return;
    if (this.answering.demandeId == null) {
      this.notifications.error('Réponse impossible', 'Aucune demande d’explication active pour cette procédure.');
      this.respondVisible = false;
      this.answering = null;
      this.load();
      return;
    }
    const texte = this.answerText.trim();
    if (!texte) { this.notifications.error('Réponse requise', 'Rédigez votre explication avant de l’envoyer.'); return; }
    this.submitting = true;
    this.service.respond(this.answering.demandeId, texte, this.answerFiles).subscribe({
      next: (result) => {
        this.submitting = false;
        this.respondVisible = false;
        this.answering = null;
        this.notifications.success('Réponse envoyée', `Votre réponse a été transmise${result.pieces ? ` avec ${result.pieces} pièce(s)` : ''}.`);
        this.load();
      },
      error: () => this.submitting = false
    });
  }

  pieceUrl(pieceId: number): string { return this.service.downloadEmployeePiece(pieceId); }
}
