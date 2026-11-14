import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccountManagementService, ManagedUser } from '../../services/account-management.service';

@Component({
  selector: 'app-employee-directory-page',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './employee-directory-page.component.html',
  styleUrl: './employee-directory-page.component.scss'
})
export class EmployeeDirectoryPageComponent implements OnInit {
  private readonly service = inject(AccountManagementService);
  employees: ManagedUser[] = [];
  loading = true;
  search = '';
  department = '';

  ngOnInit(): void {
    this.service.employees().pipe(finalize(() => this.loading = false)).subscribe({
      next: (items) => this.employees = items,
      error: () => this.employees = []
    });
  }

  get departments(): string[] {
    return [...new Set(this.employees.map((employee) => employee.departement).filter((value): value is string => Boolean(value)))].sort();
  }

  get filtered(): ManagedUser[] {
    const query = this.search.trim().toLocaleLowerCase();
    return this.employees.filter((item) => {
      const queryMatch = !query || [item.full_name, item.username, item.email, item.matricule ?? '', item.poste ?? '']
        .some((value) => value.toLocaleLowerCase().includes(query));
      return queryMatch && (!this.department || item.departement === this.department);
    });
  }
}
