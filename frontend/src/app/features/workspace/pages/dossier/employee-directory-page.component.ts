import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { RoleService } from '../../../../core/auth/role.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { EmployeeDirectory, EmployeeDossierService } from '../../services/employee-dossier.service';

@Component({ selector: 'app-employee-directory', standalone: true, imports: [DatePipe, FormsModule],
  templateUrl: './employee-directory-page.component.html', styleUrl: './employee-directory-page.component.scss' })
export class EmployeeDirectoryPageComponent implements OnInit {
  private readonly service = inject(EmployeeDossierService); private readonly role = inject(RoleService);
  private readonly router = inject(Router); private readonly notifications = inject(NotificationService);
  readonly manager = this.role.currentRole() === 'manager';
  data: EmployeeDirectory | null = null; loading = true; search = ''; departmentId: number | null = null;
  postId: number | null = null; status = ''; page = 0; readonly pageSize = 15;
  sortBy = 'nom'; sortDirection: 'asc' | 'desc' = 'asc';
  get posts() { return (this.data?.postes ?? []).filter((post) => !this.departmentId || post.departementId === this.departmentId); }
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true;
    this.service.directory(this.manager, { q: this.search, departementId: this.departmentId, posteId: this.postId, statut: this.status, page: this.page, size: this.pageSize, sortBy: this.sortBy, sortDirection: this.sortDirection })
      .pipe(finalize(() => this.loading = false)).subscribe({ next: (data) => this.data = data,
        error: () => { this.data = null; this.notifications.error('Annuaire indisponible', 'Impossible de charger les employés autorisés pour votre compte.'); } });
  }
  resetPage(): void { this.page = 0; this.load(); }
  toggleSortDirection(): void { this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc'; this.resetPage(); }
  open(id: number): void { void this.router.navigateByUrl(this.manager ? `/espace/manager/equipe/effectif/${id}` : `/espace/rh/personnel/employes/${id}`); }
  previous(): void { if (this.page > 0) { this.page--; this.load(); } }
  next(): void { if (this.data && this.page + 1 < this.data.totalPages) { this.page++; this.load(); } }
}
