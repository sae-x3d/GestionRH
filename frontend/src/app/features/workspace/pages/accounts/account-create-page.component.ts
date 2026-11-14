import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { AccountManagementService, ManagedPost, ManagedUser } from '../../services/account-management.service';

@Component({
  selector: 'app-account-create-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './account-create-page.component.html',
  styleUrl: './account-create-page.component.scss'
})
export class AccountCreatePageComponent implements OnInit {
  private readonly service = inject(AccountManagementService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  roles: { code: string }[] = [];
  postes: ManagedPost[] = [];
  account = { username: '', email: '', fullName: '', password: '', role: '', posteId: null as number | null, dateEmbauche: '', dateNaissance: '' };
  loading = true;
  saving = false;
  readonly today = new Date().toISOString().slice(0, 10);

  ngOnInit(): void {
    this.service.options().pipe(finalize(() => this.loading = false)).subscribe({
      next: (options) => { this.roles = options.roles; this.postes = options.postes; this.account.role = this.roles[0]?.code ?? ''; },
      error: () => { this.roles = []; this.postes = []; }
    });
  }

  get employmentRequired(): boolean { return ['manager', 'employe'].includes(this.account.role); }

  submit(): void {
    if (this.saving || this.loading) return;
    if (!this.account.username.trim() || !this.account.fullName.trim() || !this.account.email.trim() || this.account.password.length < 12) {
      this.notifications.error('Informations incomplètes', 'Renseignez le nom, l’identifiant, l’e-mail et un mot de passe d’au moins 12 caractères.');
      return;
    }
    if (this.employmentRequired && (!this.account.posteId || !this.account.dateEmbauche || !this.account.dateNaissance)) {
      this.notifications.error('Fiche employé incomplète', 'Pour un manager ou un employé, renseignez le poste, la date d’embauche et la date de naissance.');
      return;
    }
    const payload: Record<string, unknown> = { ...this.account };
    if (!this.account.posteId) {
      delete payload['posteId']; delete payload['dateEmbauche']; delete payload['dateNaissance'];
    }
    this.saving = true;
    this.service.create(payload).pipe(finalize(() => this.saving = false)).subscribe({
      next: (user: ManagedUser) => {
        this.notifications.success('Compte créé', `Le compte de ${user.full_name} est actif avec le rôle attribué.`);
        void this.router.navigate(['/espace/gestionnaire/comptes']);
      }, error: () => undefined
    });
  }
}
