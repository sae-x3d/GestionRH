import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NotificationService } from '../../../../core/notifications/notification.service';
import {
  CareerEmployeeSummary,
  CareerFile,
  CareerMovementRequest,
  CareerPostOption,
  CareerService,
  CareerSituation
} from '../../services/career.service';

type CareerMode = 'rh' | 'manager' | 'self';
type MovementKind = 'promotion' | 'mutation' | 'changement-poste' | 'changement-salaire';

@Component({
  selector: 'app-career-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './career-page.component.html',
  styleUrl: './career-page.component.scss'
})
export class CareerPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CareerService);
  private readonly notifications = inject(NotificationService);

  readonly mode: CareerMode = (this.route.snapshot.data['careerMode'] as CareerMode | undefined) ?? 'self';

  employees: CareerEmployeeSummary[] = [];
  selected: CareerFile | null = null;
  selectedEmployeeId: number | null = null;
  situation: CareerSituation | null = null;
  situationDate = this.today();
  maxDate = this.today();
  search = '';
  loadingDirectory = false;
  loadingCareer = false;
  loadingSituation = false;
  submitting = false;
  activeMovement: MovementKind | null = null;
  formPostId: number | null = null;
  formSalary: number | null = null;
  formDate = this.today();
  formMotif = '';
  errorMessage = '';

  get isRh(): boolean { return this.mode === 'rh'; }
  get isManager(): boolean { return this.mode === 'manager'; }
  get isSelf(): boolean { return this.mode === 'self'; }
  get salaryVisible(): boolean { return this.selected?.salaireVisible === true; }
  get currentPostId(): number | null { return this.selected?.employe.posteId ?? null; }
  get currentDepartmentId(): number | null { return this.selected?.employe.departementId ?? null; }
  get currentSalary(): number | null { return this.selected?.employe.salaireBase ?? null; }
  get posts(): CareerPostOption[] { return this.selected?.postesDisponibles ?? []; }
  get selectedPost(): CareerPostOption | null {
    return this.posts.find((post) => post.id === this.formPostId) ?? null;
  }
  get availablePosts(): CareerPostOption[] {
    if (!this.activeMovement) return [];
    const current = this.currentSalary;
    return this.posts.filter((post) => {
      if (post.id === this.currentPostId) return false;
      if (this.activeMovement === 'mutation') return post.departement_id !== this.currentDepartmentId;
      if (post.departement_id !== this.currentDepartmentId) return false;
      if (this.activeMovement === 'changement-poste') {
        return current !== null && current >= post.salaire_base_min && current <= post.salaire_base_max;
      }
      return current === null || post.salaire_base_max > current;
    });
  }
  get headingEyebrow(): string {
    if (this.isRh) return 'RESSOURCES HUMAINES / CARRIÈRE';
    if (this.isManager) return 'MON ÉQUIPE / CARRIÈRE';
    return 'MON ESPACE / CARRIÈRE';
  }
  get headingTitle(): string {
    if (this.isRh) return 'Gestion de carrière';
    if (this.isManager) return 'Carrière de mon équipe';
    return 'Ma carrière';
  }
  get headingIntro(): string {
    if (this.isRh) return 'Enregistrez les promotions, mutations, changements de poste et de salaire. Chaque mouvement est historisé et ne remplace jamais les données précédentes.';
    if (this.isManager) return 'Consultez l’évolution professionnelle des salariés de votre département. Les rémunérations ne sont pas affichées à ce niveau.';
    return 'Consultez votre parcours professionnel, vos affectations et l’évolution de votre rémunération à la date de votre choix.';
  }
  get authorityLabel(): string {
    if (this.isRh) return 'Ressources humaines';
    if (this.isManager) return 'Lecture seule';
    return 'Mon dossier';
  }
  get movementTitle(): string {
    switch (this.activeMovement) {
      case 'promotion': return 'Enregistrer une promotion';
      case 'mutation': return 'Enregistrer une mutation';
      case 'changement-poste': return 'Changer le poste';
      case 'changement-salaire': return 'Changer le salaire de base';
      default: return '';
    }
  }
  get movementHint(): string {
    switch (this.activeMovement) {
      case 'promotion': return 'Même département, nouveau poste et salaire strictement supérieur au salaire actuel.';
      case 'mutation': return 'Nouveau département et nouveau poste. Le salaire peut rester inchangé ou être ajusté dans la fourchette du poste cible.';
      case 'changement-poste': return 'Même département et nouveau poste, sans modification de salaire.';
      case 'changement-salaire': return 'Même poste, nouveau salaire dans la fourchette du poste actuel.';
      default: return '';
    }
  }

  ngOnInit(): void {
    this.formDate = this.today();
    this.situationDate = this.today();
    this.maxDate = this.today();
    if (this.isSelf) this.loadOwnCareer();
    else this.loadDirectory();
  }

  loadDirectory(): void {
    this.loadingDirectory = true;
    const request = this.isRh ? this.service.rhEmployees(this.search) : this.service.managerTeam(this.search);
    request.subscribe({
      next: (rows) => {
        this.employees = rows;
        this.loadingDirectory = false;
        if (this.selected && !rows.some((row) => row.id === this.selectedEmployeeId)) this.clearSelection();
      },
      error: () => {
        this.loadingDirectory = false;
        this.errorMessage = 'Impossible de charger les salariés autorisés pour votre compte.';
      }
    });
  }

  selectEmployee(employee: CareerEmployeeSummary): void {
    if (this.selectedEmployeeId === employee.id) return;
    this.selectedEmployeeId = employee.id;
    this.selected = null;
    this.situation = null;
    this.situationDate = this.today();
    this.cancelMovement();
    this.loadingCareer = true;
    const request = this.isRh ? this.service.rhCareer(employee.id) : this.service.managerCareer(employee.id);
    request.subscribe({
      next: (file) => { this.selected = file; this.loadingCareer = false; this.errorMessage = ''; },
      error: () => {
        this.loadingCareer = false;
        this.errorMessage = 'Impossible de charger le dossier de carrière de ce salarié.';
      }
    });
  }

  querySituation(): void {
    if (!this.situationDate) {
      this.notifications.error('Date manquante', 'Choisissez une date de consultation.');
      return;
    }
    if (!this.isSelf && this.selectedEmployeeId === null) return;
    this.loadingSituation = true;
    const request = this.isSelf
      ? this.service.ownSituation(this.situationDate)
      : this.isRh
        ? this.service.rhSituation(this.selectedEmployeeId!, this.situationDate)
        : this.service.managerSituation(this.selectedEmployeeId!, this.situationDate);
    request.subscribe({
      next: (situation) => { this.situation = situation; this.loadingSituation = false; },
      error: () => { this.loadingSituation = false; }
    });
  }

  openMovement(kind: MovementKind): void {
    this.activeMovement = kind;
    this.formPostId = null;
    this.formSalary = kind === 'changement-salaire' ? this.currentSalary : null;
    this.formDate = this.today();
    this.formMotif = '';
  }

  cancelMovement(): void {
    this.activeMovement = null;
    this.formPostId = null;
    this.formSalary = null;
    this.formMotif = '';
    this.formDate = this.today();
  }

  onPostSelected(): void {
    const post = this.selectedPost;
    if (!post || this.activeMovement === 'changement-salaire') return;
    if (this.activeMovement === 'mutation') {
      this.formSalary = null;
      return;
    }
    const current = this.currentSalary;
    let proposal = current ?? post.salaire_base_min;
    if (proposal < post.salaire_base_min) proposal = post.salaire_base_min;
    if (proposal > post.salaire_base_max) proposal = post.salaire_base_max;
    if (this.activeMovement === 'promotion' && current !== null && proposal <= current) {
      proposal = Math.min(post.salaire_base_max, current + 1);
    }
    this.formSalary = proposal;
  }

  submitMovement(): void {
    if (!this.selected || !this.activeMovement) return;
    const kind = this.activeMovement;
    if (this.formDate > this.today()) {
      this.notifications.error('Date invalide', 'La date d’effet ne peut pas être dans le futur.');
      return;
    }
    if (!this.formMotif.trim()) {
      this.notifications.error('Motif obligatoire', 'Expliquez la décision avant de l’enregistrer.');
      return;
    }
    if (kind !== 'changement-salaire' && !this.formPostId) {
      this.notifications.error('Poste manquant', 'Sélectionnez le poste cible.');
      return;
    }
    if ((kind === 'promotion' || kind === 'changement-salaire') && !this.formSalary) {
      this.notifications.error('Salaire manquant', 'Saisissez le nouveau salaire de base.');
      return;
    }

    const request: CareerMovementRequest = {
      employeId: this.selected.employe.id,
      dateEffet: this.formDate,
      motif: this.formMotif.trim()
    };
    if (kind !== 'changement-salaire') request.nouveauPosteId = this.formPostId!;
    if (kind !== 'changement-poste' && this.formSalary !== null && this.formSalary !== undefined) {
      request.nouveauSalaireBase = Number(this.formSalary);
    }

    const call = kind === 'promotion'
      ? this.service.promotion(request)
      : kind === 'mutation'
        ? this.service.mutation(request)
        : kind === 'changement-poste'
          ? this.service.changementPoste(request)
          : this.service.changementSalaire(request);

    this.submitting = true;
    call.subscribe({
      next: (file) => {
        this.selected = file;
        this.submitting = false;
        this.cancelMovement();
        this.situation = null;
        this.notifications.success('Mouvement enregistré', 'La carrière du salarié est mise à jour et l’historique est conservé.');
        this.loadDirectory();
      },
      error: () => { this.submitting = false; }
    });
  }

  private loadOwnCareer(): void {
    this.loadingCareer = true;
    this.service.ownCareer().subscribe({
      next: (file) => { this.selected = file; this.loadingCareer = false; this.errorMessage = ''; },
      error: () => {
        this.loadingCareer = false;
        this.errorMessage = 'Aucun dossier de carrière n’est accessible avec ce compte.';
      }
    });
  }

  private clearSelection(): void {
    this.selected = null;
    this.selectedEmployeeId = null;
    this.situation = null;
    this.cancelMovement();
  }

  private today(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }
}
