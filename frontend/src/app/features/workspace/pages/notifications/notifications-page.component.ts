import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { InboxNotification, InboxNotificationService } from '../../../../core/notifications/inbox-notification.service';

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [DatePipe],
  template: `
    <main class="notifications-page">
      <header class="notifications-heading"><div><p class="eyebrow">ESPACE PERSONNEL <span>/</span> ALERTES</p><h1>Notifications</h1><p>Retrouvez les changements de statut et les actions qui attendent votre intervention.</p></div><span class="unread-pill">{{ unreadCount }} non lue{{ unreadCount === 1 ? '' : 's' }}</span></header>
      <section class="notifications-panel" aria-label="Liste des notifications">
        @if (loading) {
          <div class="empty-state" role="status"><i class="pi pi-spin pi-spinner" aria-hidden="true"></i> Chargement des notifications…</div>
        } @else if (items.length === 0) {
          <div class="empty-state"><i class="pi pi-bell-slash" aria-hidden="true"></i><strong>Aucune notification</strong><span>Vous êtes à jour.</span></div>
        } @else {
          @for (item of items; track item.id) {
            <article class="notification-row" [class.unread]="!item.lue">
              <button class="notification-open" type="button" (click)="open(item)">
                <span class="notification-icon"><i class="pi" [class.pi-briefcase]="item.type.includes('DEMANDE') || item.type.includes('OFFRE')" [class.pi-info-circle]="!item.type.includes('DEMANDE') && !item.type.includes('OFFRE')" aria-hidden="true"></i></span>
                <span class="notification-copy"><span class="notification-title">{{ item.titre }}</span><span class="notification-summary">{{ item.resume }}</span><time>{{ item.createdAt | date:'dd/MM/yyyy, HH:mm' }}</time></span>
                <span class="open-link">Ouvrir <i class="pi pi-arrow-up-right" aria-hidden="true"></i></span>
              </button>
              <button class="delete-notification" type="button" aria-label="Supprimer cette notification" title="Supprimer" (click)="remove(item, $event)"><i class="pi pi-trash" aria-hidden="true"></i></button>
            </article>
          }
        }
      </section>
    </main>
  `,
  styles: [`
    :host { display:block; min-width:0; color:#1b2924; }
    .notifications-page { width:100%; max-width:none; margin:0; padding:0; }
    .notifications-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; margin-bottom:26px; }
    .eyebrow { margin:0 0 8px; color:#57766a; font-size:10px; font-weight:750; letter-spacing:.15em; }
    .eyebrow span { padding:0 5px; color:#a4b2ac; }
    h1 { margin:0; color:#1c2923; font-size:clamp(26px,3vw,36px); line-height:1.12; letter-spacing:-.045em; }
    .notifications-heading p:last-child { max-width:600px; margin:10px 0 0; color:#6c7972; font-size:13px; line-height:1.6; }
    .unread-pill { flex:0 0 auto; padding:9px 13px; border:1px solid #dce8e2; border-radius:999px; background:#f4f8f6; color:#315b4d; font-size:11px; font-weight:700; }
    .notifications-panel { overflow:hidden; border:1px solid #e5ebe8; border-radius:16px; background:white; box-shadow:0 10px 30px rgb(26 57 44 / 4%); }
    .notification-row { display:flex; align-items:stretch; gap:4px; padding:5px 12px 5px 0; border-bottom:1px solid #edf1ef; }
    .notification-row:last-child { border-bottom:0; }
    .notification-row.unread { background:#f7faf8; }
    .notification-open { display:flex; flex:1; min-width:0; align-items:center; gap:14px; padding:16px 18px; border:0; background:transparent; color:inherit; text-align:left; cursor:pointer; }
    .notification-icon { display:grid; flex:0 0 40px; width:40px; height:40px; place-items:center; border-radius:13px; background:#edf4f0; color:#4d7762; }
    .notification-copy { display:grid; min-width:0; flex:1; gap:5px; }
    .notification-title { color:#2d3c34; font-size:13px; font-weight:700; }
    .notification-summary { color:#647169; font-size:12px; line-height:1.5; overflow-wrap:anywhere; }
    time { color:#89948e; font-size:10px; }
    .open-link { display:flex; align-items:center; gap:7px; color:#4e755f; font-size:11px; font-weight:700; white-space:nowrap; }
    .delete-notification { flex:0 0 38px; width:38px; height:38px; align-self:center; border:0; border-radius:9px; background:transparent; color:#88948d; cursor:pointer; }
    .delete-notification:hover { background:#fff1ee; color:#a94c40; }
    .empty-state { display:flex; min-height:200px; align-items:center; justify-content:center; flex-direction:column; gap:10px; color:#78857e; font-size:13px; }
    .empty-state > i { color:#6e9280; font-size:25px; }
    .empty-state strong { color:#35443c; font-size:14px; }
    @media(max-width:600px) { .notifications-page { padding:20px 12px; } .notifications-heading { flex-direction:column; } .notification-open { gap:10px; padding:13px 10px; } .notification-icon { flex-basis:34px; width:34px; height:34px; } .open-link { font-size:0; } .open-link i { font-size:12px; } }
  `]
})
export class NotificationsPageComponent implements OnInit {
  private readonly inbox = inject(InboxNotificationService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  items: InboxNotification[] = [];
  unreadCount = 0;
  loading = true;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.inbox.list().subscribe({
      next: (items) => { this.items = items; this.unreadCount = items.filter((item) => !item.lue).length; this.inbox.setUnreadCount(this.unreadCount); this.loading = false; },
      error: () => this.loading = false
    });
  }

  open(item: InboxNotification): void {
    this.inbox.markRead(item.id).subscribe({
      next: () => {
        this.items = this.items.map((value) => value.id === item.id ? { ...value, lue: true } : value);
        if (!item.lue) { this.unreadCount = Math.max(0, this.unreadCount - 1); this.inbox.decrementUnreadCount(); }
        void this.router.navigateByUrl(item.lien);
      },
      error: () => undefined
    });
  }

  remove(item: InboxNotification, event: Event): void {
    event.stopPropagation();
    this.inbox.delete(item.id).subscribe({
      next: () => {
        this.items = this.items.filter((value) => value.id !== item.id);
        if (!item.lue) { this.unreadCount = Math.max(0, this.unreadCount - 1); this.inbox.decrementUnreadCount(); }
        this.notifications.success('Notification supprimée', 'La notification a été retirée de votre liste.');
      },
      error: () => undefined
    });
  }
}
