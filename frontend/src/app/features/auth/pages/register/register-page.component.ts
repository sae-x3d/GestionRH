import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../../../core/auth/auth.service';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../../core/notifications/notification.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, PasswordModule],
  templateUrl: './register-page.component.html'
})
export class RegisterPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  loading = false;

  readonly form = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9._-]+$/)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmPassword: ['', Validators.required],
    acceptTerms: [false, Validators.requiredTrue]
  });

  submit(): void {
    if (this.loading) {
      return;
    }
    const controls = this.form.controls;
    if (this.form.invalid || !controls.fullName.value.trim() || !controls.username.value.trim() || !controls.email.value.trim() || !controls.password.value.trim()) {
      this.form.markAllAsTouched();
      this.notifyValidationErrors();
      return;
    }

    if (this.form.controls.password.value !== this.form.controls.confirmPassword.value) {
      this.notifications.error('Inscription impossible', 'Les deux mots de passe doivent correspondre.');
      return;
    }

    const { fullName, username, email, password } = this.form.getRawValue();
    this.loading = true;
    this.authService.register({
      fullName: fullName.trim(),
      username: username.trim(),
      email: email.trim(),
      password
    }).pipe(finalize(() => this.loading = false)).subscribe({
      next: () => {
        this.notifications.success('Inscription réussie', 'Redirection vers la page de connexion…');
        void this.router.navigateByUrl('/connexion');
      },
      error: () => undefined
    });
  }

  private notifyValidationErrors(): void {
    const errors: string[] = [];
    const controls = this.form.controls;

    if (controls.fullName.hasError('required') || !controls.fullName.value.trim()) errors.push('Le nom complet est obligatoire.');
    else if (controls.fullName.hasError('maxlength')) errors.push('Le nom complet ne doit pas dépasser 150 caractères.');

    if (controls.username.hasError('required') || !controls.username.value.trim()) errors.push('Le nom d’utilisateur est obligatoire.');
    else if (controls.username.hasError('minlength')) errors.push('Le nom d’utilisateur doit contenir au moins 3 caractères.');
    else if (controls.username.hasError('maxlength')) errors.push('Le nom d’utilisateur ne doit pas dépasser 50 caractères.');
    else if (controls.username.hasError('pattern')) errors.push('Le nom d’utilisateur peut contenir seulement des lettres non accentuées, chiffres, points, tirets et traits de soulignement.');

    if (controls.email.hasError('required') || !controls.email.value.trim()) errors.push('L’adresse e-mail est obligatoire.');
    else if (controls.email.hasError('email')) errors.push('L’adresse e-mail n’est pas valide.');
    else if (controls.email.hasError('maxlength')) errors.push('L’adresse e-mail ne doit pas dépasser 254 caractères.');

    if (controls.password.hasError('required') || !controls.password.value.trim()) errors.push('Le mot de passe est obligatoire.');
    else if (controls.password.hasError('minlength')) errors.push('Le mot de passe doit contenir au moins 8 caractères.');
    else if (controls.password.hasError('maxlength')) errors.push('Le mot de passe ne doit pas dépasser 72 caractères.');

    if (controls.confirmPassword.hasError('required')) errors.push('La confirmation du mot de passe est obligatoire.');
    if (controls.acceptTerms.hasError('required')) errors.push('Tu dois accepter les conditions pour créer le compte.');

    this.notifications.error(
      errors.length === 1 ? 'Champ à corriger' : 'Informations à corriger',
      errors.join(' ')
    );
  }
}
