import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface ApiKey {
  id: string;
  appName: string;
  keyPreview: string;
  isActive: boolean;
  createdAt: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
}

@Component({
  selector: 'app-api-integrations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './api-integrations.html',
  styleUrls: ['./api-integrations.scss']
})
export class ApiIntegrations implements OnInit, OnDestroy {
  baseUrl = 'http://localhost:8080/api';

 
  currentKey: ApiKey | null = null;
  isLoading = false;

  
  showCreateModal = false;
  newKeyName = '';
  newKeyExpiresInDays = 90;
  isCreating = false;

  
  freshlyCreatedKey: string | null = null;


  showRegenerateModal = false;
  isRegenerating = false;
  showRevokeModal = false;
  isRevoking = false;

  
  private refreshInterval: any;

  errorMessage = '';
  successMessage = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadKey();
  
    this.refreshInterval = setInterval(() => this.loadKey(true), 10000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  loadKey(silent = false): void {
    if (!silent) this.isLoading = true;
    this.http.get<ApiKey[]>(`${environment.apiUrl}/keys`, { headers: this.getHeaders() })
      .subscribe({
        next: (keys) => {
          
          const activeKeys = keys.filter(k => k.isActive);
          this.currentKey = activeKeys.length > 0 ? activeKeys[0] : null;
          this.isLoading = false;
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading = false;
          if (!silent) this.showError(`Erreur ${err.status} : chargement impossible`);
        }
      });
  }


  openCreateModal(): void {
    this.newKeyName = '';
    this.newKeyExpiresInDays = 90;
    this.showCreateModal = true;
  }

  closeCreateModal(): void { this.showCreateModal = false; }

  createKey(): void {
    if (!this.newKeyName.trim()) {
      this.showError('Donnez un nom à votre application');
      return;
    }
    this.isCreating = true;
    this.http.post<any>(`${environment.apiUrl}/keys/generate`,
      { appName: this.newKeyName.trim(), expiresInDays: this.newKeyExpiresInDays },
      { headers: this.getHeaders() }
    ).subscribe({
      next: (res) => {
        this.isCreating = false;
        this.closeCreateModal();
        this.freshlyCreatedKey = res.keyValue;
        this.loadKey();
        this.showSuccess('Clé créée — copiez-la maintenant');
      },
      error: (err: HttpErrorResponse) => {
        this.isCreating = false;
        this.showError(`Erreur ${err.status} : ${err.error?.message || 'création impossible'}`);
      }
    });
  }

  dismissFreshKey(): void { this.freshlyCreatedKey = null; }


  openRegenerateModal(): void {
    if (!this.currentKey) return;
    this.showRegenerateModal = true;
  }

  closeRegenerateModal(): void {
    if (this.isRegenerating) return;
    this.showRegenerateModal = false;
  }

  confirmRegenerate(): void {
    if (!this.currentKey) return;
    const oldKey = this.currentKey;
    const appName = oldKey.appName;
    const expiresInDays = oldKey.expiresAt
      ? Math.max(1, Math.ceil((new Date(oldKey.expiresAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24)))
      : 90;

    this.isRegenerating = true;
    
    this.http.delete(`${environment.apiUrl}/keys/${oldKey.id}`, { headers: this.getHeaders() })
      .subscribe({
        next: () => {
          
          this.http.post<any>(`${environment.apiUrl}/keys/generate`,
            { appName, expiresInDays },
            { headers: this.getHeaders() }
          ).subscribe({
            next: (res) => {
              this.isRegenerating = false;
              this.showRegenerateModal = false;
              this.freshlyCreatedKey = res.keyValue;
              this.loadKey();
              this.showSuccess('Clé régénérée — copiez la nouvelle maintenant');
            },
            error: (err: HttpErrorResponse) => {
              this.isRegenerating = false;
              this.showError(`Erreur ${err.status} : recréation échouée`);
            }
          });
        },
        error: (err: HttpErrorResponse) => {
          this.isRegenerating = false;
          this.showError(`Erreur ${err.status} : révocation échouée`);
        }
      });
  }


  openRevokeModal(): void {
    if (!this.currentKey) return;
    this.showRevokeModal = true;
  }

  closeRevokeModal(): void {
    if (this.isRevoking) return;
    this.showRevokeModal = false;
  }

  confirmRevoke(): void {
    if (!this.currentKey) return;
    this.isRevoking = true;
    this.http.delete(`${environment.apiUrl}/keys/${this.currentKey.id}`, { headers: this.getHeaders() })
      .subscribe({
        next: () => {
          this.isRevoking = false;
          this.showRevokeModal = false;
          this.showSuccess('Clé révoquée');
          this.currentKey = null;
          this.loadKey();
        },
        error: (err: HttpErrorResponse) => {
          this.isRevoking = false;
          this.showError(`Erreur ${err.status} : révocation échouée`);
        }
      });
  }

  
  copy(text: string, label: string = 'Texte'): void {
    navigator.clipboard.writeText(text).then(() => {
      this.showSuccess(`${label} copié`);
    });
  }

  
  curlUpload(): string {
    return `curl -X POST ${this.baseUrl}/documents/upload \\
  -H "Authorization: Bearer VOTRE_CLE_API" \\
  -F "file=@facture.pdf" \\
  -F "threshold=85"`;
  }

  curlGetExtraction(): string {
    return `curl -X GET ${this.baseUrl}/extractions/DOCUMENT_ID \\
  -H "Authorization: Bearer VOTRE_CLE_API"`;
  }

  
 
  isCurrentlyUsed(): boolean {
    if (!this.currentKey?.lastUsedAt) return false;
    const last = new Date(this.currentKey.lastUsedAt).getTime();
    return (Date.now() - last) < 5 * 60 * 1000;
  }

  
  lastUsedLabel(): string {
    if (!this.currentKey?.lastUsedAt) return 'Jamais utilisée';
    const diffMs = Date.now() - new Date(this.currentKey.lastUsedAt).getTime();
    const seconds = Math.floor(diffMs / 1000);
    if (seconds < 60) return `Il y a ${seconds} seconde${seconds > 1 ? 's' : ''}`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `Il y a ${minutes} minute${minutes > 1 ? 's' : ''}`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `Il y a ${hours} heure${hours > 1 ? 's' : ''}`;
    const days = Math.floor(hours / 24);
    return `Il y a ${days} jour${days > 1 ? 's' : ''}`;
  }

  showSuccess(msg: string): void {
    this.successMessage = msg;
    this.errorMessage = '';
    setTimeout(() => this.successMessage = '', 3000);
  }

  showError(msg: string): void {
    this.errorMessage = msg;
    this.successMessage = '';
    setTimeout(() => this.errorMessage = '', 5000);
  }
}
