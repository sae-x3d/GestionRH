import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RoleService } from '../../../../core/auth/role.service';

@Component({
  selector: 'app-workspace-placeholder',
  standalone: true,
  template: `
    <section class="workspace-placeholder">
      <span class="workspace-placeholder-kicker">{{ roleLabel() }}</span>
      <h1>{{ title() }}<span>.</span></h1>
      <p>Cette rubrique sera disponible prochainement.</p>
    </section>
  `,
  styles: [`
    .workspace-placeholder { min-height: min(58vh, 560px); display: flex; flex-direction: column; align-items: flex-start; justify-content: center; }
    .workspace-placeholder-kicker { color: #777; font-size: 10px; font-weight: 700; letter-spacing: .18em; }
    h1 { margin: 13px 0 10px; color: #171717; font-size: clamp(42px, 8vw, 86px); font-weight: 450; line-height: 1; letter-spacing: -.07em; }
    h1 span { color: #176b63; }
    p { color: #777; font-size: 14px; }
  `]
})
export class WorkspacePlaceholderComponent {
  readonly title = signal('');
  readonly currentRole = inject(RoleService).currentRole;
  readonly roleLabel = () => ({
    manager: 'ESPACE MANAGER', employe: 'ESPACE EMPLOYÉ', rh: 'ESPACE RH',
    administration: 'ADMINISTRATION', gestionnaire: 'GESTION DES COMPTES', simple_user: 'ESPACE CANDIDAT'
  }[this.currentRole() ?? 'simple_user']);
  private readonly route = inject(ActivatedRoute);

  constructor() {
    this.route.data.subscribe((data) => this.title.set(data['title'] ?? 'Espace candidat'));
  }
}
