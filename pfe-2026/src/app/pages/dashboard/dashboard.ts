import { Component, OnInit, OnDestroy } from '@angular/core';
import { ChartData, ChartOptions } from 'chart.js';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BaseChartDirective } from 'ng2-charts';
import { AuthService } from '../../core/services/auth';
import {
  Chart, LineController, LineElement, PointElement,
  BarController, BarElement, DoughnutController, ArcElement,
  CategoryScale, LinearScale, Legend, Tooltip, Filler
} from 'chart.js';
import { environment } from '../../../environments/environment';

Chart.register(LineController, LineElement, PointElement, BarController, BarElement,
  DoughnutController, ArcElement, CategoryScale, LinearScale, Legend, Tooltip, Filler);

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, BaseChartDirective],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit, OnDestroy {
  today = new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  isLoading = true;
  userRole: string | null = null;
  
  totalDocuments = 0;
  totalValides = 0;
  totalRejetes = 0;
  totalEnAttente = 0;
  scoreMoyen = 0;
  recentDocuments: any[] = [];
  
  private refreshInterval: any;

  kpiCards = [
    { label: 'TOTAL DOCUMENTS', value: '0', icon: 'docs', color: '#6C63FF', delta: '+0%', deltaUp: true },
    { label: 'DOCUMENTS VALIDÉS', value: '0', icon: 'success', color: '#00D4A1', delta: '+0%', deltaUp: true },
    { label: 'EN ATTENTE', value: '0', icon: 'pending', color: '#FFB84C', delta: '-0%', deltaUp: false },
    { label: 'CONFIANCE MOYENNE', value: '0%', icon: 'time', color: '#FF6B6B', delta: '+0%', deltaUp: true }
  ];

  lineChartData: ChartData<'line'> = {
    labels: ['Oct', 'Nov', 'Déc', 'Jan', 'Fév', 'Mar'],
    datasets: [{ label: 'Documents extraits', data: [0, 0, 0, 0, 0, 0], borderColor: '#6C63FF', backgroundColor: 'rgba(108,99,255,0.08)', borderWidth: 2, tension: 0.4, fill: true, pointRadius: 4, pointBackgroundColor: '#6C63FF' }]
  };
  
  lineChartOptions: ChartOptions<'line'> = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { labels: { color: '#7a7a8c' } } },
    scales: { x: { ticks: { color: '#5a5a6c' } }, y: { ticks: { color: '#5a5a6c' } } }
  };
  
  doughnutChartData: ChartData<'doughnut'> = {
    labels: ['Factures', 'Bons commande', 'Contrats', 'Formulaires', 'Autres'],
    datasets: [{ data: [0, 0, 0, 0, 0], backgroundColor: ['#6C63FF', '#00D4A1', '#FFB84C', '#FF6B6B', '#4a4a6c'], borderColor: '#161820', borderWidth: 3 }]
  };
  
  doughnutChartOptions: ChartOptions<'doughnut'> = { 
    responsive: true, maintainAspectRatio: false, cutout: '72%', 
    plugins: { legend: { position: 'bottom', labels: { color: '#7a7a8c' } } } 
  };
  
  barChartData: ChartData<'bar'> = {
    labels: ['Factures', 'Bons cmd', 'Contrats', 'Formulaires', 'Autres'],
    datasets: [{ label: 'Score moyen (%)', data: [0, 0, 0, 0, 0], backgroundColor: ['rgba(108,99,255,0.7)', 'rgba(0,212,161,0.7)', 'rgba(108,99,255,0.7)', 'rgba(255,107,107,0.7)', 'rgba(74,74,108,0.7)'], borderRadius: 6 }]
  };
  
  barChartOptions: ChartOptions<'bar'> = { 
    responsive: true, maintainAspectRatio: false, 
    plugins: { legend: { display: false } }, 
    scales: { 
      x: { grid: { display: false }, ticks: { color: '#5a5a6c' } }, 
      y: { max: 100, ticks: { color: '#5a5a6c', callback: (v) => v + '%' } } 
    } 
  };

  constructor(private http: HttpClient, private authService: AuthService) {}
  
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken');
    return new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  }

  ngOnInit() { 
    this.userRole = this.authService.getUserRole();
    this.loadDashboardData();
    this.refreshInterval = setInterval(() => {
      this.loadDashboardData();
    }, 10000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadDashboardData() {
    console.log('🔄 Chargement des données dashboard...');
    
    const documents = this.getAllDocumentsFromStorage();
    
    this.totalDocuments = documents.length;
    this.totalValides = documents.filter(d => d.statut === 'VALIDATED' || d.statut === 'Validé').length;
    this.totalRejetes = documents.filter(d => d.statut === 'REJECTED' || d.statut === 'Rejeté').length;
    this.totalEnAttente = documents.filter(d => d.statut === 'PENDING' || d.statut === 'En attente').length;
    
    const docsAvecConfiance = documents.filter(d => d.confiance && d.confiance > 0);
    if (docsAvecConfiance.length > 0) {
      const sum = docsAvecConfiance.reduce((acc, doc) => acc + doc.confiance, 0);
      this.scoreMoyen = Math.round(sum / docsAvecConfiance.length);
    } else {
      this.scoreMoyen = 0;
    }
    
    this.recentDocuments = [...documents]
      .sort((a, b) => new Date(b.dateUpload).getTime() - new Date(a.dateUpload).getTime())
      .slice(0, 5);
    
    this.updateKpiCards();
    this.updateCharts(documents);
    this.loadFromApi();
    
    this.isLoading = false;
  }

  getAllDocumentsFromStorage(): any[] {
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
              statut: data.statut || 'PENDING',
              confiance: data.confiance || 0,
              pages: data.pages || 1
            });
          }
        } catch(e) {
          console.error('Erreur chargement:', e);
        }
      }
    }
    
    return docs;
  }

  updateKpiCards() {
    this.kpiCards = [
      { label: 'TOTAL DOCUMENTS', value: this.totalDocuments.toString(), icon: 'docs', color: '#6C63FF', delta: '+12%', deltaUp: true },
      { label: 'DOCUMENTS VALIDÉS', value: this.totalValides.toString(), icon: 'success', color: '#00D4A1', delta: '+8%', deltaUp: true },
      { label: 'EN ATTENTE', value: this.totalEnAttente.toString(), icon: 'pending', color: '#FFB84C', delta: '-3%', deltaUp: false },
      { label: 'CONFIANCE MOYENNE', value: `${this.scoreMoyen}%`, icon: 'time', color: '#FF6B6B', delta: '+5%', deltaUp: true }
    ];
  }

  updateCharts(documents: any[]) {
    const monthlyData = this.getMonthlyData(documents);
    this.lineChartData.datasets[0].data = monthlyData;
    
    const typeStats = this.getTypeStats(documents);
    this.doughnutChartData.datasets[0].data = typeStats;
    
    const typeScores = this.getTypeScores(documents);
    this.barChartData.datasets[0].data = typeScores;
  }

  getMonthlyData(documents: any[]): number[] {
    const data = [0, 0, 0, 0, 0, 0];
    
    documents.forEach(doc => {
      const date = new Date(doc.dateUpload);
      const month = date.getMonth();
      if (month === 9) data[0]++;
      else if (month === 10) data[1]++;
      else if (month === 11) data[2]++;
      else if (month === 0) data[3]++;
      else if (month === 1) data[4]++;
      else if (month === 2) data[5]++;
    });
    
    return data;
  }

  getTypeStats(documents: any[]): number[] {
    const stats = [0, 0, 0, 0, 0];
    
    documents.forEach(doc => {
      const type = doc.type?.toUpperCase() || 'AUTRE';
      if (type === 'FACTURE') stats[0]++;
      else if (type === 'CONTRAT') stats[1]++;
      else if (type === 'BON_COMMANDE') stats[2]++;
      else if (type === 'FORMULAIRE') stats[3]++;
      else stats[4]++;
    });
    
    return stats;
  }

  
  getTypeScores(documents: any[]): number[] {
    const factureScores: number[] = [];
    const contratScores: number[] = [];
    const bonCommandeScores: number[] = [];
    const formulaireScores: number[] = [];
    const autreScores: number[] = [];
    
    documents.forEach(doc => {
      const type = doc.type?.toUpperCase() || 'AUTRE';
      const confiance = doc.confiance || 0;
      
      if (confiance > 0) {
        if (type === 'FACTURE') factureScores.push(confiance);
        else if (type === 'CONTRAT') contratScores.push(confiance);
        else if (type === 'BON_COMMANDE') bonCommandeScores.push(confiance);
        else if (type === 'FORMULAIRE') formulaireScores.push(confiance);
        else autreScores.push(confiance);
      }
    });
    
    const avg = (scores: number[]): number => {
      if (scores.length === 0) return 0;
      return Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
    };
    
    return [
      avg(factureScores),
      avg(contratScores),
      avg(bonCommandeScores),
      avg(formulaireScores),
      avg(autreScores)
    ];
  }

  loadFromApi() {
    this.http.get<any>(`${environment.apiUrl}/analytics/stats`, { headers: this.getHeaders() }).subscribe({
      next: (data) => {
        console.log('📡 Données API reçues:', data);
      },
      error: () => {
        console.log('📡 API non disponible');
      }
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = { 
      'VALIDATED': 'status-ok', 
      'PENDING': 'status-warn', 
      'REJECTED': 'status-err',
      'Validé': 'status-ok',
      'En attente': 'status-warn',
      'Rejeté': 'status-err'
    };
    return map[status] || 'status-warn';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = { 
      'VALIDATED': 'Validé', 
      'PENDING': 'En attente', 
      'REJECTED': 'Rejeté'
    };
    return map[status] || status;
  }

  getConfidenceClass(score: number): string {
    if (score >= 85) return 'conf-high';
    if (score >= 60) return 'conf-mid';
    return 'conf-low';
  }

  refresh() {
    if (this.isLoading) return;
    this.isLoading = true;
    console.log('🔄 Rafraîchissement du dashboard...');
    this.loadDashboardData();
    
    setTimeout(() => {
      this.isLoading = false;
    }, 1000);
  }

  canUploadDocument(): boolean {
  
    const allowedRoles = ['ADMIN', 'GESTIONNAIRE', 'OPERATEUR'];
    return this.userRole ? allowedRoles.includes(this.userRole) : false;
  }
}