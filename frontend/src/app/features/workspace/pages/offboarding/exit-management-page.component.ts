import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { CreateExit, ExitEmployeeOption, ExitManagementService, ExitMotif, ExitRequest } from '../../services/exit-management.service';

@Component({
  selector: 'app-exit-management-page', standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './exit-management-page.component.html', styleUrl: './exit-management-page.component.scss'
})
export class ExitManagementPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(ExitManagementService);
  private readonly toast = inject(NotificationService);
  readonly role = this.route.snapshot.data['exitRole'] as 'manager'|'rh';
  rows: ExitRequest[] = [];
  options: ExitEmployeeOption[] = [];
  motifs: ExitMotif[] = [];
  loading = true;
  busyId: number|null = null;
  expandedId: number|null = null;
  selectedOption = '';
  paymentReference = '';
  paymentDate = new Date().toISOString().slice(0,10);
  paymentAmount: number|null = null;
  netAmount: number|null = null;
  netJustification = '';
  form: CreateExit = { employeeId: 0, contractId: 0, motifCode: '', dateNotification: new Date().toISOString().slice(0,10), dernierJourTravaille: '', commentaire: '' };
  ngOnInit(): void {
    this.service.motifs().subscribe({next: rows => { this.motifs=rows; this.form.motifCode=rows[0]?.code??''; }});
    if (this.role==='manager') this.service.managerOptions().subscribe({next: rows=>this.options=rows});
    this.load();
  }
  selectEmployee(value:string): void { this.selectedOption=value; const selected=this.options.find(x=>String(x.contractId)===value); this.form.employeeId=selected?.employeeId??0; this.form.contractId=selected?.contractId??0; }
  create(): void {
    if (!this.form.employeeId || !this.form.contractId || !this.form.motifCode || !this.form.dernierJourTravaille) { this.toast.error('Informations manquantes','Choisissez le salarié, le motif et le dernier jour travaillé.'); return; }
    this.busyId=0; this.service.create(this.form).subscribe({next:()=>{this.busyId=null;this.form.commentaire='';this.form.dernierJourTravaille='';this.toast.success('Demande transmise','Les RH vont contrôler le contrat et préparer le dossier de sortie.');this.load();},error:()=>this.busyId=null});
  }
  prepare(row:ExitRequest): void { this.run(row.id,()=>this.service.prepare(row.id),'Décompte préparé','Le brouillon reprend le solde de congés et le dernier mois de paie réglé. Vérifiez les autres droits avant validation.'); }
  validate(row:ExitRequest): void { if(this.netAmount===null||!this.netJustification.trim()){this.toast.error('Contrôle du net requis','Saisissez le net à payer tiré du décompte contrôlé et la référence de son calcul.');return;}this.run(row.id,()=>this.service.validate(row.id,this.netAmount!,this.netJustification),'Décompte validé','Le net contrôlé est figé dans le dossier.'); }
  confirmPayment(row:ExitRequest): void { if(!this.paymentReference.trim()||this.paymentAmount===null){this.toast.error('Paiement incomplet','Saisissez la référence, la date et le montant réellement payé.');return;}this.run(row.id,()=>this.service.confirmPayment(row.id,this.paymentReference,this.paymentDate,this.paymentAmount!),'Paiement enregistré','Le montant enregistré correspond au net validé.'); }
  generateDocuments(row:ExitRequest): void { this.run(row.id,()=>this.service.generateDocuments(row.id),'PDF générés','Le certificat de travail et le décompte STC sont archivés dans le dossier salarié.'); }
  documentsRemitted(row:ExitRequest): void { this.run(row.id,()=>this.service.documentsRemitted(row.id),'Remise confirmée','Les documents obligatoires sont rattachés et leur remise est enregistrée.'); }
  finalize(row:ExitRequest): void { if(!window.confirm('Confirmer la clôture ? Le contrat sera terminé et le compte salarié suspendu.'))return;this.run(row.id,()=>this.service.finalize(row.id),'Sortie clôturée','Le contrat est terminé et l’accès du salarié est suspendu.'); }
  cancel(row:ExitRequest): void { const motif=window.prompt('Motif obligatoire de l’annulation'); if(!motif?.trim())return; this.run(row.id,()=>this.service.cancel(row.id,motif),'Demande annulée','Le motif est conservé dans l’historique.'); }
  private run(id:number,request:()=>any,title:string,message:string):void {if(this.busyId!==null)return;this.busyId=id;request().subscribe({next:()=>{this.busyId=null;this.toast.success(title,message);this.load();},error:(error:HttpErrorResponse)=>{this.busyId=null;const detail=typeof error.error?.message==='string'?error.error.message:error.status===0?'Le serveur RH est injoignable. Réessayez lorsque la connexion sera rétablie.':'La demande n’a pas pu être enregistrée. Vérifiez le dossier et réessayez.';this.toast.error('Action impossible',detail);}});}
  private load():void {this.loading=true;this.service.list(this.role).subscribe({next:rows=>{this.rows=rows;this.loading=false;},error:()=>this.loading=false});}
}
