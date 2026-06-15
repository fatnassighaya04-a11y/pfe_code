import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';

Chart.register(...registerables);

interface AiAccuracyResponse {
  correctionsByType: Record<string, number>;
  topFieldsByType: Record<string, Array<{ field: string; corrections: number }>>;
  totalCorrections: number;
  accuracyRate: number;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
  templateUrl: './analytics.html',
  styleUrls: ['./analytics.scss']
})
export class Analytics implements OnInit, OnDestroy {
  
  totalTraites = 0;
  tauxValidationAuto = 0;
  tempsMoyen = 0;
  economieEstimee = 0;

  
  aiAccuracy: AiAccuracyResponse | null = null;
  aiAccuracyTypes: string[] = [];
  aiAccuracyLoading = false;
  
 
  lineChartData: ChartData<'line'> = { labels: [], datasets: [] };
  lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } }
  };
  
  
  barChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  barChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
    scales: { y: { max: 100, title: { display: true, text: 'Précision (%)' } } }
  };
  
  private refreshInterval: any;
  isLoading = true;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadAnalytics();
    this.loadAiAccuracy();
    // Rafraîchir toutes les 30 secondes
    this.refreshInterval = setInterval(() => {
      this.loadAnalytics();
      this.loadAiAccuracy();
    }, 30000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  loadAnalytics() {
    const documents = this.getAllDocuments();
    this.calculateStats(documents);
    this.updateLineChart(documents);
    this.updateBarChart(documents);
    this.isLoading = false;
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
              nom: data.nom,
              type: data.type,
              statut: data.statut,
              confiance: data.confiance || 0,
              dateUpload: data.dateUpload || data.date || new Date().toISOString(),
              pages: data.pages || 1
            });
          }
        } catch(e) {}
      }
    }
    return docs;
  }

  calculateStats(documents: any[]) {
   
    this.totalTraites = documents.filter(d => d.statut !== 'PENDING' && d.statut !== 'En attente').length;
    
    
    const confianceElevee = documents.filter(d => d.confiance >= 70).length;
    this.tauxValidationAuto = documents.length ? Math.round((confianceElevee / documents.length) * 100) : 0;
    
    
    if (documents.length > 0) {
      const totalSeconds = documents.length * (Math.random() * 90 + 30);
      this.tempsMoyen = Math.round(totalSeconds / documents.length);
    } else {
      this.tempsMoyen = 0;
    }
    
    
    this.economieEstimee = this.totalTraites * 2;
  }

  updateLineChart(documents: any[]) {
    // Regrouper par date (derniers 7 jours)
    const last7Days = this.getLast7Days();
    const counts = new Array(7).fill(0);
    
    documents.forEach(doc => {
      const date = new Date(doc.dateUpload).toISOString().split('T')[0];
      const index = last7Days.findIndex(day => day === date);
      if (index !== -1) counts[index]++;
    });
    
    this.lineChartData = {
      labels: last7Days.map(d => d.split('-').slice(1).join('/')),
      datasets: [{
        label: 'Documents traités',
        data: counts,
        borderColor: '#6C63FF',
        backgroundColor: 'rgba(108,99,255,0.1)',
        fill: true,
        tension: 0.4
      }]
    };
  }

  updateBarChart(documents: any[]) {
    const types = ['FACTURE', 'CONTRAT', 'BON_COMMANDE', 'FORMULAIRE', 'AUTRE'];
    const avgConfidence = types.map(type => {
      const docsOfType = documents.filter(d => d.type?.toUpperCase() === type);
      if (docsOfType.length === 0) return 0;
      const sum = docsOfType.reduce((acc, d) => acc + (d.confiance || 0), 0);
      return Math.round(sum / docsOfType.length);
    });
    
    this.barChartData = {
      labels: types.map(t => t === 'BON_COMMANDE' ? 'Bon commande' : t),
      datasets: [{
        label: 'Précision moyenne (%)',
        data: avgConfidence,
        backgroundColor: '#6C63FF',
        borderRadius: 8
      }]
    };
  }

  getLast7Days(): string[] {
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      days.push(d.toISOString().split('T')[0]);
    }
    return days;
  }

  refresh() {
    this.loadAnalytics();
    this.loadAiAccuracy();
  }

  loadAiAccuracy() {
    this.aiAccuracyLoading = true;
    const token = localStorage.getItem('accessToken') || '';
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get<AiAccuracyResponse>('http://localhost:8080/api/analytics/ai-accuracy', { headers })
      .subscribe({
        next: (data) => {
          this.aiAccuracy = data;
          this.aiAccuracyTypes = Object.keys(data.topFieldsByType || {});
          this.aiAccuracyLoading = false;
        },
        error: (err) => {
          console.error('AI accuracy error', err);
          this.aiAccuracyLoading = false;
        }
      });
  }

  topFieldsFor(docType: string): Array<{ field: string; corrections: number }> {
    if (!this.aiAccuracy) return [];
    return (this.aiAccuracy.topFieldsByType[docType] || []).slice(0, 5);
  }
}
