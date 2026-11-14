import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const appRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'offres' },
  {
    path: 'offres',
    loadChildren: () => import('./features/public/public.routes').then((routes) => routes.PUBLIC_ROUTES)
  },
  {
    path: 'espace',
    canActivate: [authGuard],
    loadChildren: () => import('./features/workspace/workspace.routes').then((routes) => routes.WORKSPACE_ROUTES)
  },
  {
    path: '',
    loadChildren: () => import('./features/auth/auth.routes').then((routes) => routes.AUTH_ROUTES)
  },
  { path: '**', redirectTo: 'offres' }
];
