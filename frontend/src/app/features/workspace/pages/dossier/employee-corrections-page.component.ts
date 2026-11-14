import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { EmployeeDossierService } from '../../services/employee-dossier.service';

@Component({ selector: 'app-employee-corrections', standalone: true, imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './employee-corrections-page.component.html', styleUrl: './employee-corrections-page.component.scss' })
export class EmployeeCorrectionsPageComponent implements OnInit {
  private readonly service = inject(EmployeeDossierService); private readonly toast = inject(NotificationService);
  items: Record<string, any>[] = []; comments: Record<number, string> = {}; statusFilter = ''; loading = true; busyId: number | null = null;
  get filtered(): Record<string, any>[] { return this.items.filter((item) => !this.statusFilter || item['statutCode'] === this.statusFilter); }
  ngOnInit(): void { this.load(); }
  decide(item: Record<string, any>, decision: 'APPROUVEE' | 'REFUSEE'): void {
    const id = Number(item['id']); const comment = this.comments[id] ?? '';
    if (this.busyId !== null) return;
    if (decision === 'REFUSEE' && !comment.trim()) { this.toast.error('Motif requis', 'Expliquez le refus afin que le salarié puisse corriger sa demande.'); return; }
    this.busyId = id;
    this.service.decideCorrection(id, decision, comment).pipe(finalize(() => this.busyId = null)).subscribe({
      next: () => { this.toast.success(decision === 'APPROUVEE' ? 'Correction approuvée' : 'Demande refusée', 'Le salarié a été notifié de la décision.'); this.load(); },
      error: () => this.toast.error('Décision impossible', 'Cette demande a déjà été traitée ou le dossier a changé depuis sa soumission.')
    });
  }
  downloadAttachment(item: Record<string, any>): void {
    this.service.downloadCorrectionAttachment(Number(item['id'])).subscribe({
      next: (blob) => { const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = String(item['pieceJointe'] ?? 'justificatif'); link.click(); URL.revokeObjectURL(url); },
      error: () => this.toast.error('Pièce jointe indisponible', 'Le fichier est absent ou vous ne disposez pas des droits nécessaires.')
    });
  }
  private load(): void { this.loading = true; this.service.corrections(true).pipe(finalize(() => this.loading = false)).subscribe({
    next: (items) => this.items = items, error: () => { this.items = []; this.toast.error('Demandes indisponibles', 'Impossible de charger les demandes de correction.'); }
  }); }
}
