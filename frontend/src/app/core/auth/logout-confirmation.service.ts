import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class LogoutConfirmationService {
  private readonly confirmation = inject(ConfirmationService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  confirm(): void {
    this.confirmation.confirm({
      header: 'Confirmer la déconnexion',
      message: 'Voulez-vous vraiment fermer votre session ?',
      icon: 'pi pi-sign-out',
      acceptLabel: 'Me déconnecter',
      rejectLabel: 'Rester connecté',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-secondary',
      accept: () => this.auth.logout().subscribe(() => void this.router.navigateByUrl('/connexion'))
    });
  }
}
