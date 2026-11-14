import { Injectable, inject } from '@angular/core';
import { MessageService } from 'primeng/api';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly messages = inject(MessageService);

  error(summary: string, detail: string): void {
    this.messages.add({ severity: 'error', summary, detail, life: 3000 });
  }

  success(summary: string, detail: string): void {
    this.messages.add({ severity: 'success', summary, detail, life: 3000 });
  }
}
