import { Component, inject } from '@angular/core';
import { RoleService } from '../../../../core/auth/role.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss'
})
export class HomePageComponent {
  readonly currentRole = inject(RoleService).currentRole;
}
