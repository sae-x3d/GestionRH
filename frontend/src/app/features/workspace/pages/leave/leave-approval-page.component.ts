import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { LeaveManagementService, LeaveRequestRow } from '../../services/leave-management.service';

@Component({
  selector: 'app-leave-approval-page', standalone: true,
  imports: [DatePipe, FormsModule, ButtonModule, InputTextModule],
  templateUrl: './leave-approval-page.component.html', styleUrl: './leave-approval-page.component.scss'
})
export class LeaveApprovalPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(LeaveManagementService);
  private readonly notifications = inject(NotificationService);
  readonly role = this.route.snapshot.data['leaveRole'] as 'manager' | 'rh';
  requests: LeaveRequestRow[] = [];
  comments: Record<number, string> = {};
  loading = true;
  actionId: number | null = null;

  ngOnInit(): void { this.load(); }

  decide(request: LeaveRequestRow, decision: 'APPROUVEE' | 'REFUSEE'): void {
    if (this.actionId !== null) return;
    const comment = (this.comments[request.id] ?? '').trim();
    if (decision === 'REFUSEE' && !comment) { this.notifications.error('Motif requis', 'Indiquez la raison du refus avant de poursuivre.'); return; }
    this.actionId = request.id;
    this.service.decideLeave(this.role, request.id, decision, comment).subscribe({
      next: (result) => {
        this.actionId = null;
        this.notifications.success('Décision enregistrée', result.message);
        if (result.alerteEffectif) this.notifications.error('Sous-effectif prévisionnel', result.alerteEffectif);
        this.load();
      },
      error: () => this.actionId = null
    });
  }

  private load(): void {
    this.loading = true;
    this.service.getApprovalQueue(this.role).subscribe({ next: (rows) => { this.requests = rows; this.loading = false; }, error: () => this.loading = false });
  }
}
