import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { UserProfile, UserProfileService } from '../../services/user-profile.service';

@Component({
  selector: 'app-user-profile-page', standalone: true, imports: [FormsModule],
  templateUrl: './user-profile-page.component.html', styleUrl: './user-profile-page.component.scss'
})
export class UserProfilePageComponent implements OnInit {
  private readonly service = inject(UserProfileService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  profile: UserProfile | null = null;
  fullName = ''; phoneNumber = ''; region = '';
  photo: File | null = null; photoPreview: string | null = null;
  loading = true; saving = false; requesting = false;

  ngOnInit(): void { this.load(); }

  selectPhoto(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    if (file.type !== 'image/png' || !file.name.toLowerCase().endsWith('.png')) {
      this.notifications.error('Format non accepté', 'La photo de profil doit être une image PNG.'); return;
    }
    if (file.size > 4 * 1024 * 1024) { this.notifications.error('Fichier trop volumineux', 'La photo ne doit pas dépasser 4 Mo.'); return; }
    this.photo = file;
    const reader = new FileReader(); reader.onload = () => this.photoPreview = String(reader.result); reader.readAsDataURL(file);
  }

  save(): void {
    if (this.saving || !this.profile?.editable) return;
    const name = this.fullName.trim(); const phone = this.phoneNumber.trim(); const region = this.region.trim();
    if (!name || !phone || !region) { this.notifications.error('Profil incomplet', 'Renseignez votre nom complet, votre téléphone et votre région.'); return; }
    if (!/^[+0-9() .-]{7,30}$/.test(phone)) { this.notifications.error('Téléphone invalide', 'Utilisez un numéro de téléphone valide (7 à 30 caractères).'); return; }
    const data = new FormData(); data.set('fullName', name); data.set('phoneNumber', phone); data.set('region', region);
    if (this.photo) data.set('photo', this.photo);
    this.saving = true;
    this.service.update(data).pipe(finalize(() => this.saving = false)).subscribe({
      next: (profile) => { this.setProfile(profile); this.photo = null; this.photoPreview = null; this.notifications.success('Profil enregistré', 'Vos informations personnelles ont été mises à jour.'); },
      error: () => this.notifications.error('Enregistrement impossible', 'Vérifiez les informations et le format PNG de votre photo, puis réessayez.')
    });
  }

  requestVerification(): void {
    if (this.requesting || !this.profile?.editable) return;
    if (!this.profile.fullName?.trim() || !this.profile.phoneNumber?.trim() || !this.profile.region?.trim() || !this.profile.photoUrl) {
      this.notifications.error('Profil incomplet', 'Enregistrez votre nom complet, votre téléphone, votre région et votre photo avant la demande.'); return;
    }
    this.requesting = true;
    this.service.requestVerification().pipe(finalize(() => this.requesting = false)).subscribe({
      next: (profile) => { this.setProfile(profile); this.auth.refreshCurrentSession().subscribe({ error: () => undefined }); this.notifications.success('Demande envoyée', 'Le gestionnaire examinera votre profil et vous recevrez une notification après sa décision.'); },
      error: () => this.notifications.error('Demande impossible', 'Complétez les quatre informations obligatoires et enregistrez le profil avant de réessayer.')
    });
  }

  private load(): void {
    this.loading = true;
    this.service.profile().pipe(finalize(() => this.loading = false)).subscribe({
      next: (profile) => this.setProfile(profile),
      error: () => this.notifications.error('Profil indisponible', 'Impossible de charger votre profil. Actualisez la page ou reconnectez-vous.')
    });
  }

  private setProfile(profile: UserProfile): void {
    this.profile = profile; this.fullName = profile.fullName ?? ''; this.phoneNumber = profile.phoneNumber ?? ''; this.region = profile.region ?? '';
  }
}
