import { Component, Input } from '@angular/core';

export interface EvolutionChartPoint {
  date: string;
  label: string;
  value: number;
}

export interface EvolutionChartSeries {
  label: string;
  color: string;
  points: EvolutionChartPoint[];
}

@Component({
  selector: 'app-formation-evolution-chart',
  standalone: true,
  templateUrl: './formation-evolution-chart.component.html',
  styleUrl: './formation-evolution-chart.component.scss'
})
export class FormationEvolutionChartComponent {
  @Input() series: EvolutionChartSeries[] = [];
  @Input() yMin = 0;
  @Input() yMax = 100;
  @Input() yUnit = '%';
  @Input() height = 300;

  readonly width = 760;
  readonly padding = { left: 48, right: 20, top: 20, bottom: 46 };

  get plotWidth(): number {
    return this.width - this.padding.left - this.padding.right;
  }

  get plotHeight(): number {
    return this.height - this.padding.top - this.padding.bottom;
  }

  get allPoints(): EvolutionChartPoint[] {
    return this.series.flatMap((item) => item.points);
  }

  get gridValues(): number[] {
    return Array.from({ length: 5 }, (_, index) => this.yMin + ((this.yMax - this.yMin) * index) / 4);
  }

  get times(): number[] {
    return this.allPoints
      .map((point) => new Date(point.date).getTime())
      .filter((time) => !Number.isNaN(time));
  }

  get minTime(): number {
    return this.times.length ? Math.min(...this.times) : 0;
  }

  get maxTime(): number {
    return this.times.length ? Math.max(...this.times) : 0;
  }

  get xTicks(): EvolutionChartPoint[] {
    const byDay = new Map<string, EvolutionChartPoint>();
    for (const point of [...this.allPoints].sort(
      (left, right) => new Date(left.date).getTime() - new Date(right.date).getTime()
    )) {
      const day = this.day(point.date);
      if (!byDay.has(day)) byDay.set(day, point);
    }
    const points = [...byDay.values()];
    if (points.length <= 6) return points;
    const step = Math.ceil((points.length - 1) / 5);
    return points.filter((_, index) => index % step === 0 || index === points.length - 1);
  }

  x(point: EvolutionChartPoint): number {
    if (this.maxTime === this.minTime) return this.padding.left + this.plotWidth / 2;
    const time = new Date(point.date).getTime();
    return this.padding.left + ((time - this.minTime) / (this.maxTime - this.minTime)) * this.plotWidth;
  }

  y(value: number): number {
    const bounded = Math.max(this.yMin, Math.min(this.yMax, value));
    return this.padding.top + (1 - (bounded - this.yMin) / (this.yMax - this.yMin)) * this.plotHeight;
  }

  path(item: EvolutionChartSeries): string {
    return item.points
      .map((point, index) => `${index === 0 ? 'M' : 'L'} ${this.x(point).toFixed(1)} ${this.y(point.value).toFixed(1)}`)
      .join(' ');
  }

  formatValue(value: number): string {
    if (Number.isInteger(value)) return String(value);
    return value.toFixed(2).replace(/\.?0+$/, '');
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: '2-digit' });
  }

  private day(date: string): string {
    const value = new Date(date);
    return Number.isNaN(value.getTime()) ? date : value.toISOString().slice(0, 10);
  }
}
