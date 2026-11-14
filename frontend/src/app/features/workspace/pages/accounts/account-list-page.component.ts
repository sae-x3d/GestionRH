import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { AccountManagementService, ManagedPost, ManagedUser } from '../../services/account-management.service';

@Component({
  selector: 'app-account-list-page',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './account-list-page.component.html',
  styleUrl: './account-list-page.component.scss'
})
export class AccountListPageComponent implements OnInit {
  private readonly service = inject(AccountManagementService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  users: ManagedUser[] = [];
  roleOptions: { code: string }[] = [];
  postOptions: ManagedPost[] = [];
  selected: ManagedUser | null = null;
  selectedRoles: string[] = [];
  selectedPostId: number | null = null;
  search = '';
  statusFilter = '';
  roleFilter = '';
  loading = true;
  saving = false;

  ngOnInit(): void { this.load(); }

  get filteredUsers(): ManagedUser[] {
    const query = this.search.trim().toLocaleLowerCase();
    return this.users.filter((user) => {
      const matchesQuery = !query || [user.username, user.full_name, user.email, user.matricule ?? '', user.departement ?? '']
        .some((value) => value.toLocaleLowerCase().includes(query));
      const matchesStatus = !this.statusFilter || (this.statusFilter === 'SUSPENDED'
        ? user.is_suspended
        : this.statusFilter === 'APPROVED'
          ? user.validation_status === 'APPROVED' && !user.is_suspended
          : user.validation_status === this.statusFilter);
      const matchesRole = !this.roleFilter || user.roles.includes(this.roleFilter);
      return matchesQuery && matchesStatus && matchesRole;
    });
  }

  open(user: ManagedUser): void {
    this.selected = user;
    this.selectedRoles = [...user.roles];
    this.selectedPostId = user.poste_id;
  }

  close(): void { this.selected = null; }

  toggleRole(role: string, checked: boolean): void {
    this.selectedRoles = checked
      ? [...new Set([...this.selectedRoles, role])]
      : this.selectedRoles.filter((item) => item !== role);
  }

  saveRoles(): void {
    if (!this.selected || this.saving || !this.canManageSelected) return;
    this.saving = true;
    this.service.setRoles(this.selected.id, this.selectedRoles).pipe(finalize(() => this.saving = false)).subscribe({
      next: (user) => { this.replaceUser(user); this.notifications.success('Rôles mis à jour', `Les accès de ${user.full_name} ont été enregistrés.`); },
      error: () => undefined
    });
  }

  saveAssignment(): void {
    if (!this.selected || !this.selectedPostId || this.saving || !this.canManageSelected) return;
    this.saving = true;
    this.service.assignPost(this.selected.id, this.selectedPostId).pipe(finalize(() => this.saving = false)).subscribe({
      next: (user) => { this.replaceUser(user); this.notifications.success('Affectation mise à jour', `Le rattachement de ${user.full_name} est enregistré.`); },
      error: () => undefined
    });
  }

  toggleSuspension(): void {
    if (!this.selected || this.saving || !this.canManageSelected) return;
    const suspend = !this.selected.is_suspended;
    this.saving = true;
    this.service.setSuspended(this.selected.id, suspend).pipe(finalize(() => this.saving = false)).subscribe({
      next: (user) => {
        this.replaceUser(user);
        this.notifications.success(suspend ? 'Compte désactivé' : 'Compte réactivé', suspend
          ? 'La connexion de cet utilisateur est maintenant bloquée.' : 'Cet utilisateur peut de nouveau se connecter.');
      }, error: () => undefined
    });
  }

  get canManageSelected(): boolean { return Boolean(this.selected && this.selected.id !== this.auth.currentSession()?.userId); }

  roleName(role: string): string {
    return ({ simple_user: 'Candidat', employe: 'Employé', manager: 'Manager', rh: 'RH', administration: 'Administration', gestionnaire: 'Gestionnaire' } as Record<string, string>)[role] ?? role;
  }

  private replaceUser(user: ManagedUser): void {
    this.users = this.users.map((item) => item.id === user.id ? user : item);
    this.selected = user;
    this.selectedRoles = [...user.roles];
  }

  private load(): void {
    this.loading = true;
    forkJoin({ users: this.service.list(), options: this.service.options() }).pipe(finalize(() => this.loading = false)).subscribe({
      next: ({ users, options }) => { this.users = users; this.roleOptions = options.roles; this.postOptions = options.postes; },
      error: () => { this.users = []; this.roleOptions = []; this.postOptions = []; }
    });
  }
}
