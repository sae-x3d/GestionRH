import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PayrollAdministrationService, PayrollParameter } from '../../services/payroll-administration.service';

@Component({
  selector: 'app-payroll-configuration-page',
  standalone: true,
  imports: [DatePipe, FormsModule, ButtonModule, InputTextModule],
  templateUrl: './payroll-configuration-page.component.html',
  styleUrl: './payroll-configuration-page.component.scss'
})
export class PayrollConfigurationPageComponent implements OnInit {
  private readonly service = inject(PayrollAdministrationService);
  private readonly notifications = inject(NotificationService);
  parameters: PayrollParameter[] = [];
  loading = true;
  savingId: number | null = null;

  ngOnInit(): void { this.load(); }

  save(parameter: PayrollParameter): void {
    const value = Number(parameter.nouvelleValeur);
    const effectiveDate = parameter.dateEffet ?? this.today();
    const reason = parameter.motif?.trim() ?? '';
    if (this.savingId !== null || !parameter.modifiable) return;
    if (!Number.isFinite(value) || value < 0 || !effectiveDate || !reason) {
      this.notifications.error('Paramètres incomplets', 'Saisissez une valeur positive ou nulle, une date d’effet et un motif.');
      return;
    }
    this.savingId = parameter.id;
    this.service.updateParameter(parameter.id, value, effectiveDate, reason).subscribe({
      next: (rows) => {
        this.parameters = rows.map((row) => ({ ...row, nouvelleValeur: row.valeur, dateEffet: this.today(), motif: '' }));
        this.savingId = null;
        this.notifications.success('Règle enregistrée', 'La nouvelle valeur et sa date d’effet sont conservées dans l’historique.');
      },
      error: () => this.savingId = null
    });
  }

  private load(): void {
    this.loading = true;
    this.service.getParameters().subscribe({
      next: (rows) => {
        this.parameters = rows.map((row) => ({ ...row, nouvelleValeur: row.valeur, dateEffet: this.today(), motif: '' }));
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  private today(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }
}
