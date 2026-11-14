import { Routes } from '@angular/router';
import { AuthShellComponent } from './layout/auth-shell.component';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    component: AuthShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'connexion' },
      {
        path: 'connexion',
        loadComponent: () => import('./pages/login/login-page.component').then((page) => page.LoginPageComponent)
      },
      {
        path: 'inscription',
        loadComponent: () => import('./pages/register/register-page.component').then((page) => page.RegisterPageComponent)
      }
    ]
  }
];
