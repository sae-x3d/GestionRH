import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmployeeDossierService, EmployeeSummary } from '../../services/employee-dossier.service';
import { LeaveManagementService, RecordedAbsence } from '../../services/leave-management.service';
import { NotificationService } from '../../../../core/notifications/notification.service';

@Component({
  selector: 'app-leave-absence-page', standalone: true, imports: [DatePipe, FormsModule],
  templateUrl: './leave-absence-page.component.html', styleUrl: './leave-absence-page.component.scss'
})
export class LeaveAbsencePageComponent implements OnInit {
  private readonly leaves = inject(LeaveManagementService);
  private readonly employeesApi = inject(EmployeeDossierService);
  private readonly notifications = inject(NotificationService);
  employees: EmployeeSummary[] = [];
  types: { code: string; libelle: string; justificatifRequis: boolean }[] = [];
  absences: RecordedAbsence[] = [];
  loading = true;
  saving = false;
  justificatif: File | null = null;
  readonly today = this.dateValue(new Date());
  filterStart = `${this.today.slice(0, 7)}-01`;
  filterEnd = this.monthEnd(this.filterStart);
  form = { employeId: null as number | null, typeCode: '', dateDebut: this.today, dateFin: this.today, motif: '', justifiee: false };

  ngOnInit(): void {
    this.employeesApi.directory(false, { page: 0, size: 200, sortBy: 'nom', sortDirection: 'asc' }).subscribe({
      next: (data) => this.employees = data.content,
      error: () => this.notifications.error('Salariés indisponibles', 'Impossible de charger la liste des salariés.')
    });
    this.leaves.getAbsenceTypes().subscribe({
      next: (types) => { this.types = types; this.form.typeCode = types[0]?.code ?? ''; },
      error: () => this.notifications.error('Référentiel indisponible', 'Impossible de charger les types d’absence configurés.')
    });
    this.load();
  }

  get selectedType() { return this.types.find((type) => type.code === this.form.typeCode); }

  selectJustificatif(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (file && (file.size > 15 * 1024 * 1024 || !['application/pdf', 'image/png', 'image/jpeg'].includes(file.type))) {
      this.notifications.error('Fichier refusé', 'Choisissez un PDF, PNG ou JPEG valide de 15 Mo maximum.');
      (event.target as HTMLInputElement).value = ''; this.justificatif = null; return;
    }
    this.justificatif = file;
  }

  load(): void {
    this.loading = true;
    this.leaves.listAbsences(this.filterStart, this.filterEnd).subscribe({
      next: (rows) => { this.absences = rows; this.loading = false; },
      error: () => { this.absences = []; this.loading = false; }
    });
  }

  record(): void {
    if (this.saving) return;
    if (!this.form.employeId || !this.form.typeCode || !this.form.dateDebut || !this.form.dateFin || !this.form.motif.trim()) {
      this.notifications.error('Formulaire incomplet', 'Sélectionnez un salarié, un type, une période et indiquez le motif.'); return;
    }
    if (this.form.dateFin < this.form.dateDebut) { this.notifications.error('Période incorrecte', 'La fin doit être égale ou postérieure au début.'); return; }
    if (this.selectedType?.justificatifRequis && !this.justificatif) { this.notifications.error('Justificatif obligatoire', 'Ajoutez le document demandé pour ce type d’absence.'); return; }
    this.saving = true;
    this.leaves.recordAbsence({ ...this.form, employeId: this.form.employeId!, motif: this.form.motif.trim() }, this.justificatif ?? undefined).subscribe({
      next: (result) => { this.saving = false; this.justificatif = null; this.notifications.success('Absence enregistrée', result.message); if (result.alerteEffectif) this.notifications.error('Sous-effectif prévisionnel', result.alerteEffectif); this.load(); },
      error: () => this.saving = false
    });
  }

  updateRange(): void { if (this.filterStart) this.filterEnd = this.monthEnd(this.filterStart); this.load(); }
  private monthEnd(value: string): string { const [year, month] = value.split('-').map(Number); return this.dateValue(new Date(year, month, 0)); }
  private dateValue(date: Date): string { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
}
