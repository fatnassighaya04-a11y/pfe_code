// src/app/pages/documents/documents.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { DocumentStatsService } from '../../shared/components/sidebar/document-stats';
import { DocumentRefreshService } from '../../core/services/document-refresh.service';
import { AuthService } from '../../core/services/auth';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './documents.html',
  styleUrls: ['./documents.scss']
})
export class Documents implements OnInit, OnDestroy {
  searchTerm = '';
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;
  documents: any[] = [];
  filteredDocuments: any[] = [];
  isLoading = true;
  errorMessage = '';
  userRole: string | null = null;

  private refreshSubscription!: Subscription;

  constructor(
    private router: Router,
    private http: HttpClient,
    private docStats: DocumentStatsService,
    private refreshService: DocumentRefreshService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.userRole = this.authService.getUserRole();
    this.loadDocuments();
    this.refreshSubscription = this.refreshService.refresh$.subscribe(() => {
      this.loadDocuments();
    });
  }

  ngOnDestroy() {
    if (this.refreshSubscription) this.refreshSubscription.unsubscribe();
  }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  loadDocuments() {
    this.isLoading = true;
    this.errorMessage = '';

    this.http.get<any>(`${environment.apiUrl}/documents`, { headers: this.getHeaders() })
      .subscribe({
        next: (response: any) => {
          console.log('📦 Réponse brute GET /documents :', response);

          // Gère les réponses paginées (content) ou tableaux directs
          let raw = Array.isArray(response) ? response : (response?.content || []);
          console.log('📄 Tableau extrait :', raw);

          this.documents = raw.map((doc: any) => ({
            id: doc.id,
            nom: doc.originalFilename || doc.filename || 'Sans nom',
            type: doc.documentType || doc.fileType || 'AUTRE',
            taille: this.formatSize(doc.fileSize),
            dateUpload: doc.createdAt || doc.uploadedAt || new Date().toISOString(),
            traitePar: doc.uploadedBy?.email || doc.createdBy || 'Système',
            statut: doc.status || 'PENDING',
            confiance: doc.confidenceScore ?? 0
          }));

          this.filteredDocuments = [...this.documents];
          this.totalPages = Math.ceil(this.documents.length / this.pageSize);
          this.currentPage = 1;

          
          this.docStats.setTotalDocuments(this.documents.length);
          const pending = this.documents.filter(d =>
            ['PENDING', 'PROCESSING', 'COMPLETED', 'UPLOADED'].includes(d.statut)
          ).length;
          this.docStats.setPendingValidation(pending);

          this.isLoading = false;
        },
        error: (err: any) => {
          console.error('❌ Erreur GET /documents :', err);
          this.errorMessage = `Erreur ${err.status} : ${err.message}`;
          this.isLoading = false;
        }
      });
  }

  private formatSize(bytes: number): string {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  filterDocuments() {
    const term = this.searchTerm.toLowerCase();
    this.filteredDocuments = term
      ? this.documents.filter(d =>
          d.nom.toLowerCase().includes(term) ||
          d.type.toLowerCase().includes(term) ||
          d.statut.toLowerCase().includes(term))
      : [...this.documents];
    this.totalPages = Math.ceil(this.filteredDocuments.length / this.pageSize);
    this.currentPage = 1;
  }

  changePage(page: number) {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
  }

  viewDocument(id: string) {
    if (this.userRole === 'LECTEUR' || this.userRole === 'LECTURE') {
      return; // Empêcher la navigation pour le rôle LECTEUR
    }
    this.router.navigate(['/validation', id]);
  }

  canViewDetails(): boolean {
    return this.userRole !== 'LECTEUR' && this.userRole !== 'LECTURE';
  }

  canUploadDocument(): boolean {
    return this.userRole !== 'LECTEUR' && this.userRole !== 'LECTURE';
  }

  get paginatedDocuments(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredDocuments.slice(start, start + this.pageSize);
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible - 1);
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      'UPLOADED': 'Uploadé',
      'PROCESSING': 'En cours',
      'COMPLETED': 'À valider',
      'VALIDATED': 'Validé',
      'REJECTED': 'Rejeté',
      'FAILED': 'Erreur',
      'PENDING': 'En attente'
    };
    return map[status] || status;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'VALIDATED': 'bg-success',
      'COMPLETED': 'bg-warning text-dark',
      'PENDING': 'bg-warning text-dark',
      'PROCESSING': 'bg-info',
      'UPLOADED': 'bg-secondary',
      'REJECTED': 'bg-danger',
      'FAILED': 'bg-dark'
    };
    return map[status] || 'bg-secondary';
  }
}