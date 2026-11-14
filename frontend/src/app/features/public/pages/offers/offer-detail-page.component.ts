import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PublicOffer, PublicOffersService } from '../../services/public-offers.service';

@Component({
  selector: 'app-offer-detail-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './offer-detail-page.component.html',
  styleUrl: './offer-detail-page.component.scss'
})
export class OfferDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly offersService = inject(PublicOffersService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  offer: PublicOffer | null = null;
  loading = true;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      if (!Number.isInteger(id) || id < 1) { this.loading = false; return; }
      this.loading = true;
      this.offersService.get(id).subscribe({
        next: (offer) => { this.offer = offer; this.loading = false; },
        error: () => { this.offer = null; this.loading = false; }
      });
    });
  }

  apply(): void {
    if (!this.offer) return;
    const offerId = this.offer.id;
    this.auth.ensureCurrentSession().subscribe((session) => {
      if (!session) {
        void this.router.navigateByUrl('/connexion');
        return;
      }
      if (!session.roles.includes('simple_user')) {
        this.notifications.error('Espace candidat', 'Seuls les comptes candidats peuvent postuler à cette offre.');
        return;
      }
      if (session.accountValidationStatus !== 'APPROVED') {
        this.notifications.error('Compte en attente', 'Vous pourrez postuler après la validation de votre compte.');
        return;
      }
      void this.router.navigate(['/offres', offerId, 'postuler']);
    });
  }
}
