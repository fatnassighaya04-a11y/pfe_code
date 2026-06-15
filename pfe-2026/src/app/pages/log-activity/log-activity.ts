import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface AuditLog {
  id: string;
  action: string;
  resourceType: string;
  resourceId: string;
  result: string;
  ipAddress: string;
  details?: string;
  createdAt: string;
  userEmail: string;
}

@Component({
  selector: 'app-log-activity',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './log-activity.html',
  styleUrl: './log-activity.scss',
})
export class LogActivity implements OnInit {
  private apiUrl = 'http://localhost:8080/api/audit';
  private readonly actionCatalog: string[] = [
    'USER_LOGIN',
    'USER_LOGOUT',
    'USER_REGISTER',
    'USER_APPROVE',
    'USER_REJECT',
    'USER_ACTIVATE',
    'USER_DEACTIVATE',
    'USER_ROLE_CHANGE',
    'USER_STATUS_CHANGE',
    'USER_LOCK',
    'USER_UNLOCK',
    'USER_DELETE',
    'USER_UPDATE',
    'DOCUMENT_UPLOAD',
    'DOCUMENT_UPDATE',
    'DOCUMENT_DELETE',
    'DOCUMENT_DOWNLOAD',
    'EXTRACTION_START',
    'EXTRACTION_VALIDATE',
    'EXTRACTION_APPROVE',
    'EXTRACTION_REJECT',
    'EXTRACTION_UPDATE_FIELDS',
    'API_KEY_GENERATE',
    'API_KEY_REVOKE',
    'SETTINGS_UPDATE',
  ];
  logs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  actionOptions: string[] = [];
  isLoading = false;
  currentPage = 0;
  totalPages = 1;
  totalElements = 0;
  pageSize = 20;
  searchTerm = '';
  selectedAction = '';
  selectedResult = '';

  constructor(private http: HttpClient) {}

  ngOnInit() { this.loadLogs(); }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  loadLogs() {
    this.isLoading = true;
    let url = `${this.apiUrl}?page=${this.currentPage}&size=${this.pageSize}`;
    if (this.selectedAction) {
      url += `&action=${encodeURIComponent(this.selectedAction)}`;
    }
    this.http.get<any>(url, {
      headers: this.getHeaders()
    }).subscribe({
      next: (data) => {
        this.logs = data.content || [];
        this.buildActionOptions();
        this.totalElements = data.totalElements || 0;
        this.totalPages = data.totalPages || 1;
        this.filterLogs();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement audit logs:', err);
        this.isLoading = false;
      }
    });
  }

  filterLogs() {
    this.filteredLogs = this.logs.filter(log => {
      const matchSearch = !this.searchTerm ||
        log.action?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        log.userEmail?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        log.ipAddress?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        log.details?.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchAction = !this.selectedAction || log.action === this.selectedAction;
      const matchResult = !this.selectedResult || log.result === this.selectedResult;

      return matchSearch && matchAction && matchResult;
    });
  }

  private buildActionOptions() {
    const set = new Set<string>(this.actionCatalog);
    for (const l of this.logs) {
      if (l.action) set.add(l.action);
    }
    this.actionOptions = Array.from(set).sort();
  }

  clearFilters() {
    this.searchTerm = '';
    this.selectedAction = '';
    this.selectedResult = '';
    this.currentPage = 0;
    this.loadLogs();
    this.filterLogs();
  }

  onActionChange() {
    this.currentPage = 0;
    this.loadLogs();
  }

  changePage(page: number) {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadLogs();
  }

  getPageNumbers(): number[] {
    const pages = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  get successCount(): number {
    return this.logs.filter(l => l.result === 'SUCCESS').length;
  }

  get failureCount(): number {
    return this.logs.filter(l => l.result === 'FAILURE').length;
  }

  getActionLabel(action: string): string {
    const map: { [key: string]: string } = {
      'USER_LOGIN': '🔑 Connexion',
      'USER_LOGOUT': '🚪 Déconnexion',
      'USER_REGISTER': '📝 Inscription',
      'USER_APPROVE': '✅ Approbation utilisateur',
      'USER_REJECT': '❌ Rejet utilisateur',
      'USER_ACTIVATE': '🟢 Activation utilisateur',
      'USER_DEACTIVATE': '⛔ Désactivation utilisateur',
      'USER_ROLE_CHANGE': '🛡️ Modification rôle utilisateur',
      'USER_STATUS_CHANGE': '🔁 Modification statut utilisateur',
      'USER_LOCK': '🔒 Blocage utilisateur',
      'USER_UNLOCK': '🔓 Déblocage utilisateur',
      'USER_DELETE': '🗑️ Suppression utilisateur',
      'USER_UPDATE': '✏️ Modification utilisateur',
      'DOCUMENT_UPLOAD': '📤 Upload',
      'DOCUMENT_UPDATE': '✏️ Modification document',
      'DOCUMENT_DELETE': '🗑️ Suppression document',
      'DOCUMENT_DOWNLOAD': '⬇️ Téléchargement document',
      'EXTRACTION_START': '🤖 Extraction IA',
      'EXTRACTION_VALIDATE': '✅ Validation extraction',
      'EXTRACTION_APPROVE': '✅ Approbation extraction',
      'EXTRACTION_REJECT': '❌ Rejet extraction',
      'EXTRACTION_UPDATE_FIELDS': '✏️ Correction extraction',
      'API_KEY_GENERATE': '🔑 Génération clé API',
      'API_KEY_REVOKE': '🗝️ Révocation clé API',
      'SETTINGS_UPDATE': '⚙️ Mise à jour paramètres',
    };
    return map[action] || action;
  }

  getActionClass(action: string): string {
    if (action?.includes('LOGIN') || action?.includes('LOGOUT')) return 'action-auth';
    if (action?.includes('REGISTER')) return 'action-register';
    if (action?.includes('UPLOAD') || action?.includes('DOWNLOAD') || action?.includes('DOCUMENT')) return 'action-doc';
    if (action?.includes('APPROVE') || action?.includes('REJECT') || action?.includes('DELETE') || action?.includes('VALIDATE')) return 'action-admin';
    return 'action-default';
  }
}