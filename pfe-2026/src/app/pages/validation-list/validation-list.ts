import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-validation-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './validation-list.html',
  styleUrls: ['./validation-list.scss']
})
export class ValidationList implements OnInit {
  searchTerm = '';
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;
  allDocuments: any[] = [];        
  filteredDocuments: any[] = [];   
  isLoading = false;
  selectedStatus: string | null = null; 

  stats = {
    total: 0,
    enAttente: 0,
    valides: 0,
    rejetes: 0
  };

  constructor(private router: Router, private http: HttpClient, private authService: AuthService) {}

  ngOnInit() {
    this.loadDocuments();
  }

  loadDocuments() {
    this.isLoading = true;
    
    const token = this.authService.getToken() || '';
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get<any>(`${environment.apiUrl}/documents`, { headers })
      .subscribe({
        next: (response: any) => {
          let raw = Array.isArray(response) ? response : (response?.content || []);
          this.allDocuments = raw.map((doc: any) => {
            const rawStatus = (doc.status || doc.statut || 'PENDING').toString();
            const statut = rawStatus ? rawStatus.toUpperCase() : 'PENDING';
            return {
              id: doc.id,
              nom: doc.originalFilename || doc.filename || doc.nom || 'Document sans nom',
              type: doc.documentType || doc.fileType || doc.type || 'DOCUMENT',
              dateUpload: doc.createdAt || doc.uploadedAt || doc.dateUpload || new Date().toISOString(),
              statut,
              confiance: Math.round((doc.confidenceScore ?? doc.confiance ?? 0)),
              taille: doc.fileSize ? this.formatSize(doc.fileSize) : (doc.taille || '0 KB')
            };
          });

          this.updateStats();
          this.applyFilters();
          this.isLoading = false;
        },
        error: () => {
          
          this.allDocuments = this.getAllDocuments();
          this.updateStats();
          this.applyFilters();
          this.isLoading = false;
        }
      });
  }

  private formatSize(bytes: number): string {
    if (!bytes) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  getAllDocuments(): any[] {
    const docs: any[] = [];

    
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && (key.startsWith('doc_') || key.startsWith('demo_'))) {
        try {
          const data = JSON.parse(localStorage.getItem(key) || '{}');
          if (data && data.id) {
            docs.push({
              id: data.id,
              nom: data.nom || 'Document sans nom',
              type: data.type || 'DOCUMENT',
              dateUpload: data.dateUpload || data.date || new Date().toISOString(),
              statut: (data.statut || 'PENDING').toString().toUpperCase(),
              confiance: data.confiance || 0,
              taille: data.taille || '0 KB'
            });
          }
        } catch(e) {}
      }
    }

    
    if (docs.length === 0) {
      const testDocs = [
        {
          id: 'doc_001',
          nom: 'Facture_ACME_2024.pdf',
          type: 'FACTURE',
          dateUpload: new Date().toISOString(),
          statut: 'PENDING',
          confiance: 87,
          taille: '245 KB'
        },
        {
          id: 'doc_002',
          nom: 'Contrat_prestation.pdf',
          type: 'CONTRAT',
          dateUpload: new Date().toISOString(),
          statut: 'PENDING',
          confiance: 67,
          taille: '1.2 MB'
        },
        {
          id: 'doc_003',
          nom: 'Facture_validée.pdf',
          type: 'FACTURE',
          dateUpload: new Date().toISOString(),
          statut: 'VALIDATED',
          confiance: 95,
          taille: '300 KB'
        },
        {
          id: 'doc_004',
          nom: 'Document_rejeté.pdf',
          type: 'AUTRE',
          dateUpload: new Date().toISOString(),
          statut: 'REJECTED',
          confiance: 12,
          taille: '120 KB'
        }
      ];

      testDocs.forEach(doc => {
        localStorage.setItem(`doc_${doc.id}`, JSON.stringify(doc));
        docs.push(doc);
      });
    }

    return docs.sort((a, b) => new Date(b.dateUpload).getTime() - new Date(a.dateUpload).getTime());
  }

  private normalize(s: string): string {
    if (!s) return 'PENDING';
    const t = s.toString().toLowerCase();
    if (t.includes('reject') || t.includes('rejet') || t.includes('failed') || t.includes('error') || t.includes('erreur')) return 'REJECTED';
    if (t.includes('valid') || t.includes('validé') || t.includes('valide') || t.includes('completed')) return 'VALIDATED';
    if (t.includes('pending') || t.includes('en attente') || t.includes('processing') || t.includes('uploaded')) return 'PENDING';
    return 'PENDING';
  }

  updateStats() {
    this.stats = {
      total: this.allDocuments.length,
      enAttente: this.allDocuments.filter(d => this.normalize(d.statut) === 'PENDING').length,
      valides: this.allDocuments.filter(d => this.normalize(d.statut) === 'VALIDATED').length,
      rejetes: this.allDocuments.filter(d => this.normalize(d.statut) === 'REJECTED').length
    };
  }

  
  applyFilters() {
    let result = [...this.allDocuments];

   
    if (this.selectedStatus) {
      result = result.filter(doc => this.normalize(doc.statut) === this.selectedStatus);
    }

    
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(doc =>
        doc.nom.toLowerCase().includes(term) ||
        doc.type.toLowerCase().includes(term)
      );
    }

    this.filteredDocuments = result;
    this.totalPages = Math.ceil(this.filteredDocuments.length / this.pageSize);
    this.currentPage = 1; // Retour à la première page après filtre
  }

  filterDocuments() {
    this.applyFilters();
  }

  
  filterByStatus(status: string | null) {
    this.selectedStatus = status;
    this.applyFilters();
  }

  changePage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  viewDocument(id: string) {
    this.router.navigate(['/validation', id]);
  }

  getStatusClass(statut: string): string {
    const map: Record<string, string> = {
      'VALIDATED': 'bg-success',
      'PENDING': 'bg-warning text-dark',
      'REJECTED': 'bg-danger'
    };
    return map[statut] || 'bg-secondary';
  }

  getStatusLabel(statut: string): string {
    const map: Record<string, string> = {
      'VALIDATED': 'Validé',
      'PENDING': 'En attente',
      'REJECTED': 'Rejeté'
    };
    return map[statut] || statut;
  }

  getConfianceClass(confiance: number): string {
    if (confiance >= 70) return 'bg-success';
    if (confiance >= 50) return 'bg-warning';
    return 'bg-danger';
  }

  refresh() {
    this.loadDocuments();
  }
}