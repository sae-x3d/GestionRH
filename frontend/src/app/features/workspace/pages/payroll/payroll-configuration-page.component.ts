import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { IrsaBracket, PayrollAdministrationService, PayrollParameter } from '../../services/payroll-administration.service';

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
  irsaBrackets: IrsaBracket[] = [];
  irsaDateEffet = this.today();
  irsaMotif = '';
  savingIrsa = false;
  loading = true;
  savingId: number | null = null;

  ngOnInit(): void { this.load(); this.loadIrsa(); }

  addIrsaBracket(): void {
    const last = this.irsaBrackets.at(-1);
    if (!last || last.borneSuperieure === null || last.borneSuperieure <= last.borneInferieure) return;
    const lower = last.borneSuperieure;
    last.borneSuperieure = lower + 50000;
    this.irsaBrackets.push({ borneInferieure: lower + 50000, borneSuperieure: null, taux: last.taux, sourceRegle: 'Barème configurable par Administration' });
  }

  removeIrsaBracket(index: number): void {
    if (this.irsaBrackets.length <= 1) return;
    this.irsaBrackets.splice(index, 1);
    const last = this.irsaBrackets.at(-1);
    if (last) last.borneSuperieure = null;
  }

  saveIrsa(): void {
    if (this.savingIrsa || !this.irsaMotif.trim() || !this.irsaDateEffet) return;
    this.savingIrsa = true;
    this.service.updateIrsa(this.irsaDateEffet, this.irsaMotif, this.irsaBrackets).subscribe({
      next: rows => { this.irsaBrackets = rows.filter(row => row.modifiable); this.irsaMotif = ''; this.savingIrsa = false; this.notifications.success('Barème IRSA versionné', 'La nouvelle grille est historisée et sera appliquée selon sa date d’effet.'); },
      error: () => { this.savingIrsa = false; this.notifications.error('Barème non enregistré', 'Vérifiez les bornes contiguës, les taux entre 0 et 100 %, et la date d’effet.'); }
    });
  }

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

  private loadIrsa(): void {
    this.service.getIrsa().subscribe({
      next: rows => { this.irsaBrackets = rows.filter(row => row.modifiable).sort((a, b) => a.borneInferieure - b.borneInferieure); },
      error: () => this.notifications.error('Barème indisponible', 'Les tranches IRSA ne peuvent pas être chargées.')
    });
  }

  private today(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }
}
