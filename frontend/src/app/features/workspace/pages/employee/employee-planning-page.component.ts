import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PlanningDay, PlanningWeek, TimeManagementService } from '../../services/time-management.service';

@Component({
  selector: 'app-employee-planning-page', standalone: true, imports: [CommonModule],
  template: `
    <main class="employee-planning">
      <header class="heading"><div><p class="eyebrow">MON TEMPS <span>/</span> PLANNING</p><h1>Ma semaine</h1><p>Consultez vos horaires, vos repos et les observations communiquees par votre manager.</p></div><div class="week-switcher"><button type="button" (click)="move(-7)" aria-label="Semaine precedente"><i class="pi pi-angle-left"></i></button><div><strong>{{ week?.semaineDebut | date:'dd MMM' }} — {{ week?.semaineFin | date:'dd MMM yyyy' }}</strong><button type="button" (click)="currentWeek()">Cette semaine</button></div><button type="button" (click)="move(7)" aria-label="Semaine suivante"><i class="pi pi-angle-right"></i></button></div></header>
      @if (loading) { <div class="state"><i class="pi pi-spin pi-spinner"></i> Chargement de votre planning…</div> }
      @else if (week) {
        <section class="summary-strip"><span class="summary-icon"><i class="pi pi-calendar-clock"></i></span><div><strong>{{ plannedDays }} jour{{ plannedDays === 1 ? '' : 's' }} de travail</strong><small>Les cases vertes representent votre presence planifiee. Les cases grisees correspondent aux pauses ou aux heures hors service.</small></div></section>
        <section class="calendar-card"><div class="section-heading"><div><p class="eyebrow">VUE HEBDOMADAIRE</p><h2>Heures par jour</h2></div><span class="legend"><i></i> Heures travaillees</span></div>
          @if (!week.jours?.length) { <div class="empty"><i class="pi pi-calendar-times"></i><strong>Aucun planning publie</strong><span>Votre manager n'a pas encore publie les horaires de cette semaine.</span></div> }
          @else {
            <div class="table-scroll"><table><thead><tr><th class="hour-head">HEURE</th>@for (day of days; track day.date) { <th><span>{{ day.label }}</span><b>{{ day.number }}</b><small>{{ daySchedule(day.date)?.type_jour || '—' }}</small></th> }</tr></thead>
              <tbody>@for (slot of slots; track slot.value) { <tr><th class="hour-label">{{ slot.label }}</th>@for (day of days; track day.date) { <td><span class="time-cell" [class.worked]="isWorking(daySchedule(day.date), slot.value)" [class.break]="isBreak(daySchedule(day.date), slot.value)" [class.leave]="isLeave(daySchedule(day.date))"></span></td> }</tr> }</tbody>
              <tfoot><tr><th>Total</th>@for (day of days; track day.date) { <td>{{ dayTotal(daySchedule(day.date)) }}</td> }</tr><tr><th>Observation</th>@for (day of days; track day.date) { <td class="observation-note">{{ daySchedule(day.date)?.observation || '—' }}</td> }</tr></tfoot>
            </table></div>
            <div class="legend-row"><span><i class="swatch work-swatch"></i> Travail planifie</span><span><i class="swatch break-swatch"></i> Pause prevue</span><span><i class="swatch off-swatch"></i> Hors service / repos</span></div>
          }
        </section>
      }
    </main>
  `,
  styles: [`
    :host{display:block;min-width:0;color:#1c2b24}.employee-planning{display:grid;gap:22px}.heading{display:flex;align-items:center;justify-content:space-between;gap:18px}.eyebrow{margin:0 0 8px;color:#507967;font-size:10px;font-weight:800;letter-spacing:.15em}.eyebrow span{padding:0 5px;color:#a1b0a8}h1,h2,p{margin-top:0}h1{margin-bottom:8px;color:#1d2b24;font-size:clamp(27px,3vw,38px);letter-spacing:-.045em}.heading p:last-child{max-width:620px;margin:0;color:#718078;font-size:13px;line-height:1.55}.week-switcher{display:flex;align-items:center;gap:11px;padding:7px;border:1px solid #e2eae5;border-radius:14px;background:#fff;white-space:nowrap}.week-switcher>div{display:grid;justify-items:center;gap:4px;color:#31463a;font-size:12px}.week-switcher button{display:grid;width:34px;height:34px;place-items:center;border:0;border-radius:10px;background:#f1f6f3;color:#47715c;cursor:pointer}.week-switcher>div button{display:block;width:auto;height:auto;padding:0;background:transparent;font-size:10px;font-weight:700}.summary-strip{display:flex;align-items:center;gap:12px;padding:14px 17px;border:1px solid #dce9e2;border-radius:14px;background:#f7faf8}.summary-icon{display:grid;width:38px;height:38px;place-items:center;border-radius:11px;background:#e8f2ec;color:#3d7258}.summary-strip>div{display:grid;gap:4px}.summary-strip strong{color:#33463a;font-size:12px}.summary-strip small{color:#79867e;font-size:10px;line-height:1.5}.calendar-card{min-width:0;padding:22px;border:1px solid #e3ebe6;border-radius:18px;background:#fff;box-shadow:0 10px 30px rgb(25 57 42 / 3%)}.section-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}.section-heading h2{margin:0;color:#24352c;font-size:19px}.legend{display:flex;align-items:center;gap:7px;color:#65806e;font-size:10px}.legend i{width:8px;height:8px;border-radius:3px;background:#7bb28e}.table-scroll{overflow:auto;border:1px solid #e8eeea;border-radius:12px}table{width:100%;min-width:910px;border-collapse:collapse;table-layout:fixed}th,td{padding:3px 5px;border-bottom:1px solid #f0f3f1;text-align:center}thead th{height:58px;background:#f7faf8;color:#728078;font-size:9px;text-transform:capitalize}thead th span,thead th b,thead th small{display:block}thead th b{margin-top:4px;color:#405449;font-size:11px}thead th small{margin-top:4px;color:#89a08f;font-size:8px;font-weight:500}.hour-head{width:58px}.hour-label{width:58px;color:#89968e;font-size:8px;font-weight:500;text-align:right}.time-cell{display:block;height:14px;border-radius:4px;background:#f1f3f1}.time-cell.worked{background:#82b693}.time-cell.break{background:repeating-linear-gradient(135deg,#e3e9e5 0 4px,#f8faf8 4px 8px)}.time-cell.leave{background:#dceee2;border:1px solid #9fbea8}.observation-note{max-width:110px;overflow-wrap:anywhere;color:#849188;font-size:8px;line-height:1.4}.empty{display:grid;min-height:210px;align-content:center;justify-items:center;gap:8px;color:#7c8981;font-size:12px}.empty i{color:#72927e;font-size:24px}.empty strong{color:#35463b;font-size:14px}.legend-row{display:flex;flex-wrap:wrap;gap:16px;margin-top:13px;color:#7d8981;font-size:9px}.legend-row span{display:flex;align-items:center;gap:6px}.swatch{width:8px;height:8px;border-radius:3px}.work-swatch{background:#82b693}.break-swatch{background:#cbd5ce}.off-swatch{background:#f1f3f1}tfoot th,tfoot td{padding:10px 5px;border:0;background:#f8faf9;color:#5f7166;font-size:9px;font-weight:700}.state{display:grid;min-height:120px;place-items:center;color:#748178;font-size:12px}
    @media(max-width:700px){.heading{align-items:flex-start;flex-direction:column}.week-switcher{align-self:stretch;justify-content:center}.calendar-card{padding:14px;border-radius:14px}.section-heading{align-items:flex-start;flex-direction:column;gap:10px}}
  `]
})
export class EmployeePlanningPageComponent implements OnInit {
  private readonly time = inject(TimeManagementService);
  private readonly toast = inject(NotificationService);
  weekStart = this.mondayOf(new Date()); week?: PlanningWeek; loading = true;
  get slots(): Array<{ value: number; label: string }> {
    const shifts = (this.week?.jours ?? []).filter((day) => day.type_code === 'TRAVAIL' && day.debut && day.fin);
    const fallback = this.week?.horairesParDefaut;
    const starts = shifts.map((day) => this.toMinutes(day.debut!));
    const ends = shifts.map((day) => this.toMinutes(day.fin!));
    if (!starts.length && fallback?.heureDebut && fallback.heureFin) {
      starts.push(this.toMinutes(fallback.heureDebut)); ends.push(this.toMinutes(fallback.heureFin));
    }
    if (!starts.length) return [];
    const first = Math.floor(Math.min(...starts) / 30) * 30;
    const last = Math.ceil(Math.max(...ends) / 30) * 30;
    return Array.from({ length: Math.max(1, (last - first) / 30) }, (_, index) => {
      const minutes = first + index * 30;
      return { value: minutes, label: minutes % 60 === 0 ? `${String(Math.floor(minutes / 60)).padStart(2, '0')}:00` : '' };
    });
  }
  ngOnInit(): void { this.load(); }
  get days(): Array<{date:string;label:string;number:string}> { return Array.from({length:7},(_,index)=>{const date=this.addDays(this.weekStart,index);const d=this.parseDate(date);return {date,label:new Intl.DateTimeFormat('fr-FR',{weekday:'short'}).format(d),number:new Intl.DateTimeFormat('fr-FR',{day:'2-digit',month:'2-digit'}).format(d)};}); }
  get plannedDays(): number { return this.week?.jours?.filter((day)=>day.type_code==='TRAVAIL').length ?? 0; }
  load(): void { this.loading=true;this.time.myPlanning(this.weekStart).subscribe({next:(week)=>{this.week=week;this.loading=false;},error:(error)=>{this.loading=false;this.toast.error('Planning indisponible',error?.error?.detail??'Impossible de charger votre semaine.');}}); }
  move(days:number):void{this.weekStart=this.addDays(this.weekStart,days);this.load();}
  currentWeek():void{this.weekStart=this.mondayOf(new Date());this.load();}
  daySchedule(date:string):PlanningDay|undefined{return this.week?.jours?.find((day)=>day.date===date);}
  isWorking(day:PlanningDay|undefined,minute:number):boolean{if(!day||day.type_code!=='TRAVAIL'||!day.debut||!day.fin)return false;const start=this.toMinutes(day.debut),end=this.toMinutes(day.fin),pauseStart=day.pauseDebut?this.toMinutes(day.pauseDebut):-1,pauseEnd=day.pauseFin?this.toMinutes(day.pauseFin):-1;return minute>=start&&minute<end&&!(pauseStart>=0&&minute>=pauseStart&&minute<pauseEnd);}
  isLeave(day:PlanningDay|undefined):boolean{return day?.type_code==='CONGE'||day?.type_code==='ABSENCE';}
  isBreak(day:PlanningDay|undefined,minute:number):boolean{return !!day?.pauseDebut&&!!day.pauseFin&&minute>=this.toMinutes(day.pauseDebut)&&minute<this.toMinutes(day.pauseFin);}
  dayTotal(day:PlanningDay|undefined):string{if(!day||day.type_code!=='TRAVAIL'||!day.debut||!day.fin)return '—';let minutes=this.toMinutes(day.fin)-this.toMinutes(day.debut);if(day.pauseDebut&&day.pauseFin)minutes-=this.toMinutes(day.pauseFin)-this.toMinutes(day.pauseDebut);return `${Math.floor(minutes/60)} h${minutes%60?` ${minutes%60} min`:''}`;}
  private toMinutes(time:string):number{const [hours,minutes]=time.split(':').map(Number);return hours*60+minutes;}
  private mondayOf(date:Date):string{const local=new Date(date.getFullYear(),date.getMonth(),date.getDate());const day=local.getDay()||7;local.setDate(local.getDate()-day+1);return this.toIso(local);}
  private addDays(date:string,count:number):string{const parsed=this.parseDate(date);parsed.setDate(parsed.getDate()+count);return this.toIso(parsed);}
  private parseDate(value:string):Date{const [year,month,day]=value.split('-').map(Number);return new Date(year,month-1,day);}
  private toIso(value:Date):string{return `${value.getFullYear()}-${String(value.getMonth()+1).padStart(2,'0')}-${String(value.getDate()).padStart(2,'0')}`;}
}




