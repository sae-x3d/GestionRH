import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { LeaveConfiguration, LeaveManagementService, LeaveTypeRuleView, StaffingRuleView } from '../../services/leave-management.service';

@Component({
  selector: 'app-leave-configuration-page',
  standalone: true,
  imports: [DatePipe, FormsModule, ButtonModule, InputTextModule],
  templateUrl: './leave-configuration-page.component.html',
  styleUrl: './leave-configuration-page.component.scss'
})
export class LeaveConfigurationPageComponent implements OnInit {
  private readonly service = inject(LeaveManagementService);
  private readonly notifications = inject(NotificationService);
  data: LeaveConfiguration | null = null;
  loading = true;
  savingKey: string | null = null;

  ngOnInit(): void { this.load(); }

  saveMinimum(rule: StaffingRuleView): void {
    if (this.savingKey || rule.nouveauMinimum == null || !Number.isInteger(Number(rule.nouveauMinimum)) || Number(rule.nouveauMinimum) < 0 || !rule.motif?.trim()) {
      this.notifications.error('Paramètres incomplets', 'Indiquez un seuil entier positif ou nul et un motif de modification.');
      return;
    }
    this.savingKey = `poste-${rule.posteId}`;
    this.service.setMinimum(rule.posteId, Number(rule.nouveauMinimum), this.today(), rule.motif.trim()).subscribe({
      next: (data) => { this.data = data; this.savingKey = null; this.notifications.success('Seuil enregistré', `Le seuil minimal de ${rule.poste} a été mis à jour.`); },
      error: () => this.savingKey = null
    });
  }

  saveAccrual(type: LeaveTypeRuleView): void {
    if (this.savingKey || type.nouveauTaux == null || Number(type.nouveauTaux) < 0) {
      this.notifications.error('Taux invalide', 'Saisissez un nombre de jours par mois positif ou nul.');
      return;
    }
    this.savingKey = `type-${type.typeId}`;
    this.service.setAccrual(type.typeId, Number(type.nouveauTaux), this.today()).subscribe({
      next: (data) => { this.data = data; this.savingKey = null; this.notifications.success('Règle enregistrée', `Le taux d’acquisition de ${type.libelle} a été mis à jour.`); },
      error: () => this.savingKey = null
    });
  }

  saveTypePolicy(type: LeaveTypeRuleView): void {
    if (this.savingKey) return;
    this.savingKey = `policy-${type.typeId}`;
    this.service.setLeaveTypePolicy(type.typeId, type.actif, type.justificatifRequis).subscribe({
      next: (data) => { this.data = data; this.savingKey = null; this.notifications.success('Type mis à jour', `Les règles de ${type.libelle} ont été enregistrées.`); },
      error: () => this.savingKey = null
    });
  }

  private load(): void {
    this.loading = true;
    this.service.getConfiguration().subscribe({
      next: (data) => {
        this.data = data;
        this.data.typesConge.forEach((type) => type.nouveauTaux = type.joursParMois);
        this.data.effectifs.forEach((item) => { item.nouveauMinimum = item.effectifMinimal; item.motif = ''; });
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
