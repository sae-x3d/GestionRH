import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MyFormation, FormationService } from '../../services/formation.service';

@Component({
  selector: 'app-my-formations-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './my-formations-page.component.html',
  styleUrl: './my-formations-page.component.scss'
})
export class MyFormationsPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(FormationService);
  readonly role = this.route.snapshot.data['formationRole'] as 'employee' | 'manager';
  formations: MyFormation[] = [];
  loading = true;

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading = true;
    this.service.myFormations().subscribe({
      next: (formations) => { this.formations = formations; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
