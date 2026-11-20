import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { FormationRow, FormationService } from '../../services/formation.service';

@Component({
  selector: 'app-formation-list-page',
  standalone: true,
  imports: [DatePipe, RouterLink, ButtonModule],
  templateUrl: './formation-list-page.component.html',
  styleUrl: './formation-list-page.component.scss'
})
export class FormationListPageComponent implements OnInit {
  private readonly service = inject(FormationService);
  formations: FormationRow[] = [];
  loading = true;

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading = true;
    this.service.list().subscribe({
      next: (formations) => { this.formations = formations; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
