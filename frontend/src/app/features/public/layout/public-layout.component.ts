import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LogoutConfirmationService } from '../../../core/auth/logout-confirmation.service';
import { RoleService } from '../../../core/auth/role.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastModule } from 'primeng/toast';

interface PublicNavLink {
  label: string;
  icon: string;
  route: string;
  exact?: boolean;
}

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastModule],
  templateUrl: './public-layout.component.html',
  styleUrl: './public-layout.component.scss'
})
export class PublicLayoutComponent {
  private readonly roleService = inject(RoleService);
  private readonly auth = inject(AuthService);
  private readonly logoutConfirmation = inject(LogoutConfirmationService);
  readonly navigationOpen = signal(false);
  readonly currentRole = this.roleService.currentRole;
  readonly currentSession = this.auth.currentSession;
  readonly sessionInitialized = this.auth.sessionInitialized;
  readonly isFinanceMember = this.roleService.isFinanceMember;
  readonly navLinks = computed<PublicNavLink[]>(() => {
    const role = this.currentRole();
    const common = [{ label: 'Offres d’emploi', icon: 'pi-briefcase', route: '/offres', exact: true }];
    switch (role) {
      case 'simple_user': return [
        ...common,
        { label: 'Mes candidatures', icon: 'pi-file-edit', route: '/espace/candidatures' },
        { label: 'Notifications', icon: 'pi-bell', route: '/espace/notifications' }
      ];
      case 'employe': return [
        { label: 'Mon tableau de bord', icon: 'pi-home', route: '/espace', exact: true },
        { label: 'Mon dossier', icon: 'pi-id-card', route: '/espace/dossiers/me' },
        { label: 'Pointage', icon: 'pi-clock', route: '/espace/employe/pointage' },
        { label: 'Mon planning', icon: 'pi-calendar-clock', route: '/espace/employe/planning' },
        { label: 'Mes congés', icon: 'pi-sun', route: '/espace/employe/conges' },
        ...common,
        { label: 'Mon contrat', icon: 'pi-file', route: '/espace/employe/contrat' },
        { label: 'Historiques', icon: 'pi-history', route: '/espace/employe/historiques' },
        { label: 'Notifications', icon: 'pi-bell', route: '/espace/notifications' }
      ];
      case 'manager': return [
        { label: 'Mon tableau de bord', icon: 'pi-home', route: '/espace', exact: true },
        { label: 'Mon dossier', icon: 'pi-id-card', route: '/espace/dossiers/me' },
        { label: 'Effectif de mon équipe', icon: 'pi-users', route: '/espace/manager/equipe/effectif' },
        { label: 'Demandes de recrutement', icon: 'pi-user-plus', route: '/espace/manager/demandes/nouvelle' },
        { label: 'Congés de mon équipe', icon: 'pi-calendar-plus', route: '/espace/manager/equipe/conges' },
        { label: 'Présence de mon équipe', icon: 'pi-clock', route: '/espace/manager/equipe/presence' },
        { label: 'Pointage', icon: 'pi-stopwatch', route: '/espace/manager/planning/pointage' },
        { label: 'Mon planning', icon: 'pi-calendar-clock', route: '/espace/manager/planning/mon-planning' },
        { label: 'Planning de mon équipe', icon: 'pi-calendar', route: '/espace/manager/planning/equipe' },
        { label: 'Congés', icon: 'pi-sun', route: '/espace/manager/futur/conges' },
        { label: 'Contrat', icon: 'pi-file', route: '/espace/manager/contrat' },
        { label: 'Historique des contrats', icon: 'pi-history', route: '/espace/manager/contrat/historique' },
        ...common,
        { label: 'Mes candidatures', icon: 'pi-file-edit', route: '/espace/candidatures' },
        { label: 'Notifications', icon: 'pi-bell', route: '/espace/notifications' }
      ];
      case 'rh': return [
        { label: 'Tableau de bord RH', icon: 'pi-home', route: '/espace', exact: true },
        { label: 'Demandes de recrutement', icon: 'pi-inbox', route: '/espace/rh/recrutement/demandes' },
        { label: 'Offres et candidatures', icon: 'pi-megaphone', route: '/espace/rh/recrutement/offres' },
        { label: 'Candidatures reçues', icon: 'pi-file-edit', route: '/espace/rh/recrutement/candidatures' },
        { label: 'Effectifs', icon: 'pi-users', route: '/espace/rh/equipe/effectifs' },
        { label: 'Dossiers salariés', icon: 'pi-id-card', route: '/espace/rh/personnel/demandes' },
        { label: 'Alertes administratives', icon: 'pi-bell', route: '/espace/rh/personnel/alertes' },
        { label: 'Présences', icon: 'pi-clock', route: '/espace/rh/equipe/presences' },
        { label: 'Planning global', icon: 'pi-calendar-clock', route: '/espace/rh/planning' },
        { label: 'Demandes de congé', icon: 'pi-calendar-plus', route: '/espace/rh/conges/demandes' },
        { label: 'Planning des absences', icon: 'pi-calendar', route: '/espace/rh/conges/planning' },
        { label: 'Paramètres congés et effectifs', icon: 'pi-sliders-h', route: '/espace/rh/conges/parametres' },
        { label: 'Contrats', icon: 'pi-file', route: '/espace/rh/contrats' },
        { label: 'Historique des contrats', icon: 'pi-history', route: '/espace/rh/contrats/historique' },
        ...common,
        { label: 'Notifications', icon: 'pi-bell', route: '/espace/notifications' }
      ];
      case 'administration': return [
        { label: 'Administration', icon: 'pi-home', route: '/espace', exact: true },
        { label: 'Départements', icon: 'pi-building', route: '/espace/administration/organisation/departements' },
        { label: 'Postes', icon: 'pi-briefcase', route: '/espace/administration/organisation/postes' },
        { label: 'Effectifs', icon: 'pi-users', route: '/espace/administration/organisation/effectifs' },
        { label: 'Demandes de recrutement', icon: 'pi-inbox', route: '/espace/administration/recrutement/demandes' },
        { label: 'Offres d’emploi', icon: 'pi-megaphone', route: '/espace/administration/recrutement/offres' },
        { label: 'Employés', icon: 'pi-id-card', route: '/espace/administration/personnel/employes' },
        { label: 'Planning global', icon: 'pi-calendar-clock', route: '/espace/administration/personnel/planning' },
        { label: 'Présences globales', icon: 'pi-clock', route: '/espace/administration/personnel/presences' },
        { label: 'Contrats', icon: 'pi-file', route: '/espace/administration/personnel/contrats' },
        { label: 'Indicateurs et rapports', icon: 'pi-chart-line', route: '/espace/administration/pilotage' },
        ...common,
        { label: 'Notifications', icon: 'pi-bell', route: '/espace/notifications' }
      ];
      case 'gestionnaire': return [
        { label: 'Gestion des comptes', icon: 'pi-home', route: '/espace', exact: true },
        { label: 'Liste des comptes', icon: 'pi-list', route: '/espace/gestionnaire/comptes' },
        { label: 'Liste des employés', icon: 'pi-id-card', route: '/espace/gestionnaire/comptes/employes' },
        { label: 'Créer un compte', icon: 'pi-user-plus', route: '/espace/gestionnaire/comptes/nouveau' },
        { label: 'Inscriptions à traiter', icon: 'pi-inbox', route: '/espace/gestionnaire/comptes/inscriptions' },
        { label: 'Rôles', icon: 'pi-shield', route: '/espace/gestionnaire/acces/roles' },
        { label: 'Affectations aux départements', icon: 'pi-sitemap', route: '/espace/gestionnaire/acces/affectations' },
        ...common,
        { label: 'Notifications', icon: 'pi-bell', route: '/espace/notifications' }
      ];
      default: return common;
    }
  });
  photoLoadFailed = false;

  onPhotoError(): void { this.photoLoadFailed = true; }

  toggleNavigation(): void {
    this.navigationOpen.update((isOpen) => !isOpen);
  }

  closeNavigation(): void {
    this.navigationOpen.set(false);
  }

  logout(): void {
    this.logoutConfirmation.confirm();
  }
}
