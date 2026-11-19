import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RoleService } from '../../../../core/auth/role.service';
import { InboxNotificationService } from '../../../../core/notifications/inbox-notification.service';
import { CandidateApplicationService } from '../../../public/services/candidate-application.service';
import { PublicOffersService } from '../../../public/services/public-offers.service';
import { AccountManagementService } from '../../services/account-management.service';
import { EmployeeDossierService } from '../../services/employee-dossier.service';
import { LeaveManagementService } from '../../services/leave-management.service';
import { ManagerRecruitmentService } from '../../services/manager-recruitment.service';
import { PayrollPayslipsService } from '../../services/payroll-payslips.service';
import { RecruitmentWorkflowService } from '../../services/recruitment-workflow.service';

interface DashboardMetric { id: string; label: string; value: number | string; detail: string; icon: string; }
interface DashboardShortcut { label: string; description: string; route: string; icon: string; }

@Component({
  selector: 'app-home-page', standalone: true, imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './home-page.component.html', styleUrl: './home-page.component.scss'
})
export class HomePageComponent implements OnInit {
  private readonly roleService = inject(RoleService);
  private readonly offers = inject(PublicOffersService);
  private readonly applications = inject(CandidateApplicationService);
  private readonly leave = inject(LeaveManagementService);
  private readonly recruitment = inject(ManagerRecruitmentService);
  private readonly workflow = inject(RecruitmentWorkflowService);
  private readonly accounts = inject(AccountManagementService);
  private readonly dossiers = inject(EmployeeDossierService);
  private readonly payroll = inject(PayrollPayslipsService);
  private readonly inbox = inject(InboxNotificationService);

  readonly currentRole = this.roleService.currentRole;
  readonly today = new Date();
  metrics: DashboardMetric[] = [];

  get title(): string {
    const labels: Record<string, string> = {
      simple_user: 'Espace candidat', employe: 'Mon tableau de bord', manager: 'Mon tableau de bord',
      rh: 'Pilotage des ressources humaines', administration: 'Vue générale', gestionnaire: 'Gestion des comptes'
    };
    return labels[this.currentRole() ?? ''] ?? 'Mon espace';
  }

  get introduction(): string {
    return ({
      simple_user: 'Retrouvez les offres ouvertes et suivez vos candidatures.',
      employe: 'Vos informations utiles pour organiser votre semaine et suivre votre situation.',
      manager: 'Les priorites de votre equipe et les decisions qui attendent votre attention.',
      rh: 'Les recrutements, conges et dossiers à traiter dans les differents departements.',
      administration: 'Suivez les besoins à transformer en offres et l’activite globale.',
      gestionnaire: 'Suivez les comptes à verifier et accedez rapidement aux annuaires.'
    } as Record<string, string>)[this.currentRole() ?? ''] ?? 'Accedez rapidement à vos outils.';
  }

  get shortcuts(): DashboardShortcut[] {
    const links: Record<string, DashboardShortcut[]> = {
      simple_user: [
        { label: 'Offres d’emploi', description: 'Voir les postes ouverts', route: '/offres', icon: 'pi-briefcase' },
        { label: 'Mes candidatures', description: 'Suivre les reponses des RH', route: '/espace/candidatures', icon: 'pi-file-edit' },
        { label: 'Mon profil', description: 'Mettre à jour vos coordonnees', route: '/espace/profil', icon: 'pi-user' },
        { label: 'Notifications', description: 'Voir les dernières nouvelles', route: '/espace/notifications', icon: 'pi-bell' }
      ],
      employe: [
        { label: 'Mon planning', description: 'Consulter la semaine en cours', route: '/espace/employe/planning', icon: 'pi-calendar' },
        { label: 'Pointer', description: 'Enregistrer une arrivee ou une pause', route: '/espace/employe/pointage', icon: 'pi-clock' },
        { label: 'Mes conges', description: 'Consulter le solde ou deposer une demande', route: '/espace/employe/conges', icon: 'pi-sun' },
        { label: 'Mon dossier', description: 'Consulter les documents administratifs', route: '/espace/dossiers/me', icon: 'pi-id-card' },
        { label: 'Mes bulletins', description: 'Acceder aux fiches de paie', route: '/espace/employe/paie', icon: 'pi-wallet' }
      ],
      manager: [
        { label: 'Valider les conges', description: 'Examiner les demandes de votre equipe', route: '/espace/manager/equipe/conges', icon: 'pi-sun' },
        { label: 'Demande de recrutement', description: 'Transmettre un besoin du departement', route: '/espace/manager/demandes/nouvelle', icon: 'pi-user-plus' },
        { label: 'Planning de l’equipe', description: 'Organiser la semaine à venir', route: '/espace/manager/planning/equipe', icon: 'pi-calendar' },
        { label: 'Presence de l’equipe', description: 'Voir les arrivees et les anomalies', route: '/espace/manager/equipe/presence', icon: 'pi-clock' },
        { label: 'Effectif', description: 'Consulter les salaries du departement', route: '/espace/manager/equipe/effectif', icon: 'pi-users' }
      ],
      rh: [
        { label: 'Demandes de recrutement', description: 'Traiter la file de validation RH', route: '/espace/rh/recrutement/demandes', icon: 'pi-user-plus' },
        { label: 'Candidatures', description: 'Examiner les dossiers reçus', route: '/espace/rh/recrutement/candidatures', icon: 'pi-briefcase' },
        { label: 'Conges à valider', description: 'Examiner les demandes transmises', route: '/espace/rh/conges/demandes', icon: 'pi-sun' },
        { label: 'Dossiers salaries', description: 'Rechercher un salarie ou ses pièces', route: '/espace/rh/equipe/effectifs', icon: 'pi-id-card' },
        { label: 'Planning global', description: 'Voir les equipes et absences', route: '/espace/rh/planning', icon: 'pi-calendar' },
        { label: 'Presences', description: 'Suivre les pointages', route: '/espace/rh/equipe/presences', icon: 'pi-clock' }
      ],
      administration: [
        { label: 'Creer les offres', description: 'Publier les besoins approuves', route: '/espace/administration/recrutement/demandes', icon: 'pi-megaphone' },
        { label: 'Offres d’emploi', description: 'Consulter les offres et leur historique', route: '/espace/offres', icon: 'pi-briefcase' },
        { label: 'Planning global', description: 'Voir l’activite previsionnelle', route: '/espace/administration/personnel/planning', icon: 'pi-calendar' },
        { label: 'Presences globales', description: 'Consulter les pointages par departement', route: '/espace/administration/personnel/presences', icon: 'pi-clock' },
        { label: 'Paramètres de paie', description: 'Gerer les règles de paie', route: '/espace/administration/paie/parametres', icon: 'pi-wallet' }
      ],
      gestionnaire: [
        { label: 'Inscriptions à verifier', description: 'Examiner les profils en attente', route: '/espace/gestionnaire/comptes/inscriptions', icon: 'pi-user-edit' },
        { label: 'Comptes utilisateurs', description: 'Gerer rôles et accès', route: '/espace/gestionnaire/comptes', icon: 'pi-users' },
        { label: 'Annuaire des salaries', description: 'Rechercher les employes', route: '/espace/gestionnaire/comptes/employes', icon: 'pi-id-card' },
        { label: 'Creer un compte', description: 'Ajouter un compte interne', route: '/espace/gestionnaire/comptes/nouveau', icon: 'pi-user-plus' },
        { label: 'Notifications', description: 'Voir les alertes reçues', route: '/espace/notifications', icon: 'pi-bell' }
      ]
    };
    return links[this.currentRole() ?? ''] ?? [];
  }

  ngOnInit(): void {
    this.metrics = [
      { id: 'primary', label: 'À traiter', value: '—', detail: 'Chargement des donnees', icon: 'pi-inbox' },
      { id: 'secondary', label: 'Suivi', value: '—', detail: 'Chargement des donnees', icon: 'pi-chart-bar' },
      { id: 'tertiary', label: 'Effectif', value: '—', detail: 'Chargement des donnees', icon: 'pi-users' },
      { id: 'notifications', label: 'Notifications non lues', value: '—', detail: 'Dernières informations', icon: 'pi-bell' }
    ];
    this.inbox.getUnreadCount().subscribe({ next: (count) => this.setMetric('notifications', count, count ? 'À consulter' : 'Tout est à jour') });
    this.loadForRole();
  }

  private loadForRole(): void {
    switch (this.currentRole()) {
      case 'simple_user':
        this.offers.list().subscribe({ next: (items) => this.setMetric('primary', items.length, 'Offres actuellement ouvertes') });
        this.applications.myApplications().subscribe({ next: (items) => this.setMetric('secondary', items.length, 'Candidatures dans votre espace') });
        break;
      case 'employe':
        this.leave.getEmployeeOverview().subscribe({ next: (data) => {
          const pending = data.demandes.filter((item) => ['A_VALIDER_MANAGER', 'A_VALIDER_RH'].includes(item.statutCode)).length;
          const balance = data.soldes.reduce((sum, item) => sum + Number(item.solde), 0);
          this.setMetric('primary', pending, 'Demandes de conge en cours');
          this.setMetric('secondary', `${balance.toLocaleString('fr-FR')} j`, 'Solde cumule des conges');
        } });
        this.payroll.own().subscribe({ next: (items) => this.setMetric('tertiary', items.length, 'Bulletins disponibles') });
        break;
      case 'manager':
        this.leave.getApprovalQueue('manager').subscribe({ next: (items) => this.setMetric('primary', items.length, 'Demandes de conge en attente') });
        this.recruitment.getRequests().subscribe({ next: (items) => this.setMetric('secondary', items.length, 'Demandes de recrutement du departement') });
        this.dossiers.directory(true, { page: 0, size: 1 }).subscribe({ next: (data) => this.setMetric('tertiary', data.totalElements, 'Salaries dans votre departement') });
        break;
      case 'rh':
        this.leave.getApprovalQueue('rh').subscribe({ next: (items) => this.setMetric('primary', items.length, 'Demandes de conge à valider') });
        this.workflow.pending('rh').subscribe({ next: (items) => this.setMetric('secondary', items.length, 'Demandes de recrutement à examiner') });
        this.applications.receivedByRh().subscribe({ next: (items) => this.setMetric('tertiary', items.length, 'Candidatures reçues') });
        break;
      case 'administration':
        this.workflow.pending('administration').subscribe({ next: (items) => this.setMetric('primary', items.length, 'Demandes prêtes pour une offre') });
        this.offers.list().subscribe({ next: (items) => {
          this.setMetric('secondary', items.length, 'Offres actuellement en cours');
          this.setMetric('tertiary', items.reduce((sum, item) => sum + item.effectifRestant, 0), 'Postes encore à pourvoir');
        } });
        break;
      case 'gestionnaire':
        this.accounts.pending().subscribe({ next: (items) => this.setMetric('primary', items.length, 'Inscriptions en attente de decision') });
        this.accounts.list().subscribe({ next: (items) => this.setMetric('secondary', items.length, 'Comptes utilisateurs enregistres') });
        this.accounts.employees().subscribe({ next: (items) => this.setMetric('tertiary', items.length, 'Salaries references') });
        break;
    }
  }

  private setMetric(id: string, value: number | string, detail: string): void {
    this.metrics = this.metrics.map((metric) => metric.id === id ? { ...metric, value, detail } : metric);
  }
}
