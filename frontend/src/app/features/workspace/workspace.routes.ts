import { Routes } from '@angular/router';
import { WorkspaceLayoutComponent } from './layout/workspace-layout.component';
import { managerGuard } from '../../core/auth/manager.guard';
import { roleGuard } from '../../core/auth/role.guard';
import { financeDepartmentGuard } from '../../core/auth/finance-department.guard';

export const WORKSPACE_ROUTES: Routes = [
  {
    path: '',
    component: WorkspaceLayoutComponent,
    children: [
      {
        path: 'manager/equipe/effectif',
        canActivate: [managerGuard],
        loadComponent: () => import('./pages/dossier/employee-directory-page.component').then((page) => page.EmployeeDirectoryPageComponent)
      },
      {
        path: 'manager/equipe/effectif/:employeeId', canActivate: [managerGuard],
        loadComponent: () => import('./pages/dossier/employee-dossier-page.component').then((page) => page.EmployeeDossierPageComponent)
      },
      {
        path: 'dossiers/me', canActivate: [roleGuard], data: { roles: ['employe', 'manager'], self: true, title: 'Mon dossier administratif' },
        loadComponent: () => import('./pages/dossier/employee-dossier-page.component').then((page) => page.EmployeeDossierPageComponent)
      },
      {
        path: 'manager/equipe/conges',
        canActivate: [managerGuard],
        data: { title: 'Demandes de congé de mon équipe', leaveRole: 'manager' },
        loadComponent: () => import('./pages/leave/leave-approval-page.component').then((page) => page.LeaveApprovalPageComponent)
      },
      {
        path: 'manager/equipe/presence',
        canActivate: [managerGuard],
        data: { title: 'Présence de mon équipe' },
        loadComponent: () => import('./pages/planning/team-presence-page.component').then((page) => page.TeamPresencePageComponent)
      },
      {
        path: 'manager/equipe/discipline',
        canActivate: [managerGuard],
        data: { title: 'Discipline de mon équipe' },
        loadComponent: () => import('./pages/discipline/manager-discipline-page.component').then((page) => page.ManagerDisciplinePageComponent)
      },
      {
        path: 'manager/planning/pointage',
        canActivate: [managerGuard],
        data: { title: 'Pointage' },
        loadComponent: () => import('./pages/employee/time-clock-page.component').then((page) => page.TimeClockPageComponent)
      },
      {
        path: 'manager/planning/mon-planning',
        canActivate: [managerGuard],
        data: { title: 'Mon planning' },
        loadComponent: () => import('./pages/employee/employee-planning-page.component').then((page) => page.EmployeePlanningPageComponent)
      },
      {
        path: 'manager/planning/equipe',
        canActivate: [managerGuard],
        data: { title: 'Planning de mon équipe' },
        loadComponent: () => import('./pages/manager/team-planning-page.component').then((page) => page.TeamPlanningPageComponent)
      },
      {
        path: 'manager/futur/conges',
        canActivate: [managerGuard],
        data: { title: 'Congés' },
        loadComponent: () => import('./pages/placeholder/workspace-placeholder.component').then((page) => page.WorkspacePlaceholderComponent)
      },
      {
        path: 'manager/contrat',
        canActivate: [managerGuard],
        data: { title: 'Contrat' },
        loadComponent: () => import('./pages/placeholder/workspace-placeholder.component').then((page) => page.WorkspacePlaceholderComponent)
      },
      {
        path: 'manager/contrat/historique',
        canActivate: [managerGuard],
        data: { title: 'Historique du contrat' },
        loadComponent: () => import('./pages/placeholder/workspace-placeholder.component').then((page) => page.WorkspacePlaceholderComponent)
      },
      {
        path: 'manager/demandes/nouvelle',
        canActivate: [managerGuard],
        loadComponent: () => import('./pages/manager/manager-recruitment-page.component').then((page) => page.ManagerRecruitmentPageComponent)
      },
      ...rolePlaceholderRoutes('rh', [
        ['rh/contrats', 'Contrats'],
        ['rh/contrats/historique', 'Historique des contrats']
      ]),
      {
        path: 'rh/conges/demandes', canActivate: [roleGuard], data: { roles: ['rh'], title: 'Demandes de congés à traiter', leaveRole: 'rh' },
        loadComponent: () => import('./pages/leave/leave-approval-page.component').then((page) => page.LeaveApprovalPageComponent)
      },
      {
        path: 'rh/conges/parametres', canActivate: [roleGuard], data: { roles: ['rh'], title: 'Paramétrage congés et effectifs' },
        loadComponent: () => import('./pages/leave/leave-configuration-page.component').then((page) => page.LeaveConfigurationPageComponent)
      },
      {
        path: 'rh/conges/planning', canActivate: [roleGuard], data: { roles: ['rh'], title: 'Planning et absences' },
        loadComponent: () => import('./pages/leave/leave-absence-page.component').then((page) => page.LeaveAbsencePageComponent)
      },
      {
        path: 'rh/equipe/presences', canActivate: [roleGuard], data: { roles: ['rh'], area: 'rh', title: 'Présences de l’équipe' },
        loadComponent: () => import('./pages/planning/team-presence-page.component').then((page) => page.TeamPresencePageComponent)
      },
      {
        path: 'rh/discipline', canActivate: [roleGuard], data: { roles: ['rh'], title: 'Procédures disciplinaires' },
        loadComponent: () => import('./pages/discipline/rh-discipline-page.component').then((page) => page.RhDisciplinePageComponent)
      },
      {
        path: 'rh/planning', canActivate: [roleGuard], data: { roles: ['rh'], area: 'rh', title: 'Planning global' },
        loadComponent: () => import('./pages/planning/global-planning-page.component').then((page) => page.GlobalPlanningPageComponent)
      },
      {
        path: 'rh/equipe/effectifs', canActivate: [roleGuard], data: { roles: ['rh'] },
        loadComponent: () => import('./pages/dossier/employee-directory-page.component').then((page) => page.EmployeeDirectoryPageComponent)
      },
      {
        path: 'rh/personnel/employes/:employeeId', canActivate: [roleGuard], data: { roles: ['rh'] },
        loadComponent: () => import('./pages/dossier/employee-dossier-page.component').then((page) => page.EmployeeDossierPageComponent)
      },
      {
        path: 'rh/personnel/demandes', canActivate: [roleGuard], data: { roles: ['rh'] },
        loadComponent: () => import('./pages/dossier/employee-corrections-page.component').then((page) => page.EmployeeCorrectionsPageComponent)
      },
      {
        path: 'rh/personnel/alertes', canActivate: [roleGuard], data: { roles: ['rh'] },
        loadComponent: () => import('./pages/dossier/employee-alerts-page.component').then((page) => page.EmployeeAlertsPageComponent)
      },
      {
        path: 'rh/recrutement/offres',
        canActivate: [roleGuard],
        data: { roles: ['rh'], rhRecruitmentView: 'offers' },
        loadComponent: () => import('./pages/workflow/rh-candidatures-page.component').then((page) => page.RhCandidaturesPageComponent)
      },
      {
        path: 'rh/recrutement/demandes',
        canActivate: [roleGuard],
        data: { roles: ['rh'], workflowArea: 'rh' },
        loadComponent: () => import('./pages/workflow/recruitment-validation-page.component').then((page) => page.RecruitmentValidationPageComponent)
      },
      {
        path: 'rh/recrutement/candidatures',
        canActivate: [roleGuard],
        data: { roles: ['rh'], rhRecruitmentView: 'candidates' },
        loadComponent: () => import('./pages/workflow/rh-candidatures-page.component').then((page) => page.RhCandidaturesPageComponent)
      },
      {
        path: 'administration/paie/parametres', canActivate: [roleGuard],
        data: { roles: ['administration'], title: 'Paramètres de paie' },
        loadComponent: () => import('./pages/payroll/payroll-configuration-page.component').then((page) => page.PayrollConfigurationPageComponent)
      },
      {
        path: 'rh/paie/bulletins', canActivate: [roleGuard],
        data: { roles: ['rh'], title: 'Bulletins de paie', payrollView: 'rh' },
        loadComponent: () => import('./pages/payroll/payroll-bulletins-page.component').then((page) => page.PayrollBulletinsPageComponent)
      },
      {
        path: 'rh/paie/periodes', canActivate: [roleGuard],
        data: { roles: ['rh'], title: 'Préparation de la paie' },
        loadComponent: () => import('./pages/payroll/payroll-periods-page.component').then((page) => page.PayrollPeriodsPageComponent)
      },
      {
        path: 'employe/paie', canActivate: [roleGuard],
        data: { roles: ['employe', 'manager'], title: 'Mes bulletins de paie', payrollView: 'employee' },
        loadComponent: () => import('./pages/payroll/payroll-bulletins-page.component').then((page) => page.PayrollBulletinsPageComponent)
      },
      ...rolePlaceholderRoutes('administration', [
        ['administration/organisation/departements', 'Départements'],
        ['administration/organisation/postes', 'Postes'],
        ['administration/organisation/effectifs', 'Effectifs'],
        ['administration/recrutement/offres', 'Offres d’emploi'],
        ['administration/personnel/employes', 'Employés'],
        ['administration/personnel/contrats', 'Contrats'],
        ['administration/pilotage', 'Indicateurs et rapports']
      ]),
      {
        path: 'administration/personnel/planning', canActivate: [roleGuard], data: { roles: ['administration'], area: 'administration', title: 'Planning global' },
        loadComponent: () => import('./pages/planning/global-planning-page.component').then((page) => page.GlobalPlanningPageComponent)
      },
      {
        path: 'administration/personnel/presences', canActivate: [roleGuard], data: { roles: ['administration'], area: 'administration', title: 'Présences globales' },
        loadComponent: () => import('./pages/planning/team-presence-page.component').then((page) => page.TeamPresencePageComponent)
      },
      {
        path: 'administration/recrutement/demandes',
        canActivate: [roleGuard],
        data: { roles: ['administration'] },
        loadComponent: () => import('./pages/workflow/administration-offer-page.component').then((page) => page.AdministrationOfferPageComponent)
      },
      {
        path: 'finance/recrutement/demandes',
        canActivate: [financeDepartmentGuard],
        data: { workflowArea: 'finance' },
        loadComponent: () => import('./pages/workflow/recruitment-validation-page.component').then((page) => page.RecruitmentValidationPageComponent)
      },
      {
        path: 'gestionnaire/comptes', canActivate: [roleGuard], data: { roles: ['gestionnaire'] },
        loadComponent: () => import('./pages/accounts/account-list-page.component').then((page) => page.AccountListPageComponent)
      },
      {
        path: 'gestionnaire/comptes/nouveau', canActivate: [roleGuard], data: { roles: ['gestionnaire'] },
        loadComponent: () => import('./pages/accounts/account-create-page.component').then((page) => page.AccountCreatePageComponent)
      },
      {
        path: 'gestionnaire/comptes/inscriptions', canActivate: [roleGuard], data: { roles: ['gestionnaire'] },
        loadComponent: () => import('./pages/accounts/pending-registrations-page.component').then((page) => page.PendingRegistrationsPageComponent)
      },
      {
        path: 'gestionnaire/comptes/employes', canActivate: [roleGuard], data: { roles: ['gestionnaire'] },
        loadComponent: () => import('./pages/accounts/employee-directory-page.component').then((page) => page.EmployeeDirectoryPageComponent)
      },
      {
        path: 'gestionnaire/acces/roles', canActivate: [roleGuard], data: { roles: ['gestionnaire'] },
        loadComponent: () => import('./pages/accounts/account-list-page.component').then((page) => page.AccountListPageComponent)
      },
      {
        path: 'gestionnaire/acces/affectations', canActivate: [roleGuard], data: { roles: ['gestionnaire'] },
        loadComponent: () => import('./pages/accounts/account-list-page.component').then((page) => page.AccountListPageComponent)
      },
      {
        path: 'employe/pointage',
        canActivate: [roleGuard], data: { title: 'Pointage', roles: ['employe', 'manager'] },
        loadComponent: () => import('./pages/employee/time-clock-page.component').then((page) => page.TimeClockPageComponent)
      },
      {
        path: 'employe/planning',
        canActivate: [roleGuard], data: { title: 'Planning', roles: ['employe', 'manager'] },
        loadComponent: () => import('./pages/employee/employee-planning-page.component').then((page) => page.EmployeePlanningPageComponent)
      },
      {
        path: 'employe/conges', canActivate: [roleGuard],
        data: { title: 'Mes congés', roles: ['employe', 'manager'] },
        loadComponent: () => import('./pages/leave/employee-leave-page.component').then((page) => page.EmployeeLeavePageComponent)
      },
      {
        path: 'employe/discipline', canActivate: [roleGuard],
        data: { title: 'Mes procédures disciplinaires', roles: ['employe', 'manager'] },
        loadComponent: () => import('./pages/discipline/employee-discipline-page.component').then((page) => page.EmployeeDisciplinePageComponent)
      },
      {
        path: 'employe/contrat', canActivate: [roleGuard],
        data: { title: 'Mon contrat', roles: ['employe', 'manager'] },
        loadComponent: () => import('./pages/candidate/employee-contract-page.component').then((page) => page.EmployeeContractPageComponent)
      },
      {
        path: 'employe/historiques', canActivate: [roleGuard],
        data: { title: 'Historiques', roles: ['employe', 'manager'] },
        loadComponent: () => import('./pages/placeholder/workspace-placeholder.component').then((page) => page.WorkspacePlaceholderComponent)
      },
      {
        path: 'candidatures', canActivate: [roleGuard],
        data: { title: 'Mes candidatures', roles: ['simple_user'] },
        loadComponent: () => import('./pages/candidate/candidate-applications-page.component').then((page) => page.CandidateApplicationsPageComponent)
      },
      {
        path: 'notifications', canActivate: [roleGuard],
        data: { title: 'Notifications', roles: ['simple_user', 'employe', 'manager', 'rh', 'administration', 'gestionnaire'] },
        loadComponent: () => import('./pages/notifications/notifications-page.component').then((page) => page.NotificationsPageComponent)
      },
      {
        path: 'profil',
        data: { title: 'Mon profil' },
        loadComponent: () => import('./pages/profile/user-profile-page.component').then((page) => page.UserProfilePageComponent)
      },
      {
        path: '',
        loadComponent: () => import('./pages/home/home-page.component').then((page) => page.HomePageComponent)
      }
    ]
  }
];

function rolePlaceholderRoutes(role: string, pages: [string, string][]) {
  return pages.map(([path, title]) => ({
    path,
    canActivate: [roleGuard],
    data: { title, roles: [role] },
    loadComponent: () => import('./pages/placeholder/workspace-placeholder.component')
      .then((page) => page.WorkspacePlaceholderComponent)
  }));
}

