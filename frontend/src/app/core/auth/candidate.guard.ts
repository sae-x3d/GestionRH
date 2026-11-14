import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';
import { NotificationService } from '../notifications/notification.service';

export const candidateGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notifications = inject(NotificationService);
  return auth.ensureCurrentSession().pipe(map((session) => {
    if (!session) return router.createUrlTree(['/connexion']);
    if (!session.roles.includes('simple_user')) {
      notifications.error('Espace candidat', 'La candidature est réservée aux comptes candidats.');
      return router.createUrlTree(['/offres']);
    }
    if (session.accountValidationStatus !== 'APPROVED') {
      notifications.error('Compte en attente', 'Votre compte doit être validé avant de pouvoir postuler.');
      return router.createUrlTree(['/offres']);
    }
    return true;
  }));
};
