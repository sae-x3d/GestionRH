import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { FormationNeed, FormationService } from '../../services/formation.service';

@Component({
  selector: 'app-training-needs-page',
  standalone: true,
  imports: [DatePipe, ButtonModule],
  templateUrl: './training-needs-page.component.html',
  styleUrl: './training-needs-page.component.scss'
})
export class TrainingNeedsPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(FormationService);
  readonly role = this.route.snapshot.data['needRole'] as 'manager' | 'rh';
  needs: FormationNeed[] = [];
  loading = true;

  ngOnInit(): void { this.load(); }

  openFormations(): void {
    void this.router.navigate(['/espace/rh/formations']);
  }

  private load(): void {
    this.loading = true;
    const request = this.role === 'manager' ? this.service.managerNeeds() : this.service.rhNeeds();
    request.subscribe({
      next: (needs) => { this.needs = needs; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
