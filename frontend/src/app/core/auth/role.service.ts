import { computed, Injectable, Signal } from '@angular/core';
import { AuthService } from './auth.service';

export type UserRole = 'simple_user' | 'employe' | 'manager' | 'rh' | 'administration' | 'gestionnaire';

const ROLE_PRIORITY: UserRole[] = [
  'administration', 'rh', 'manager', 'gestionnaire', 'employe', 'simple_user'
];

@Injectable({ providedIn: 'root' })
export class RoleService {
  readonly currentRoles: Signal<string[]>;
  readonly currentRole: Signal<UserRole | null>;
  readonly isFinanceMember: Signal<boolean>;

  constructor(private readonly authService: AuthService) {
    this.currentRoles = computed(() => this.authService.currentSession()?.roles ?? []);
    this.isFinanceMember = computed(() => this.authService.currentSession()?.departmentCodes?.includes('FINANCE') ?? false);
    this.currentRole = computed(() => {
      const roles = this.currentRoles();
      return ROLE_PRIORITY.find((role) => roles.includes(role)) ?? null;
    });
  }

  getRole(): UserRole | null {
    return this.currentRole();
  }

  clearRole(): void {
    this.authService.clearLocalSession();
  }
}
