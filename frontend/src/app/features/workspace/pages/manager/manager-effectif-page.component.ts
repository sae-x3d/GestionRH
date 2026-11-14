import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-manager-effectif-page',
  standalone: true,
  imports: [RouterLink, ButtonModule],
  template: `
    <section aria-labelledby="manager-effectif-title">
      <p>MON ÉQUIPE</p>
      <h1 id="manager-effectif-title">Effectif</h1>
      <p>La gestion des effectifs sera disponible dans cette rubrique.</p>
      <a pButton routerLink="/espace/manager/demandes/nouvelle" label="Créer une demande de recrutement" icon="pi pi-plus"></a>
    </section>
  `
})
export class ManagerEffectifPageComponent { }
