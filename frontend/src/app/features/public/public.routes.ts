import { Routes } from '@angular/router';
import { PublicLayoutComponent } from './layout/public-layout.component';
import { candidateGuard } from '../../core/auth/candidate.guard';

export const PUBLIC_ROUTES: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      {
        path: ':id/postuler',
        canActivate: [candidateGuard],
        loadComponent: () => import('./pages/apply/candidate-application-page.component').then((page) => page.CandidateApplicationPageComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./pages/offers/offer-detail-page.component').then((page) => page.OfferDetailPageComponent)
      },
      {
        path: '',
        loadComponent: () => import('./pages/offers/offers-page.component').then((page) => page.OffersPageComponent)
      }
    ]
  }
];
