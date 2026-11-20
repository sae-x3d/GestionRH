import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import {
  FormationEvolutionRow,
  FormationService
} from '../../services/formation.service';
import {
  EvolutionChartSeries,
  FormationEvolutionChartComponent
} from './formation-evolution-chart.component';

interface EmployeeEvolution {
  employeId: number;
  matricule: string;
  employe: string;
  departement: string;
  poste: string;
  points: FormationEvolutionRow[];
}

const SERIES_COLORS = ['#176b63', '#d97706', '#2563eb', '#9333ea', '#be123c', '#0f766e', '#b45309', '#4338ca'];

@Component({
  selector: 'app-formation-evolution-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink, ButtonModule, FormationEvolutionChartComponent],
  templateUrl: './formation-evolution-page.component.html',
  styleUrl: './formation-evolution-page.component.scss'
})
export class FormationEvolutionPageComponent implements OnInit {
  private readonly service = inject(FormationService);

  employees: EmployeeEvolution[] = [];
  selectedIds = new Set<number>();
  metric: 'pourcentage' | 'niveau' = 'pourcentage';
  loading = true;

  ngOnInit(): void { this.load(); }

  get selectedEmployees(): EmployeeEvolution[] {
    return this.employees.filter((employee) => this.selectedIds.has(employee.employeId));
  }

  get chartSeries(): EvolutionChartSeries[] {
    return this.selectedEmployees.map((employee, index) => ({
      label: `${employee.employe} (${employee.matricule})`,
      color: SERIES_COLORS[index % SERIES_COLORS.length],
      points: employee.points.map((point) => ({
        date: point.dateMesure,
        label: `${point.competence} — ${point.formation}`,
        value: this.metric === 'pourcentage' ? Number(point.pourcentage) : point.niveauApres
      }))
    }));
  }

  get tableRows(): FormationEvolutionRow[] {
    return this.selectedEmployees
      .flatMap((employee) => employee.points)
      .sort((left, right) => new Date(right.dateMesure).getTime() - new Date(left.dateMesure).getTime());
  }

  isSelected(id: number): boolean {
    return this.selectedIds.has(id);
  }

  toggle(id: number): void {
    if (this.selectedIds.has(id)) this.selectedIds.delete(id);
    else this.selectedIds.add(id);
  }

  toggleAll(): void {
    this.selectedIds = this.selectedIds.size === this.employees.length
      ? new Set<number>()
      : new Set(this.employees.map((employee) => employee.employeId));
  }

  setMetric(metric: 'pourcentage' | 'niveau'): void {
    this.metric = metric;
  }

  private load(): void {
    this.loading = true;
    this.service.rhEvolution().subscribe({
      next: (rows) => {
        const grouped = new Map<number, EmployeeEvolution>();
        for (const row of rows) {
          let employee = grouped.get(row.employeId);
          if (!employee) {
            employee = {
              employeId: row.employeId,
              matricule: row.matricule,
              employe: row.employe,
              departement: row.departement,
              poste: row.poste,
              points: []
            };
            grouped.set(row.employeId, employee);
          }
          employee.points.push(row);
        }
        this.employees = [...grouped.values()].map((employee) => ({
          ...employee,
          points: employee.points.sort(
            (left, right) => new Date(left.dateMesure).getTime() - new Date(right.dateMesure).getTime()
          )
        }));
        this.selectedIds = new Set(this.employees.map((employee) => employee.employeId));
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }
}
