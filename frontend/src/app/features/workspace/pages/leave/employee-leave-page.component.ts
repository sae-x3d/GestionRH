import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { LeaveEmployeeOverview, LeaveManagementService, LeaveTypeOption } from '../../services/leave-management.service';

@Component({
  selector: 'app-employee-leave-page', standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, ButtonModule, InputTextModule],
  templateUrl: './employee-leave-page.component.html', styleUrl: './employee-leave-page.component.scss'
})
export class EmployeeLeavePageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(LeaveManagementService);
  private readonly notifications = inject(NotificationService);
  data: LeaveEmployeeOverview | null = null;
  loading = true;
  submitting = false;
  justificatif: File | null = null;
  readonly form = this.fb.group({ typeCongeId: [null as number | null, Validators.required], dateDebut: ['', Validators.required], dateFin: ['', Validators.required], motif: ['', [Validators.maxLength(2000)]] });

  ngOnInit(): void { this.load(); }
  get selectedType(): LeaveTypeOption | undefined { return this.data?.types.find((type) => type.id === this.form.controls.typeCongeId.value); }
  get selectedBalance(): number | null {
    const typeId = this.form.controls.typeCongeId.value;
    return this.data?.soldes.find((balance) => balance.typeId === typeId)?.solde ?? null;
  }
  get minDate(): string { return this.today(); }

  selectJustificatif(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (file && (file.size > 15 * 1024 * 1024 || !['application/pdf', 'image/png', 'image/jpeg'].includes(file.type))) {
      this.notifications.error('Fichier refusé', 'Choisissez un PDF, PNG ou JPEG valide de 15 Mo maximum.');
      (event.target as HTMLInputElement).value = '';
      this.justificatif = null;
      return;
    }
    this.justificatif = file;
  }

  attachForRequest(id: number, event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (file.size > 15 * 1024 * 1024 || !['application/pdf', 'image/png', 'image/jpeg'].includes(file.type)) {
      this.notifications.error('Fichier refusé', 'Choisissez un PDF, PNG ou JPEG valide de 15 Mo maximum.');
      return;
    }
    this.service.attachLeaveJustification(id, file).subscribe({
      next: () => { this.notifications.success('Justificatif ajouté', 'Le document est joint à la demande.'); this.load(); },
      error: () => undefined
    });
  }

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); this.notifications.error('Demande incomplète', 'Choisissez un type, les dates et indiquez le motif demandé.'); return; }
    const value = this.form.getRawValue();
    const dateDebut = value.dateDebut ?? '';
    const dateFin = value.dateFin ?? '';
    if (dateDebut > dateFin) { this.notifications.error('Période incorrecte', 'La date de fin doit être égale ou postérieure à la date de début.'); return; }
    if (this.selectedType?.justificatifRequis && !this.justificatif) { this.notifications.error('Justificatif obligatoire', 'Ajoutez le document demandé avant de transmettre cette demande.'); return; }
    this.submitting = true;
    this.service.submitLeave({ typeCongeId: value.typeCongeId!, dateDebut, dateFin, motif: value.motif?.trim() ?? '' }).subscribe({
      next: (request) => {
        const send = (file?: File) => {
          const done = () => { this.submitting = false; this.notifications.success('Demande transmise', `Demande n°${request.id} · ${request.jours} jour(s), en attente du manager.`); this.form.reset({ typeCongeId: null, dateDebut: '', dateFin: '', motif: '' }); this.justificatif = null; this.load(); };
          if (!file) { done(); return; }
          this.service.attachLeaveJustification(request.id, file).subscribe({ next: done, error: () => { this.submitting = false; this.notifications.error('Demande créée, justificatif non envoyé', `La demande n°${request.id} est enregistrée. Ajoutez le justificatif depuis son historique.`); this.form.reset({ typeCongeId: null, dateDebut: '', dateFin: '', motif: '' }); this.justificatif = null; this.load(); } });
        };
        send(this.justificatif ?? undefined);
      },
      error: () => this.submitting = false
    });
  }

  private load(): void { this.loading = true; this.service.getEmployeeOverview().subscribe({ next: (data) => { this.data = data; this.loading = false; }, error: () => this.loading = false }); }
  private today(): string { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`; }
}
