import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { AccountManagementService, ManagedUser } from '../../services/account-management.service';

@Component({
  selector: 'app-pending-registrations-page',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './pending-registrations-page.component.html',
  styleUrl: './pending-registrations-page.component.scss'
})
export class PendingRegistrationsPageComponent implements OnInit {
  private readonly service = inject(AccountManagementService);
  private readonly notifications = inject(NotificationService);
  registrations: ManagedUser[] = [];
  search = '';
  selected: ManagedUser | null = null;
  comment = '';
  loading = true;
  savingId: number | null = null;

  get filteredRegistrations(): ManagedUser[] {
    const query = this.search.trim().toLocaleLowerCase('fr');
    if (!query) return this.registrations;
    return this.registrations.filter((item) =>
      [item.full_name, item.username, item.email].some((value) => value?.toLocaleLowerCase('fr').includes(query)));
  }

  ngOnInit(): void { this.load(); }
  open(item: ManagedUser): void { this.selected = item; this.comment = ''; }
  close(): void { this.selected = null; }

  decide(status: 'APPROVED' | 'REJECTED'): void {
    if (!this.selected || this.savingId !== null) return;
    if (status === 'REJECTED' && !this.comment.trim()) {
      this.notifications.error('Motif requis', 'Ajoutez un motif pour informer la personne du refus de son inscription.');
      return;
    }
    const target = this.selected;
    this.savingId = target.id;
    this.service.decideRegistration(target.id, status, this.comment).pipe(finalize(() => this.savingId = null)).subscribe({
      next: () => {
        this.registrations = this.registrations.filter((item) => item.id !== target.id);
        this.selected = null;
        this.notifications.success(status === 'APPROVED' ? 'Inscription validée' : 'Inscription refusée',
          status === 'APPROVED' ? `${target.full_name} peut maintenant postuler aux offres.` : 'La personne a été informée de la décision.');
      }, error: () => undefined
    });
  }

  private load(): void {
    this.loading = true;
    this.service.pending().pipe(finalize(() => this.loading = false)).subscribe({
      next: (items) => this.registrations = items,
      error: () => this.registrations = []
    });
  }
}
