// src/app/pages/upload/upload.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { DocumentRefreshService } from '../../core/services/document-refresh.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upload.html',
  styleUrls: ['./upload.scss']
})
export class Upload {
  private apiUrl = 'http://localhost:8080/api';

  errorMessage = '';
  successMessage = '';
  isDragging = false;
  selectedFile: File | null = null;
  isLoading = false;
  uploadProgress = 0;

  // Doublon détecté
  duplicateInfo: {
    id: string;
    filename: string;
    uploadedAt: string;
    uploadedBy: string;
    status: string;
  } | null = null;

  documentTypes = ['Facture', 'Contrat', 'Devis', 'Bon de commande', 'Livraison', 'Autre'];
  documentType = 'Facture';

  showAdvancedOptions = false;
  extractionParams = {
    language: 'fr',
    ocrEnabled: false,
    confidenceThreshold: 85
  };

  constructor(
    private http: HttpClient,
    private router: Router,
    private refreshService: DocumentRefreshService
  ) {}

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) this.selectedFile = files[0];
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) this.selectedFile = input.files[0];
    input.value = '';
  }

  clearFile() {
    this.selectedFile = null;
    this.errorMessage = '';
    this.successMessage = '';
    this.duplicateInfo = null;
  }

  viewDuplicate() {
    if (this.duplicateInfo?.id) {
      this.router.navigate(['/validation', this.duplicateInfo.id]);
    }
  }

  dismissDuplicate() {
    this.duplicateInfo = null;
    this.clearFile();
  }

  cancel() {
    if (this.isLoading) return;
    this.clearFile();
    this.uploadProgress = 0;
  }

  toggleAdvancedOptions() {
    this.showAdvancedOptions = !this.showAdvancedOptions;
  }

  launchExtraction() {
    if (!this.selectedFile) {
      this.errorMessage = 'Veuillez sélectionner un fichier.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.duplicateInfo = null;
    this.uploadProgress = 0;

    const token = localStorage.getItem('accessToken');
    if (!token) {
      this.errorMessage = 'Authentification requise.';
      this.isLoading = false;
      return;
    }

    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('threshold', this.extractionParams.confidenceThreshold.toString());

    const interval = setInterval(() => {
      if (this.uploadProgress < 90) this.uploadProgress += 10;
    }, 200);

    this.http.post<any>(`${this.apiUrl}/documents/upload`, formData, { headers })
      .subscribe({
        next: (result) => {
          clearInterval(interval);
          this.uploadProgress = 100;
          this.successMessage = `Fichier uploadé. Seuil: ${result.autoValidationThreshold}%`;
          this.isLoading = false;

          // Notifier le rafraîchissement de la liste des documents
          this.refreshService.notifyRefresh();

          // Attendre 2.5 secondes pour que l'extraction synchrone soit bien terminée
          if (result.id) {
            setTimeout(() => {
              this.router.navigate(['/validation', result.id]);
            }, 2500);
          }
        },
        error: (err) => {
          clearInterval(interval);
          console.error('Upload error:', err);
          // 409 = doublon détecté côté backend
          if (err.status === 409 && err.error?.error === 'DUPLICATE_DOCUMENT') {
            this.duplicateInfo = err.error.existingDocument;
          } else {
            this.errorMessage = err.error?.error || err.error?.message || 'Erreur lors de l\'upload.';
          }
          this.isLoading = false;
          this.uploadProgress = 0;
        }
      });
  }
}