import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PayrollPayslipsService } from '../../services/payroll-payslips.service';
import { PayrollOperationsService, PayrollPeriod, PayrollPeriodBulletin, PayrollVariableOptions } from '../../services/payroll-operations.service';

@Component({
  selector: 'app-payroll-periods-page', standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, ButtonModule, InputTextModule],
  templateUrl: './payroll-periods-page.component.html', styleUrl: './payroll-periods-page.component.scss'
})
export class PayrollPeriodsPageComponent implements OnInit {
  private readonly service = inject(PayrollOperationsService);
  private readonly payslips = inject(PayrollPayslipsService);
  private readonly notifications = inject(NotificationService);
  periods: PayrollPeriod[] = [];
  bulletins: PayrollPeriodBulletin[] = [];
  variableOptions: PayrollVariableOptions | null = null;
  variableEmployeeId: number | null = null;
  variableRubricId: number | null = null;
  variableAmount: number | null = null;
  variableJustification = '';
  decisionReason = '';
  advanceEmployeeId: number | null = null;
  advanceDate = this.today();
  advanceAmount: number | null = null;
  advanceReason = '';
  paymentReference = '';
  selected: PayrollPeriod | null = null;
  month = this.currentMonth();
  loading = true;
  busy = false;
  loadingBulletins = false;
  downloadingId: number | null = null;
  downloadingExcel = false;

  ngOnInit(): void { this.load(); }
  create(): void {
    if (this.busy || !this.month) return;
    const [year, month] = this.month.split('-').map(Number);
    const dateDebut = `${this.month}-01`;
    const dateFin = `${this.month}-${String(new Date(year, month, 0).getDate()).padStart(2, '0')}`;
    this.run('create', () => this.service.create(dateDebut, dateFin), 'Période créée', 'La période mensuelle est prête au calcul.');
  }
  open(period: PayrollPeriod): void {
    this.selected = period; this.loadingBulletins = true; this.bulletins = []; this.variableOptions = null;
    this.service.bulletins(period.id).subscribe({ next: rows => { this.bulletins = rows; this.loadingBulletins = false; }, error: () => { this.loadingBulletins = false; this.notifications.error('Détails indisponibles', 'Les bulletins de cette période n’ont pas pu être chargés.'); } });
    if (period.statutCode === 'BROUILLON' || period.statutCode === 'CALCULEE') this.loadVariableOptions(period.id);
  }
  addVariable(): void {
    if (!this.selected || this.busy || !this.variableEmployeeId || !this.variableRubricId || !this.variableAmount || !this.variableJustification.trim()) return;
    this.busy=true;this.service.createVariable(this.selected.id,this.variableEmployeeId,this.variableRubricId,this.variableAmount,this.variableJustification).subscribe({next:rows=>{this.variableOptions=rows;this.variableAmount=null;this.variableJustification='';this.busy=false;this.notifications.success('Variable enregistrée','Elle attend la validation d’un autre compte RH avant le calcul.');},error:()=>{this.busy=false;this.notifications.error('Variable refusée','Vérifiez le salarié, la rubrique manuelle, le montant et la justification.');}});
  }
  decideVariable(variableId:number,approve:boolean):void {
    if(!this.selected||this.busy||!this.decisionReason.trim()) { this.notifications.error('Motif requis','Ajoutez un motif avant de traiter la variable.');return; }
    this.busy=true;this.service.decideVariable(this.selected.id,variableId,approve,this.decisionReason).subscribe({next:rows=>{this.variableOptions=rows;this.decisionReason='';this.busy=false;this.notifications.success(approve?'Variable validée':'Variable refusée',approve?'Elle sera reprise au prochain calcul de la période.':'La décision est conservée dans son historique.');},error:()=>{this.busy=false;this.notifications.error('Décision impossible','Un autre compte RH doit valider la saisie, et la période doit rester en brouillon.');}});
  }
  addAdvance():void{
    if(!this.selected||this.busy||!this.advanceEmployeeId||!this.advanceDate||!this.advanceAmount||!this.advanceReason.trim())return;
    this.busy=true;this.service.createAdvance(this.selected.id,this.advanceEmployeeId,this.advanceDate,this.advanceAmount,this.advanceReason).subscribe({next:rows=>{this.variableOptions=rows;this.advanceAmount=null;this.advanceReason='';this.busy=false;this.notifications.success('Avance enregistrée','Elle doit être marquée versée par un autre compte RH avant d’être déduite.');},error:()=>{this.busy=false;this.notifications.error('Avance refusée','Vérifiez le salarié, le montant, la date et le motif.');}});
  }
  decideAdvance(advanceId:number,approve:boolean):void{
    if(!this.selected||this.busy||!this.decisionReason.trim()){this.notifications.error('Motif requis','Ajoutez un motif avant de traiter l’avance.');return;}
    this.busy=true;this.service.decideAdvance(this.selected.id,advanceId,approve,this.decisionReason).subscribe({next:rows=>{this.variableOptions=rows;this.decisionReason='';this.busy=false;this.notifications.success(approve?'Avance marquée versée':'Avance annulée',approve?'Elle sera déduite du net de la période.':'La décision est historisée dans le motif de l’avance.');},error:()=>{this.busy=false;this.notifications.error('Décision impossible','Un autre compte RH doit traiter l’avance pendant que la période est en brouillon.');}});
  }
  calculate(): void { if (this.selected) this.run('calculate', () => this.service.calculate(this.selected!.id), 'Calcul terminé', 'Les bulletins provisoires ont été calculés depuis les données disponibles. Vérifiez les lignes avant validation.'); }
  validate(): void { if (this.selected) this.run('validate', () => this.service.validate(this.selected!.id), 'Période validée', 'Les bulletins sont figés et visibles aux salariés.'); }
  markPaid(): void { if (this.selected && this.paymentReference.trim().length >= 3) this.run('paid', () => this.service.markPaid(this.selected!.id,this.paymentReference), 'Paiement enregistré', 'La période est marquée comme payée dans le SIRH.'); else this.notifications.error('Référence manquante','Saisissez la référence du virement ou de l’ordre de paiement.'); }
  close(): void { if (this.selected) this.run('close', () => this.service.close(this.selected!.id), 'Période clôturée', 'La période est archivée.'); }
  download(row: PayrollPeriodBulletin): void {
    if (this.downloadingId !== null) return; this.downloadingId = row.id;
    this.payslips.rhPdf(row.id).subscribe({ next: blob => { const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `bulletin-${row.matricule}.pdf`; a.click(); URL.revokeObjectURL(url); this.downloadingId = null; }, error: () => { this.downloadingId = null; this.notifications.error('PDF indisponible', 'Le bulletin n’a pas pu être téléchargé.'); } });
  }
  downloadExcel(): void {
    if (!this.selected || this.downloadingExcel) return;
    this.downloadingExcel = true;
    this.service.payrollExcel(this.selected.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob); const a = document.createElement('a');
        a.href = url; a.download = `etat-de-paie-${this.selected!.dateDebut.slice(0, 7)}.xlsx`; a.click(); URL.revokeObjectURL(url);
        this.downloadingExcel = false;
      },
      error: () => { this.downloadingExcel = false; this.notifications.error('Excel indisponible', 'Aucun état Excel n’a pu être généré. Vérifiez que la période contient des bulletins calculés.'); }
    });
  }
  private load(): void { this.loading = true; this.service.periods().subscribe({ next: rows => { this.periods = rows; this.loading = false; if (this.selected) this.selected = rows.find(p => p.id === this.selected!.id) ?? null; }, error: () => { this.loading = false; this.notifications.error('Paie indisponible', 'Les périodes de paie n’ont pas pu être chargées.'); } }); }
  private loadVariableOptions(id:number):void{this.service.advanceOptions(id).subscribe({next:rows=>this.variableOptions=rows,error:()=>this.notifications.error('Variables indisponibles','Les rubriques et éléments à saisir de cette période ne sont pas accessibles.')});}
  private run(action: string, request: () => import('rxjs').Observable<PayrollPeriod>, title: string, message: string): void {
    if (this.busy) return; this.busy = true;
    request().subscribe({ next: period => { this.selected = period; this.busy = false; this.notifications.success(title, message); this.load(); this.open(period); }, error: () => { this.busy = false; const messages: Record<string,string> = { create: 'La période existe peut-être déjà ou les dates sont invalides.', calculate: 'Le calcul a échoué. Vérifiez les contrats, paramètres et données sources de la période.', validate: 'Vérifiez que la période est calculée et contient des bulletins.', paid: 'Seule une période validée peut être marquée payée.', close: 'La période doit être payée avant sa clôture.' }; this.notifications.error('Action impossible', messages[action]); } });
  }
  private currentMonth(): string { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`; }
  private today(): string { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`; }
}
