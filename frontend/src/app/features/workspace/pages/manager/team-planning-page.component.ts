import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PlanningDay, PlanningEmployee, PlanningWeek, TimeManagementService } from '../../services/time-management.service';

@Component({
  selector: 'app-team-planning-page', standalone: true, imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './team-planning-page.component.html', styleUrl: './team-planning-page.component.scss'
})
export class TeamPlanningPageComponent implements OnInit {
  private readonly time = inject(TimeManagementService);
  private readonly toast = inject(NotificationService);
  weekStart = this.addDays(this.mondayOf(new Date()), 7);
  week?: PlanningWeek;
  loading = true;
  saving = false;
  selectedEmployeeIds: number[] = [];
  selectedDates: string[] = [];
  typeJourId: number | null = null;
  heureDebut = '';
  heureFin = '';
  pauseDebut = '';
  pauseFin = '';
  private defaultScheduleLoaded = false;
  observation = '';

  ngOnInit(): void { this.load(); }

  get employees(): PlanningEmployee[] { return this.week?.employes ?? []; }
  get days(): Array<{ date: string; label: string; number: string }> {
    return Array.from({ length: 7 }, (_, index) => {
      const date = this.addDays(this.weekStart, index);
      const parsed = this.parseDate(date);
      return { date, label: new Intl.DateTimeFormat('fr-FR', { weekday: 'short' }).format(parsed), number: new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit' }).format(parsed) };
    });
  }
  get selectedType(): string { return this.week?.typesJour?.find((type) => type.id === Number(this.typeJourId))?.code ?? 'TRAVAIL'; }
  get selectedEmployeesCount(): number { return this.selectedEmployeeIds.length; }

  load(): void {
    this.loading = true;
    this.time.managerPlanning(this.weekStart).subscribe({
      next: (week) => {
        this.week = week; this.loading = false;
        if (!this.defaultScheduleLoaded && week.horairesParDefaut) {
          this.heureDebut = week.horairesParDefaut.heureDebut?.slice(0, 5) ?? '';
          this.heureFin = week.horairesParDefaut.heureFin?.slice(0, 5) ?? '';
          this.pauseDebut = week.horairesParDefaut.pauseDebut?.slice(0, 5) ?? '';
          this.pauseFin = week.horairesParDefaut.pauseFin?.slice(0, 5) ?? '';
          this.defaultScheduleLoaded = true;
        }
        if (this.typeJourId === null) this.typeJourId = week.typesJour?.find((type) => type.code === 'TRAVAIL')?.id ?? null;
        this.selectedEmployeeIds = this.selectedEmployeeIds.filter((id) => week.employes?.some((employee) => employee.employe_id === id));
      },
      error: (error) => { this.loading = false; this.toast.error('Planning indisponible', this.errorMessage(error)); }
    });
  }

  previousWeek(): void { this.weekStart = this.addDays(this.weekStart, -7); this.load(); }
  nextWeek(): void { this.weekStart = this.addDays(this.weekStart, 7); this.load(); }
  goToCurrentWeek(): void { this.weekStart = this.mondayOf(new Date()); this.load(); }

  toggleEmployee(id: number, checked: boolean): void {
    this.selectedEmployeeIds = checked ? [...new Set([...this.selectedEmployeeIds, id])] : this.selectedEmployeeIds.filter((value) => value !== id);
  }
  toggleAllEmployees(checked: boolean): void { this.selectedEmployeeIds = checked ? this.employees.map((employee) => employee.employe_id) : []; }
  toggleDate(date: string, checked: boolean): void {
    this.selectedDates = checked ? [...new Set([...this.selectedDates, date])] : this.selectedDates.filter((value) => value !== date);
  }
  selectWeekdays(): void {
    const weekdays = new Set((this.week?.joursOuvrables ?? []).map((day) => day.numero_iso));
    this.selectedDates = this.days.filter((day) => weekdays.has(this.parseDate(day.date).getDay() || 7)).map((day) => day.date);
  }
  selectAllDays(): void { this.selectedDates = this.days.map((day) => day.date); }
  clearSelection(): void { this.selectedEmployeeIds = []; this.selectedDates = []; }
  isEmployeeSelected(id: number): boolean { return this.selectedEmployeeIds.includes(id); }
  isDateSelected(date: string): boolean { return this.selectedDates.includes(date); }

  assignmentFor(employeeId: number, date: string): PlanningDay | undefined {
    return this.week?.jours?.find((item) => item.employeId === employeeId && item.date === date);
  }
  formatSlot(item?: PlanningDay): string {
    if (!item) return 'À planifier';
    if (item.type_code !== 'TRAVAIL') return item.type_jour;
    return `${item.debut?.slice(0, 5) ?? '--:--'} – ${item.fin?.slice(0, 5) ?? '--:--'}`;
  }
  summary(date: string, key: 'planifies' | 'repos' | 'arrives' | 'absents' | 'nonPlanifies'): number {
    const value = this.week?.compteurs?.find((item) => item.date === date)?.[key];
    return Number(value ?? 0);
  }

  assign(): void {
    if (!this.selectedEmployeeIds.length || !this.selectedDates.length || this.typeJourId === null) {
      this.toast.error('Selection incomplète', 'Choisissez au moins un agent, une date et un type de journee.'); return;
    }
    const workday = this.selectedType === 'TRAVAIL';
    this.saving = true;
    this.time.assign({
      employeIds: this.selectedEmployeeIds, dates: this.selectedDates, typeJourId: Number(this.typeJourId),
      heureDebut: workday ? this.heureDebut : null, heureFin: workday ? this.heureFin : null,
      pauseDebut: workday && this.pauseDebut ? this.pauseDebut : null,
      pauseFin: workday && this.pauseFin ? this.pauseFin : null,
      observation: this.observation.trim() || null
    }).subscribe({
      next: (week) => { this.week = week; this.saving = false; this.selectedDates = []; this.observation = ''; this.toast.success('Affectations enregistrees', 'Les jours selectionnes ont ete mis à jour dans le brouillon.'); },
      error: (error) => { this.saving = false; this.toast.error('Affectation impossible', this.errorMessage(error)); }
    });
  }

  publish(): void {
    const id = this.week?.planning?.id;
    if (!id || !window.confirm('Publier cette semaine ? Chaque agent devra avoir un jour travaille ou un repos pour les sept jours.')) return;
    this.saving = true;
    this.time.publish(id).subscribe({
      next: (week) => { this.week = week; this.saving = false; this.toast.success('Planning publie', 'Les agents de votre departement ont ete notifies.'); },
      error: (error) => { this.saving = false; this.toast.error('Publication impossible', this.errorMessage(error)); }
    });
  }
  reopen(): void {
    const id = this.week?.planning?.id;
    if (!id || !window.confirm('Rouvrir ce planning publie pour le corriger ?')) return;
    this.saving = true;
    this.time.reopen(id).subscribe({
      next: (week) => { this.week = week; this.saving = false; this.toast.success('Planning rouvert', 'Les modifications sont de nouveau possibles.'); },
      error: (error) => { this.saving = false; this.toast.error('Reouverture impossible', this.errorMessage(error)); }
    });
  }

  private mondayOf(date: Date): string {
    const local = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const day = local.getDay() || 7; local.setDate(local.getDate() - day + 1); return this.toIso(local);
  }
  private addDays(date: string, count: number): string { const parsed = this.parseDate(date); parsed.setDate(parsed.getDate() + count); return this.toIso(parsed); }
  private parseDate(value: string): Date { const [year, month, day] = value.split('-').map(Number); return new Date(year, month - 1, day); }
  private toIso(value: Date): string { return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`; }
  private errorMessage(error: any): string { return error?.error?.detail ?? error?.error?.message ?? 'Reessayez dans quelques instants.'; }
}

