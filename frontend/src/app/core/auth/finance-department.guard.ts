import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleService } from './role.service';

export const financeDepartmentGuard: CanActivateFn = () => inject(RoleService).isFinanceMember()
  ? true
  : inject(Router).createUrlTree(['/espace']);
