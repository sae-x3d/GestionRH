import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  FormationEvolutionPoint,
  MyFormation,
  FormationService
} from '../../services/formation.service';
import {
  EvolutionChartSeries,
  FormationEvolutionChartComponent
} from './formation-evolution-chart.component';

@Component({
  selector: 'app-my-formations-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormationEvolutionChartComponent],
  templateUrl: './my-formations-page.component.html',
  styleUrl: './my-formations-page.component.scss'
})
export class MyFormationsPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(FormationService);
  readonly role = this.route.snapshot.data['formationRole'] as 'employee' | 'manager';

  formations: MyFormation[] = [];
  evolution: FormationEvolutionPoint[] = [];
  metric: 'niveau' | 'pourcentage' = 'niveau';
  loading = true;

  ngOnInit(): void { this.load(); }

  setMetric(metric: 'niveau' | 'pourcentage'): void {
    this.metric = metric;
  }

  get levelSeries(): EvolutionChartSeries[] {
    return [
      {
        label: 'Niveau avant formation',
        color: '#d97706',
        points: this.evolution.map((point) => ({
          date: point.dateMesure,
          label: `${point.competence} — ${point.formation}`,
          value: point.niveauAvant
        }))
      },
      {
        label: 'Niveau après formation',
        color: '#176b63',
        points: this.evolution.map((point) => ({
          date: point.dateMesure,
          label: `${point.competence} — ${point.formation}`,
          value: point.niveauApres
        }))
      }
    ];
  }

  get percentageSeries(): EvolutionChartSeries[] {
    return [{
      label: 'Pourcentage du manque comblé',
      color: '#2563eb',
      points: this.evolution.map((point) => ({
        date: point.dateMesure,
        label: `${point.competence} — ${point.formation}`,
        value: Number(point.pourcentage)
      }))
    }];
  }

  private load(): void {
    this.loading = true;
    forkJoin({
      formations: this.service.myFormations(),
      evolution: this.service.myEvolution()
    }).subscribe({
      next: ({ formations, evolution }) => {
        this.formations = formations;
        this.evolution = evolution;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }
}
