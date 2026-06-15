import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DocumentStatsService } from './document-stats';

export interface NavItem {
  label: string;
  route: string;
  icon: string;
  roles?: string[];
  tooltip?: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
})
export class Sidebar implements OnInit, OnDestroy {
  currentRoute = '';
  collapsed = false;
  tooltipItem: string | null = null;
  private subs: Subscription[] = [];

  user: any = {};

  navItems: NavItem[] = [
    { label: 'Tableau de bord', route: '/dashboard', icon: 'dashboard', tooltip: 'Tableau de bord' },
    { label: 'Upload & Extraction', route: '/upload', icon: 'upload', tooltip: 'Upload & Extraction' },
    { label: 'Documents', route: '/documents', icon: 'documents', tooltip: 'Documents' },
    { label: 'Validation', route: '/validation', icon: 'validation', tooltip: 'Validation' },
    { label: 'Utilisateurs', route: '/users', icon: 'users', roles: ['ADMIN'], tooltip: 'Utilisateurs' },
    { label: 'Analytics', route: '/analytics', icon: 'analytics', tooltip: 'Analytics' },
    { label: 'API & Intégrations', route: '/api-integrations', icon: 'api', tooltip: 'API & Intégrations' },
    { label: 'Journal d\'activité', route: '/log-activity', icon: 'history', tooltip: 'Historique des actions' },
    { label: 'Paramètres', route: '/settings', icon: 'settings', tooltip: 'Paramètres' },
  ];

  constructor(private router: Router, private docStats: DocumentStatsService) {
    try {
      this.user = JSON.parse(localStorage.getItem('user') || '{}');
    } catch { this.user = {}; }
  }

  ngOnInit() {

    this.currentRoute = this.router.url;
    this.router.events.pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.currentRoute = e.url);

    this.subs.push(
      this.docStats.totalDocuments$.subscribe(() => {}),
      this.docStats.pendingValidation$.subscribe(() => {})
    );
  }

  ngOnDestroy() { this.subs.forEach(s => s.unsubscribe()); }

  isActive(route: string): boolean { return this.currentRoute.startsWith(route); }

  canSee(item: NavItem): boolean {
    if (!item.roles) return true;
    return item.roles.includes(this.user?.role);
  }

  navigate(route: string) { this.router.navigate([route]); }

  logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }

  getInitials(): string {
    const name = this.user?.username || this.user?.name || this.user?.email || 'A';
    return name.charAt(0).toUpperCase();
  }

  showTooltip(label: string) { if (this.collapsed) this.tooltipItem = label; }
  hideTooltip() { this.tooltipItem = null; }
}

