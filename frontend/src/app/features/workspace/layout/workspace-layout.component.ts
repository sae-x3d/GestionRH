import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LogoutConfirmationService } from '../../../core/auth/logout-confirmation.service';
import { RoleService } from '../../../core/auth/role.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastModule } from 'primeng/toast';
import { NotificationService } from '../../../core/notifications/notification.service';
import { InboxNotification, InboxNotificationService } from '../../../core/notifications/inbox-notification.service';

type SidebarSection = 'equipe' | 'planning' | 'futur' | 'offre' | 'contrat' | 'recrutement' | 'conges' | 'contrats' | 'organisation' | 'personnel' | 'pilotage' | 'paie' | 'comptes' | 'acces' | 'finance' | 'disciplineEmploye' | 'disciplineRh';

@Component({
  selector: 'app-workspace-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastModule, DatePipe],
  templateUrl: './workspace-layout.component.html',
  styleUrl: './workspace-layout.component.scss'
})
export class WorkspaceLayoutComponent implements OnInit, OnDestroy {
  private readonly roleService = inject(RoleService);
  private readonly auth = inject(AuthService);
  private readonly logoutConfirmation = inject(LogoutConfirmationService);
  private readonly inbox = inject(InboxNotificationService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  readonly navigationOpen = signal(false);
  readonly notificationOpen = signal(false);
  readonly currentRole = this.roleService.currentRole;
  readonly currentSession = this.auth.currentSession;
  photoLoadFailed = false;
  readonly isFinanceMember = this.roleService.isFinanceMember;
  notificationItems: InboxNotification[] = [];
  readonly unreadNotifications = this.inbox.unreadCount;
  notificationLoading = false;
  private notificationTimer?: ReturnType<typeof setInterval>;
  readonly expandedSections = signal({
    equipe: false,
    planning: false,
    futur: false,
    offre: false,
    contrat: false,
    recrutement: false,
    conges: false,
    contrats: false,
    organisation: false,
    personnel: false,
    pilotage: false,
    paie: false,
    comptes: false,
    acces: false,
    finance: false,
    disciplineEmploye: false,
    disciplineRh: false,
    carriere: false
  });

  ngOnInit(): void {
    this.refreshUnreadCount();
    this.notificationTimer = setInterval(() => this.refreshUnreadCount(), 30000);
  }

  ngOnDestroy(): void {
    if (this.notificationTimer) clearInterval(this.notificationTimer);
  }

  onPhotoError(): void { this.photoLoadFailed = true; }

  toggleNotifications(): void {
    const open = !this.notificationOpen();
    this.notificationOpen.set(open);
    if (open) this.loadNotificationPreview();
  }

  closeNotifications(): void { this.notificationOpen.set(false); }

  openNotification(item: InboxNotification): void {
    this.inbox.markRead(item.id).subscribe({
      next: () => {
        this.notificationItems = this.notificationItems.map((notification) => notification.id === item.id
          ? { ...notification, lue: true } : notification);
        if (!item.lue) this.inbox.decrementUnreadCount();
        this.closeNotifications();
        void this.router.navigateByUrl(item.lien);
      },
      error: () => undefined
    });
  }

  deleteNotification(item: InboxNotification, event: Event): void {
    event.stopPropagation();
    this.inbox.delete(item.id).subscribe({
      next: () => {
        this.notificationItems = this.notificationItems.filter((notification) => notification.id !== item.id);
        if (!item.lue) this.inbox.decrementUnreadCount();
        this.notifications.success('Notification supprimée', 'La notification a été retirée de votre liste.');
      },
      error: () => undefined
    });
  }

  private loadNotificationPreview(): void {
    this.notificationLoading = true;
    this.inbox.list().subscribe({
      next: (items) => { this.notificationItems = items.slice(0, 6); this.notificationLoading = false; },
      error: () => this.notificationLoading = false
    });
  }

  private refreshUnreadCount(): void {
    this.inbox.getUnreadCount().subscribe({ next: () => undefined, error: () => undefined });
  }

  toggleEmployeeSection(section: SidebarSection): void {
    this.expandedSections.update((sections) => ({
      ...sections,
      [section]: !sections[section]
    }));
  }

  logout(): void {
    this.logoutConfirmation.confirm();
  }

  toggleNavigation(): void {
    this.navigationOpen.update((isOpen) => !isOpen);
  }

  closeNavigation(): void {
    this.navigationOpen.set(false);
  }
}
