import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PayrollBulletin, PayrollPayslipsService } from '../../services/payroll-payslips.service';

@Component({
  selector: 'app-payroll-bulletins-page', standalone: true, imports: [DatePipe, DecimalPipe],
  templateUrl: './payroll-bulletins-page.component.html', styleUrl: './payroll-bulletins-page.component.scss'
})
export class PayrollBulletinsPageComponent implements OnInit {
  private readonly service = inject(PayrollPayslipsService);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);
  readonly rhView = this.route.snapshot.data['payrollView'] === 'rh';
  bulletins: PayrollBulletin[] = [];
  loading = true;
  downloadingId: number | null = null;
  ngOnInit(): void { this.load(); }

  download(bulletin: PayrollBulletin): void {
    if (this.downloadingId !== null) return;
    this.downloadingId = bulletin.id;
    const request = this.rhView ? this.service.rhPdf(bulletin.id) : this.service.ownPdf(bulletin.id);
    request.subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob); const anchor = document.createElement('a');
        anchor.href = url; anchor.download = `bulletin-paie-${bulletin.matricule}-${bulletin.dateDebut}.pdf`;
        anchor.click(); URL.revokeObjectURL(url); this.downloadingId = null;
      },
      error: () => { this.downloadingId = null; this.notifications.error('PDF indisponible', 'Le bulletin n’a pas pu être téléchargé. Réessayez dans un instant.'); }
    });
  }

  private load(): void {
    this.loading = true;
    (this.rhView ? this.service.rh() : this.service.own()).subscribe({
      next: (rows) => { this.bulletins = rows; this.loading = false; },
      error: () => { this.bulletins = []; this.loading = false; this.notifications.error('Bulletins indisponibles', 'La liste de paie n’a pas pu être chargée.'); }
    });
  }
}
