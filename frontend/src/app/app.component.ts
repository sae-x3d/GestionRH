import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ConfirmDialogModule],
  template: '<p-confirmDialog /><router-outlet />'
})
export class AppComponent {
  constructor(authService: AuthService) {
    authService.restoreCurrentSession();
  }
}
