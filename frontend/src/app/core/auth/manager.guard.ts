import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleService } from './role.service';

export const managerGuard: CanActivateFn = () => {
  const roles = inject(RoleService).currentRoles();
  const router = inject(Router);
  return roles.includes('manager') ? true : router.createUrlTree(['/offres']);
};
