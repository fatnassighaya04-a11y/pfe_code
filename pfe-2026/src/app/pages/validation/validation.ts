// src/app/pages/validation/validation.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';

interface ExtractedField {
  key: string;
  label: string;
  valeur: string;
  original: string;
  isModified: boolean;
}

const DEFAULT_FIELD_TEMPLATES: Record<string, Array<{ key: string; label: string }>> = {
  FACTURE: [
    { key: 'numero_facture', label: 'Numéro facture' },
    { key: 'date_emission', label: 'Date émission' },
    { key: 'date_echeance', label: 'Date échéance' },
    { key: 'fournisseur', label: 'Fournisseur' },
    { key: 'adresse_fournisseur', label: 'Adresse fournisseur' },
    { key: 'client', label: 'Client' },
    { key: 'montant_ht', label: 'Montant HT' },
    { key: 'montant_tva', label: 'Montant TVA' },
    { key: 'montant_ttc', label: 'Montant TTC' },
    { key: 'mode_paiement', label: 'Mode paiement' },
  ],
  BON_COMMANDE: [
    { key: 'numero_bc', label: 'Numéro bon de commande' },
    { key: 'date_commande', label: 'Date commande' },
    { key: 'fournisseur', label: 'Fournisseur' },
    { key: 'client', label: 'Client' },
    { key: 'articles', label: 'Articles' },
    { key: 'quantites', label: 'Quantités' },
    { key: 'montant_total', label: 'Montant total' },
    { key: 'conditions_livraison', label: 'Conditions livraison' },
  ],
  CONTRAT: [
    { key: 'parties', label: 'Parties' },
    { key: 'objet_contrat', label: 'Objet du contrat' },
    { key: 'date_debut', label: 'Date début' },
    { key: 'date_fin', label: 'Date fin' },
    { key: 'duree', label: 'Durée' },
    { key: 'montant', label: 'Montant' },
    { key: 'conditions_paiement', label: 'Conditions paiement' },
    { key: 'clauses_importantes', label: 'Clauses importantes' },
  ],
  default: [
    { key: 'numero', label: 'Numéro' },
    { key: 'date', label: 'Date' },
    { key: 'nom', label: 'Nom' },
    { key: 'adresse', label: 'Adresse' },
    { key: 'montant', label: 'Montant' },
    { key: 'reference', label: 'Référence' },
  ]
};

@Component({
  selector: 'app-validation',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validation.html',
  styleUrls: ['./validation.scss']
})
export class Validation implements OnInit, OnDestroy {
  private apiUrl = 'http://localhost:8080/api';
  private previewObjectUrl: string | null = null;

  documentId = '';
  extractionId = '';

  document: any = null;
  extraction: any = null;
  documentType = '';

  extractedFields: ExtractedField[] = [];

  isLoading = true;
  isSaving = false;
  isApproved = false;
  isRejected = false;
  isAutoValidated = false;
  needsManualValidation = false;

  confidenceScore = 0;
  autoValidationThreshold = 85;

  pdfUrl: SafeResourceUrl | null = null;
  showPdf = false;
  previewMimeType = 'application/pdf';

  rejectionReasons = [
    { value: 'DOCUMENT_ILLISIBLE',  label: '📄 Document illisible ou de mauvaise qualité' },
    { value: 'CHAMPS_INCORRECTS',   label: '🔍 Champs extraits incorrects ou incomplets' },
    { value: 'DOUBLON',             label: '🔄 Document déjà existant (doublon)' },
    { value: 'NON_CONFORME',        label: '⚠️ Document non conforme aux exigences' },
    { value: 'FRAUDE_SUSPECTEE',    label: '🚨 Fraude ou anomalie suspectée' },
    { value: 'AUTRE',               label: '📝 Autre (précisez dans le commentaire)' }
  ];
  selectedRejectionReason = '';
  comment = '';

  showConfirmModal = false;
  confirmTitle = '';
  confirmMessage = '';
  confirmAction: (() => void) | null = null;

  showNotificationModal = false;
  notificationTitle = '';
  notificationMessage = '';
  notificationType: 'success' | 'error' | 'info' = 'success';

  private pollingSubscription: Subscription | null = null;
  private maxAttempts = 15;
  private attempt = 0;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private sanitizer: DomSanitizer,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.documentId = this.route.snapshot.paramMap.get('id') || '';
    if (this.documentId) {
      this.loadDocument();
      this.startPolling();
    } else {
      this.isLoading = false;
      this.showNotification('Erreur', 'Aucun ID de document spécifié.', 'error');
    }
  }

  ngOnDestroy() {
    this.stopPolling();
    this.revokePreviewUrl();
  }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  loadDocument() {
    this.http.get<any>(`${this.apiUrl}/documents/${this.documentId}/status`, { headers: this.getHeaders() })
      .subscribe({
        next: (doc) => {
          this.document = doc;
          this.documentType = String(doc.documentType || '').toUpperCase();
          this.loadRealPreview();
        },
        error: (err) => {
          console.error('Erreur chargement document:', err);
          this.isLoading = false;
          this.showNotification('Erreur', 'Document introuvable.', 'error');
        }
      });
  }

  loadExtraction() {
    this.http.get<any>(`${this.apiUrl}/extractions/${this.documentId}`, { headers: this.getHeaders() })
      .subscribe({
        next: (data) => {
          console.log('Extraction reçue:', data);
          this.extraction = data;
          this.extractionId = data.id;
          this.confidenceScore = data.confidenceScore ?? 0;
          this.isAutoValidated = data.autoValidated === true;
          this.isApproved = data.isValidated && !this.isRejected;
          this.autoValidationThreshold = data.thresholdUsed ?? 85;
          this.needsManualValidation = !data.isValidated && this.confidenceScore < this.autoValidationThreshold;

          const parsed = this.parseExtractedFields(data.extractedData, data.status);
          if (parsed && (this.confidenceScore > 0 || data.status === 'SUCCESS' || data.status === 'FAILED')) {
            this.stopPolling();
            this.loadRealPreview();
            this.isLoading = false;
          } else if (!parsed && this.attempt < this.maxAttempts) {
            console.log(`En attente de l'extraction... tentative ${this.attempt + 1}/${this.maxAttempts}`);
          } else if (this.attempt >= this.maxAttempts) {
            this.stopPolling();
            this.isLoading = false;
            this.showNotification('Erreur', 'L\'extraction a pris trop de temps ou a échoué.', 'error');
          }
        },
        error: (err) => {
          console.error('Erreur chargement extraction:', err);
          if (this.attempt >= this.maxAttempts) {
            this.stopPolling();
            this.isLoading = false;
            this.showNotification('Erreur', 'Impossible de récupérer les données d\'extraction.', 'error');
          }
        }
      });
  }

  parseExtractedFields(jsonData: string, extractionStatus: string): boolean {
    if (!jsonData || jsonData.trim() === '') {
      console.log('Pas de données JSON');
      if (extractionStatus === 'PENDING' || extractionStatus === 'IN_PROGRESS') {
        return false;
      }
      this.loadSkeletonFields();
      return true;
    }
    try {
      let clean = jsonData.trim();
      // Supprimer les marqueurs markdown
      if (clean.startsWith('```json')) clean = clean.substring(7);
      if (clean.startsWith('```')) clean = clean.substring(3);
      if (clean.endsWith('```')) clean = clean.substring(0, clean.length - 3);
      // Supprimer les retours à la ligne et tabulations
      clean = clean.replace(/[\n\r\t]/g, ' ');
      // Remplacer les guillemets mal formés (exemple: "valeur" -> "valeur")
      // Essayer de réparer les JSON mal formés (manque de virgules, etc.) – basique
      // On tente un parsing direct
      const parsed = JSON.parse(clean);
      if (typeof parsed === 'object' && parsed !== null) {
        const entries = Object.entries(parsed)
          .filter(([key, v]) => v !== null && v !== undefined && !['erreur', 'error', 'statut', 'status', 'type_document', 'apercu'].includes(key.toLowerCase()));

        if (!entries.length) {
          if (extractionStatus === 'PENDING' || extractionStatus === 'IN_PROGRESS') {
            return false;
          }
          this.loadSkeletonFields();
          return true;
        }

        this.extractedFields = entries.map(([key, value]) => ({
          key,
          label: key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()),
          valeur: String(value),
          original: String(value),
          isModified: false
        }));
        console.log('✅ Champs extraits avec succès:', this.extractedFields);
        return true;
      }
      if (extractionStatus === 'PENDING' || extractionStatus === 'IN_PROGRESS') {
        return false;
      }
      this.loadSkeletonFields();
      return true;
    } catch (e) {
      console.error('❌ Erreur parsing JSON:', e);
      console.error('JSON reçu:', jsonData);
      if (extractionStatus === 'PENDING' || extractionStatus === 'IN_PROGRESS') {
        return false;
      }
      this.loadSkeletonFields();
      return true;
    }
  }

  private loadSkeletonFields() {
    const template = DEFAULT_FIELD_TEMPLATES[this.documentType] || DEFAULT_FIELD_TEMPLATES['default'];
    this.extractedFields = template.map(field => ({
      key: field.key,
      label: field.label,
      valeur: '',
      original: '',
      isModified: false
    }));
    console.log('🧩 Squelette de champs généré pour', this.documentType || 'DEFAULT');
  }

  startPolling() {
    if (this.pollingSubscription) return;
    this.pollingSubscription = interval(2000).subscribe(() => {
      this.attempt++;
      if (this.attempt <= this.maxAttempts) {
        this.loadExtraction();
      } else {
        this.stopPolling();
        this.isLoading = false;
        this.showNotification('Erreur', 'L\'extraction n\'a pas abouti.', 'error');
      }
    });
  }

  stopPolling() {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
    }
  }

  private loadRealPreview() {
    if (!this.documentId) return;
    this.http.get(`${this.apiUrl}/documents/${this.documentId}/file`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        this.revokePreviewUrl();
        this.previewMimeType = blob.type || 'application/pdf';
        this.previewObjectUrl = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.previewObjectUrl);
        this.showPdf = true;
      },
      error: (err) => {
        console.error('Erreur chargement aperçu réel:', err);
        this.showPdf = false;
        this.showNotification('Aperçu indisponible', 'Impossible de charger le fichier réel.', 'error');
      }
    });
  }

  private revokePreviewUrl() {
    if (this.previewObjectUrl) {
      URL.revokeObjectURL(this.previewObjectUrl);
      this.previewObjectUrl = null;
    }
  }

  onFieldChange(field: ExtractedField) {
    field.isModified = field.valeur !== field.original;
  }

  get hasModifications(): boolean {
    return this.extractedFields.some(f => f.isModified);
  }

  get modifiedCount(): number {
    return this.extractedFields.filter(f => f.isModified).length;
  }

  saveFieldCorrections() {
    if (!this.extractionId) return;
    this.isSaving = true;
    const fieldsMap: { [key: string]: string } = {};
    this.extractedFields.forEach(f => { fieldsMap[f.key] = f.valeur; });
    this.http.put(`${this.apiUrl}/extractions/${this.extractionId}/update-fields`,
      { fields: fieldsMap },
      { headers: this.getHeaders() }
    ).subscribe({
      next: () => {
        this.isSaving = false;
        this.extractedFields.forEach(f => { f.original = f.valeur; f.isModified = false; });
        this.showNotification('✅ Sauvegardé', 'Corrections enregistrées.', 'success');
      },
      error: () => {
        this.isSaving = false;
        this.showNotification('Erreur', 'Impossible de sauvegarder.', 'error');
      }
    });
  }

  resetFieldCorrections() {
    this.extractedFields.forEach(f => { f.valeur = f.original; f.isModified = false; });
  }

  approuver() {
    if (this.isApproved || this.isRejected) {
      this.showNotification('Action impossible', 'Document déjà traité.', 'error');
      return;
    }
    if (!this.extractionId) {
      this.showNotification('Extraction en attente', 'L\'extraction n\'est pas encore prête — réessayez dans quelques instants.', 'error');
      return;
    }
    this.openConfirmModal('Approbation', 'Voulez-vous vraiment approuver ce document ?', () => this.executeApprove());
  }

  private executeApprove() {
    if (!this.extractionId) {
      this.showNotification('Erreur', 'ID d’extraction manquant', 'error');
      return;
    }
    this.isSaving = true;

    const fieldsMap: { [key: string]: string } = {};
    this.extractedFields.forEach(f => { fieldsMap[f.key] = f.valeur; });
    const payload: any = { action: 'APPROVE', notes: this.comment || 'Validé manuellement' };

    const saveAndValidate = () => {
      this.http.put(`${this.apiUrl}/extractions/${this.extractionId}/validate`, payload, { headers: this.getHeaders() })
        .subscribe({
          next: () => {
            this.isSaving = false;
            this.isApproved = true;
            this.stopPolling();
            this.showNotification('Document approuvé', 'Validation réussie.', 'success');
          },
          error: (err) => {
            this.isSaving = false;
            console.error('Erreur validation:', err);
            const msg = err.error?.message || err.message || 'Erreur serveur';
            this.showNotification('Erreur', `Impossible de valider : ${msg}`, 'error');
          }
        });
    };

    if (this.hasModifications) {
      this.http.put(`${this.apiUrl}/extractions/${this.extractionId}/update-fields`, { fields: fieldsMap }, { headers: this.getHeaders() })
        .subscribe({ next: saveAndValidate, error: saveAndValidate });
    } else {
      saveAndValidate();
    }
  }

  rejeter() {
    if (this.isApproved || this.isRejected) {
      this.showNotification('Action impossible', 'Document déjà traité.', 'error');
      return;
    }
    if (!this.extractionId) {
      this.showNotification('Extraction en attente', 'L\'extraction n\'est pas encore prête — réessayez dans quelques instants.', 'error');
      return;
    }
    if (!this.selectedRejectionReason) {
      this.showNotification('Motif requis', 'Sélectionnez une cause de rejet.', 'error');
      return;
    }
    if (this.selectedRejectionReason === 'AUTRE' && !this.comment.trim()) {
      this.showNotification('Commentaire requis', 'Précisez la raison.', 'error');
      return;
    }
    const reasonObj = this.rejectionReasons.find(r => r.value === this.selectedRejectionReason);
    const reasonLabel = reasonObj ? reasonObj.label : this.selectedRejectionReason;
    const fullReason = this.comment.trim() ? `${reasonLabel} — ${this.comment.trim()}` : reasonLabel;

    this.openConfirmModal('Rejet', `Voulez-vous rejeter ce document ?\nMotif : "${fullReason}"`, () => {
      if (!this.extractionId) return;
      this.isSaving = true;
      this.http.put(`${this.apiUrl}/extractions/${this.extractionId}/validate`,
        { action: 'REJECT', rejectionReason: fullReason, notes: this.comment },
        { headers: this.getHeaders() }
      ).subscribe({
        next: () => {
          this.isSaving = false;
          this.isRejected = true;
          this.stopPolling();
          this.showNotification('Document rejeté', 'Le document a été rejeté.', 'success');
        },
        error: (err) => {
          this.isSaving = false;
          console.error('Erreur rejet:', err);
          const msg = err.error?.message || err.message || 'Erreur serveur';
          this.showNotification('Erreur', `Impossible de rejeter : ${msg}`, 'error');
        }
      });
    });
  }

  exporterJSON() {
    if (!this.extractedFields.length) return;
    const data: any = {};
    this.extractedFields.forEach(f => { data[f.key] = f.valeur; });
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `extraction_${this.documentId.substring(0, 8)}.json`; a.click();
    URL.revokeObjectURL(url);
    this.showNotification('Export JSON', 'Fichier téléchargé.', 'success');
  }

  exporterCSV() {
    if (!this.extractedFields.length) return;
    let csv = 'Champ,Valeur\n';
    this.extractedFields.forEach(f => {
      const v = f.valeur.includes(',') ? `"${f.valeur}"` : f.valeur;
      csv += `"${f.label}",${v}\n`;
    });
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `extraction_${this.documentId.substring(0, 8)}.csv`; a.click();
    URL.revokeObjectURL(url);
    this.showNotification('Export CSV', 'Fichier téléchargé.', 'success');
  }

  downloadPreview() {
    this.http.get(`${this.apiUrl}/documents/${this.documentId}/file`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.document?.filename || `document_${this.documentId.substring(0, 8)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.showNotification('Erreur', 'Téléchargement impossible.', 'error');
      }
    });
  }

  openConfirmModal(title: string, message: string, action: () => void) {
    this.confirmTitle = title;
    this.confirmMessage = message;
    this.confirmAction = action;
    this.showConfirmModal = true;
  }

  executeConfirmAction() { if (this.confirmAction) this.confirmAction(); this.closeConfirmModal(); }
  closeConfirmModal() { this.showConfirmModal = false; this.confirmTitle = this.confirmMessage = ''; }

  showNotification(title: string, message: string, type: 'success' | 'error' | 'info' = 'success') {
    this.notificationTitle = title;
    this.notificationMessage = message;
    this.notificationType = type;
    this.showNotificationModal = true;
    setTimeout(() => { this.showNotificationModal = false; }, 3500);
  }
  closeNotificationModal() { this.showNotificationModal = false; }

  formatSize(bytes: number): string {
    if (!bytes) return '—';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  get confidenceClass(): string {
    if (this.confidenceScore >= 85) return 'text-success';
    if (this.confidenceScore >= 70) return 'text-warning';
    return 'text-danger';
  }

  get confidenceBarClass(): string {
    if (this.confidenceScore >= 85) return 'bg-success';
    if (this.confidenceScore >= 70) return 'bg-warning';
    return 'bg-danger';
  }

  goToDocuments()    { this.router.navigate(['/documents']); }
  resetAndContinue() { this.router.navigate(['/upload']); }
}