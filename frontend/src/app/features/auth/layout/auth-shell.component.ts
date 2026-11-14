import { Component, ViewEncapsulation } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastModule],
  templateUrl: './auth-shell.component.html',
  styleUrl: '../auth.scss',
  encapsulation: ViewEncapsulation.None
})
export class AuthShellComponent {}
