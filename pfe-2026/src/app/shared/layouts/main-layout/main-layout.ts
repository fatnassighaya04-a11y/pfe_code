import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './main-layout.html',
  styleUrls: ['./main-layout.scss']
})
export class MainLayout implements OnInit {
  sidebarCollapsed = false;
  userName: string = '';
  userInitials: string = '';
  userRole: string = '';
  private currentUserRole: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.loadUserInfo();
  }

  ngOnInit(): void {}

  loadUserInfo(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.currentUserRole = user.role;
      this.userName = this.getUserNameFromEmail(user.email);
      this.userInitials = this.getInitialsFromEmail(user.email);
      this.userRole = this.getRoleLabel(user.role);
    }
  }

  hasRole(roles: string[]): boolean {
    return roles.includes(this.currentUserRole);
  }

  getUserNameFromEmail(email: string): string {
    if (!email) return 'Utilisateur';
    const namePart = email.split('@')[0];
    return namePart
      .split(/[._-]/)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  getInitialsFromEmail(email: string): string {
    if (!email) return '?';
    const namePart = email.split('@')[0];
    if (namePart.includes('.')) {
      const parts = namePart.split('.');
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return namePart.substring(0, 2).toUpperCase();
  }

  getRoleLabel(role: string): string {
    const roles: { [key: string]: string } = {
      'ADMIN': 'Administrateur',
      'GESTIONNAIRE': 'Gestionnaire',
      'OPERATEUR': 'Opérateur',
      'LECTEUR': 'Lecteur'
    };
    return roles[role] || role;
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}