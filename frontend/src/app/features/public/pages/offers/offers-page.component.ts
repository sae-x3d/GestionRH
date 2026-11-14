import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PublicOffer, PublicOffersService } from '../../services/public-offers.service';

@Component({
  selector: 'app-offers-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './offers-page.component.html',
  styleUrl: './offers-page.component.scss'
})
export class OffersPageComponent implements OnInit {
  private readonly offersService = inject(PublicOffersService);
  offers: PublicOffer[] = [];
  loading = true;

  ngOnInit(): void {
    this.offersService.list().subscribe({
      next: (offers) => { this.offers = offers; this.loading = false; },
      error: () => this.loading = false
    });
  }
}
