import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';
import { RoleService } from './role.service';

export const roleGuard: CanActivateFn = (route) => {
  const allowedRoles = route.data['roles'] as string[] | undefined;
  const auth = inject(AuthService);
  const roles = inject(RoleService);
  const router = inject(Router);
  return auth.ensureCurrentSession().pipe(map((session) =>
    session && allowedRoles?.some((role) => roles.currentRoles().includes(role))
      ? true
      : router.createUrlTree(['/espace'])
  ));
};
