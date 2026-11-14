import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { RoleService } from '../../../../core/auth/role.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { EmployeeDossierService } from '../../services/employee-dossier.service';

type DossierTab = 'overview' | 'personal' | 'professional' | 'documents' | 'history' | 'requests';
@Component({ selector: 'app-employee-dossier', standalone: true, imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './employee-dossier-page.component.html', styleUrl: './employee-dossier-page.component.scss' })
export class EmployeeDossierPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute); private readonly service = inject(EmployeeDossierService);
  private readonly roleService = inject(RoleService); private readonly toast = inject(NotificationService);
  readonly role = this.roleService.currentRole;
  readonly tabs: { id: DossierTab; label: string }[] = [
    { id: 'overview', label: 'Vue d’ensemble' }, { id: 'personal', label: 'Informations personnelles' },
    { id: 'professional', label: 'Situation professionnelle' }, { id: 'documents', label: 'Documents' },
    { id: 'history', label: 'Historique' }, { id: 'requests', label: 'Demandes' }
  ];
  readonly correctionFields = [
    { code: 'FULL_NAME', label: 'Nom complet' }, { code: 'PHONE_NUMBER', label: 'Téléphone' },
    { code: 'REGION', label: 'Région' }, { code: 'ADRESSE_LIGNE', label: 'Adresse' }, { code: 'VILLE', label: 'Ville' },
    { code: 'CODE_POSTAL', label: 'Code postal' }, { code: 'PAYS', label: 'Pays' },
    { code: 'CONTACT_URGENCE_NOM', label: 'Contact d’urgence' }, { code: 'CONTACT_URGENCE_LIEN', label: 'Lien avec le contact' },
    { code: 'CONTACT_URGENCE_TELEPHONE', label: 'Téléphone d’urgence' }
  ];
  dossier: Record<string, any> | null = null; employeeId: number | null = null; self = false;
  loading = true; tab: DossierTab = 'overview'; savingRequest = false; uploading = false;
  documents: Record<string, any>[] = []; documentTypes: { id: number; code: string; libelle: string; obligatoire: boolean }[] = [];
  corrections: Record<string, any>[] = []; selectedField = 'PHONE_NUMBER'; newValue = ''; reason = '';
  correctionFile: File | null = null;
  selectedTypeId: number | null = null; selectedFile: File | null = null; issueDate = ''; expiryDate = '';
  versioningDocumentId: number | null = null;
  archiveBusy: number | null = null;
  versionsExpandedDocumentId: number | null = null;
  versionsLoadingDocumentId: number | null = null;
  versionsByDocument: Record<number, Record<string, any>[]> = {};
  statusCode = '';
  statusDate = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  statusReason = '';
  savingStatus = false;
  get isRh(): boolean { return this.role() === 'rh'; }
  get editableRequests(): boolean { return this.self && (this.role() === 'employe' || this.role() === 'manager'); }

  ngOnInit(): void {
    this.self = this.route.snapshot.data['self'] === true;
    const routeId = Number(this.route.snapshot.paramMap.get('employeeId'));
    this.employeeId = this.self ? null : Number.isInteger(routeId) && routeId > 0 ? routeId : null;
    this.load();
  }
  setTab(tab: DossierTab): void { this.tab = tab; if (tab === 'requests') this.loadCorrections(); }
  selectFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    if (file.size > 15 * 1024 * 1024) { this.toast.error('Fichier trop volumineux', 'Un document administratif est limité à 15 Mo.'); return; }
    this.selectedFile = file;
  }
  selectCorrectionFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) { this.correctionFile = null; return; }
    const supportedType = ['application/pdf', 'image/png', 'image/jpeg'].includes(file.type)
      || (!file.type && /\.(pdf|png|jpe?g)$/i.test(file.name));
    if (file.size > 15 * 1024 * 1024 || !supportedType) {
      this.toast.error('Pièce jointe refusée', 'Choisissez un PDF, PNG ou JPEG valide de 15 Mo maximum.'); return;
    }
    this.correctionFile = file;
  }
  submitCorrection(): void {
    if (!this.editableRequests || this.savingRequest) return;
    if (!this.newValue.trim() || !this.reason.trim()) { this.toast.error('Demande incomplète', 'La nouvelle valeur et le motif sont obligatoires.'); return; }
    this.savingRequest = true;
    this.service.submitCorrection({ champ: this.selectedField, nouvelleValeur: this.newValue.trim(), motif: this.reason.trim() }, this.correctionFile ?? undefined)
      .pipe(finalize(() => this.savingRequest = false)).subscribe({ next: () => {
        this.newValue = ''; this.reason = ''; this.correctionFile = null; this.loadCorrections();
        this.toast.success('Demande envoyée', 'Les RH examineront votre correction avant de modifier le dossier.');
      }, error: () => this.toast.error('Demande non envoyée', 'Une demande peut déjà être ouverte pour ce champ, ou la valeur doit être corrigée.') });
  }
  cancelCorrection(item: Record<string, any>): void {
    if (!this.self || item['statutCode'] !== 'SOUMISE' || this.savingRequest) return;
    this.savingRequest = true;
    this.service.cancelCorrection(Number(item['id'])).pipe(finalize(() => this.savingRequest = false)).subscribe({
      next: () => { this.toast.success('Demande annulée', 'La demande a été retirée avant son traitement par les RH.'); this.loadCorrections(); },
      error: () => this.toast.error('Annulation impossible', 'Cette demande a peut-être déjà été prise en charge par les RH.')
    });
  }
  downloadCorrectionAttachment(item: Record<string, any>): void {
    this.service.downloadCorrectionAttachment(Number(item['id'])).subscribe({
      next: (blob) => { const url = URL.createObjectURL(blob); documentCreateLink(url, String(item['pieceJointe'] ?? 'justificatif')).click(); URL.revokeObjectURL(url); },
      error: () => this.toast.error('Pièce jointe indisponible', 'Vous ne disposez pas des droits nécessaires ou le fichier est absent.')
    });
  }
  uploadDocument(): void {
    if (this.uploading || !this.employeeId || !this.selectedFile || !this.selectedTypeId) {
      if (!this.selectedFile || !this.selectedTypeId) this.toast.error('Document incomplet', 'Choisissez une catégorie et un fichier.');
      return;
    }
    const data = new FormData(); data.set('typeId', String(this.selectedTypeId)); data.set('file', this.selectedFile);
    if (this.versioningDocumentId) data.set('documentId', String(this.versioningDocumentId));
    if (this.issueDate) data.set('dateEmission', this.issueDate); if (this.expiryDate) data.set('dateExpiration', this.expiryDate);
    this.uploading = true;
    this.service.uploadDocument(this.employeeId, data).pipe(finalize(() => this.uploading = false)).subscribe({ next: () => {
      this.selectedFile = null; this.issueDate = ''; this.expiryDate = ''; this.versioningDocumentId = null; this.reloadDossier();
      this.toast.success('Document enregistré', 'Le fichier est stocké dans le dossier privé du salarié.');
    }, error: () => this.toast.error('Document refusé', 'Seuls les PDF, PNG ou JPEG valides de 15 Mo maximum sont acceptés.') });
  }
  download(document: Record<string, any>): void {
    if (!this.employeeId) return;
    this.service.download(this.employeeId, Number(document['id'])).subscribe({ next: (blob) => {
      const url = URL.createObjectURL(blob); const link = documentCreateLink(url, String(document['nomFichier'] ?? 'document'));
      link.click(); URL.revokeObjectURL(url);
    }, error: () => this.toast.error('Téléchargement refusé', 'Vous ne disposez pas des droits nécessaires pour ce document.') });
  }
  downloadRecruitmentCv(): void {
    if (!this.employeeId) return;
    this.service.downloadRecruitmentCv(this.employeeId).subscribe({ next: (blob) => {
      const url = URL.createObjectURL(blob); const link = documentCreateLink(url, String(this.dossier?.['cvCandidature']?.['nomFichier'] ?? 'cv-recrutement.pdf'));
      link.click(); URL.revokeObjectURL(url);
    }, error: () => this.toast.error('CV indisponible', 'Le CV de la candidature ayant conduit à l’embauche est introuvable ou inaccessible.') });
  }
  downloadContract(contract: Record<string, any>): void {
    if (!this.employeeId) return;
    const id = Number(contract['id']);
    this.service.downloadContract(this.employeeId, id).subscribe({ next: (blob) => {
      const url = URL.createObjectURL(blob); const link = documentCreateLink(url, `contrat-${contract['numero'] ?? id}.pdf`);
      link.click(); URL.revokeObjectURL(url);
    }, error: () => this.toast.error('Contrat indisponible', 'Le PDF du contrat est introuvable ou inaccessible.') });
  }
  archive(document: Record<string, any>): void {
    if (!this.isRh || !this.employeeId || this.archiveBusy) return;
    const id = Number(document['id']); this.archiveBusy = id;
    this.service.archiveDocument(this.employeeId, id).pipe(finalize(() => this.archiveBusy = null)).subscribe({
      next: () => { this.toast.success('Document archivé', 'Le document reste conservé dans l’historique du dossier.'); this.reloadDossier(); },
      error: () => this.toast.error('Archivage impossible', 'Ce document est introuvable ou déjà archivé.')
    });
  }
  startNewVersion(document: Record<string, any>): void {
    this.versioningDocumentId = Number(document['id']);
    this.selectedTypeId = this.documentTypes.find((type) => type.code === document['typeCode'])?.id ?? null;
    this.toast.success('Nouvelle version', `Le prochain fichier remplacera la version actuelle de « ${document['type']} » tout en la conservant dans l’historique.`);
  }
  toggleVersions(document: Record<string, any>): void {
    const id = Number(document['id']);
    if (this.versionsExpandedDocumentId === id) { this.versionsExpandedDocumentId = null; return; }
    this.versionsExpandedDocumentId = id;
    if (this.versionsByDocument[id]) return;
    if (!this.employeeId) return;
    this.versionsLoadingDocumentId = id;
    this.service.documentVersions(this.employeeId, id).pipe(finalize(() => this.versionsLoadingDocumentId = null)).subscribe({
      next: (versions) => this.versionsByDocument = { ...this.versionsByDocument, [id]: versions },
      error: () => this.toast.error('Historique indisponible', 'Impossible de charger les versions autorisées de ce document.')
    });
  }
  downloadVersion(document: Record<string, any>, version: Record<string, any>): void {
    if (!this.employeeId) return;
    this.service.downloadVersion(this.employeeId, Number(document['id']), Number(version['versionId'])).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob); const link = documentCreateLink(url, String(version['nomFichier'] ?? 'document'));
        link.click(); URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Téléchargement refusé', 'Cette version est indisponible ou vous ne disposez pas des droits nécessaires.')
    });
  }
  versionsFor(documentId: number): Record<string, any>[] { return this.versionsByDocument[documentId] ?? []; }
  availableStatusOptions(current: string): Record<string, any>[] {
    const allowed: Record<string, string[]> = {
      EN_PREPARATION: ['ACTIF', 'SORTI'], ACTIF: ['EN_PREAVIS', 'SORTI'], EN_PREAVIS: ['ACTIF', 'SORTI']
    };
    const transitions = allowed[current] ?? [];
    return ((this.dossier?.['statutsDisponibles'] ?? []) as Record<string, any>[]).filter((item) => transitions.includes(item['code']));
  }
  changeStatus(): void {
    if (!this.isRh || !this.employeeId || this.savingStatus) return;
    if (!this.statusCode || !this.statusDate || !this.statusReason.trim()) {
      this.toast.error('Modification incomplète', 'Choisissez un statut, une date d’effet et indiquez le motif.'); return;
    }
    this.savingStatus = true;
    this.service.changeStatus(this.employeeId, { statut: this.statusCode, dateEffet: this.statusDate, motif: this.statusReason.trim() })
      .pipe(finalize(() => this.savingStatus = false)).subscribe({
        next: (data) => {
          this.dossier = data; this.statusCode = ''; this.statusReason = ''; this.documents = data['documents'] ?? [];
          this.toast.success('Statut mis à jour', 'Le changement a été historisé et le salarié a été notifié.');
        },
        error: () => this.toast.error('Changement refusé', 'Vérifiez la transition autorisée, la date d’effet et le statut du contrat actif.')
      });
  }
  private load(): void {
    this.loading = true;
    this.service.dossier(this.employeeId ?? undefined).pipe(finalize(() => this.loading = false)).subscribe({
      next: (data) => { this.dossier = data; this.employeeId = Number(data['id']); this.documents = data['documents'] ?? []; this.statusCode = ''; this.loadTypes(); },
      error: () => { this.dossier = null; this.toast.error('Dossier indisponible', 'Aucun dossier salarié accessible n’a été trouvé pour votre compte.'); }
    });
  }
  private reloadDossier(): void {
    if (!this.employeeId) return;
    this.service.dossier(this.self ? undefined : this.employeeId).subscribe({
      next: (data) => { this.dossier = data; this.documents = data['documents'] ?? []; },
      error: () => this.toast.error('Actualisation impossible', 'Le dossier n’a pas pu être actualisé.')
    });
  }
  private loadTypes(): void {
    if (!this.employeeId) return;
    this.service.documentTypes(this.employeeId).subscribe({ next: (items) => { this.documentTypes = items; this.selectedTypeId ??= items[0]?.id ?? null; }, error: () => this.documentTypes = [] });
  }
  private loadCorrections(): void { this.service.corrections(this.isRh).subscribe({
    next: (items) => this.corrections = this.isRh && this.employeeId ? items.filter((item) => Number(item['employeeId']) === this.employeeId) : items,
    error: () => this.corrections = []
  }); }
}

function documentCreateLink(url: string, name: string): HTMLAnchorElement {
  const link = window.document.createElement('a'); link.href = url; link.download = name; return link;
}
