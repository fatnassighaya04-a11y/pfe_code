import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface User {
  id: string;
  username: string;
  email: string;
  role: string;
  accountStatus: string;
  isActive: boolean;
  createdAt: string;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrls: ['./users.scss']
})
export class UsersComponent implements OnInit, OnDestroy {
  allUsers: User[] = [];
  activeUsers: User[] = [];
  pendingUsers: User[] = [];
  filteredUsers: User[] = [];

  searchTerm = '';
  isLoading = true;
  errorMessage = '';
  successMessage = '';
  activeTab: 'active' | 'pending' = 'active';

  selectedUser: User | null = null;
  showEditModal = false;

  roleStats = { ADMIN: 0, GESTIONNAIRE: 0, OPERATEUR: 0, LECTEUR: 0 };
  pendingCount = 0;

  private refreshInterval: any;

  
  showConfirmModal = false;
  confirmTitle = '';
  confirmMessage = '';
  confirmAction: (() => void) | null = null;

  showNotificationModal = false;
  notificationTitle = '';
  notificationMessage = '';
  notificationType: 'success' | 'error' = 'success';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadUsers();
    this.refreshInterval = setInterval(() => this.loadUsers(), 15000);
  }

  ngOnDestroy() { if (this.refreshInterval) clearInterval(this.refreshInterval); }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  loadUsers() {
    this.isLoading = true;
    this.errorMessage = '';
    this.http.get<any>(`${environment.apiUrl}/users`, { headers: this.getHeaders() })
      .subscribe({
        next: (response) => {
          let users: User[] = Array.isArray(response) ? response : (response?.content ?? response?.data ?? []);
          this.allUsers = users;
          this.pendingUsers = users.filter(u => u.accountStatus === 'PENDING');
          this.activeUsers = users.filter(u => u.accountStatus !== 'PENDING');
          this.pendingCount = this.pendingUsers.length;
          this.applyFilter();
          this.updateRoleStats();
          this.isLoading = false;
        },
        error: (err) => {
          this.isLoading = false;
          this.showNotification('Erreur', err.error?.message || `Erreur ${err.status}`, 'error');
        }
      });
  }

  
  openConfirmModal(title: string, message: string, action: () => void) {
    this.confirmTitle = title;
    this.confirmMessage = message;
    this.confirmAction = action;
    this.showConfirmModal = true;
  }

  executeConfirmAction() {
    if (this.confirmAction) {
      this.confirmAction();
      this.confirmAction = null;
    }
    this.closeConfirmModal();
  }

  closeConfirmModal() {
    this.showConfirmModal = false;
    this.confirmTitle = '';
    this.confirmMessage = '';
  }

  
  showNotification(title: string, message: string, type: 'success' | 'error' = 'success') {
    this.notificationTitle = title;
    this.notificationMessage = message;
    this.notificationType = type;
    this.showNotificationModal = true;
    setTimeout(() => {
      this.showNotificationModal = false;
    }, 3000);
  }

  closeNotificationModal() {
    this.showNotificationModal = false;
  }

  
  approveUser(user: User) {
    this.openConfirmModal(
      'Approuver un compte',
      `Voulez-vous vraiment approuver le compte de ${user.username} ?`,
      () => {
        this.http.put(`${environment.apiUrl}/users/${user.id}/approve`, {}, { headers: this.getHeaders() })
          .subscribe({
            next: () => {
              this.showNotification('Succès', `${user.username} approuvé avec succès !`, 'success');
              this.loadUsers();
            },
            error: (err) => {
              this.showNotification('Erreur', err.error?.message || 'Erreur lors de l\'approbation', 'error');
            }
          });
      }
    );
  }

  rejectUser(user: User) {
    this.openConfirmModal(
      'Rejeter un compte',
      `Voulez-vous vraiment rejeter le compte de ${user.username} ? Cette action est irréversible.`,
      () => {
        this.http.put(`${environment.apiUrl}/users/${user.id}/reject`, {}, { headers: this.getHeaders() })
          .subscribe({
            next: () => {
              this.showNotification('Rejet effectué', `${user.username} a été rejeté.`, 'success');
              this.loadUsers();
            },
            error: (err) => {
              this.showNotification('Erreur', err.error?.message || 'Erreur lors du rejet', 'error');
            }
          });
      }
    );
  }

  editUser(user: User) {
    
    this.selectedUser = { ...user };
    this.showEditModal = true;
  }

  saveUserChanges() {
    if (!this.selectedUser) return;
    this.http.put(`${environment.apiUrl}/users/${this.selectedUser.id}`,
      { role: this.selectedUser.role, accountStatus: this.selectedUser.accountStatus },
      { headers: this.getHeaders() }
    ).subscribe({
      next: () => {
        this.showNotification('Modification', `${this.selectedUser!.username} modifié avec succès !`, 'success');
        this.closeModal();
        this.loadUsers();
      },
      error: (err) => {
        this.showNotification('Erreur', err.error?.message || 'Erreur lors de la modification', 'error');
      }
    });
  }

  deleteUser(user: User) {
    this.openConfirmModal(
      'Supprimer un utilisateur',
      `⚠️ Attention : Voulez-vous vraiment supprimer définitivement ${user.username} ? Cette action est irréversible.`,
      () => {
        this.http.delete(`${environment.apiUrl}/users/${user.id}`, { headers: this.getHeaders() })
          .subscribe({
            next: () => {
              this.showNotification('Suppression', `${user.username} a été supprimé.`, 'success');
              this.loadUsers();
            },
            error: (err) => {
              this.showNotification('Erreur', err.error?.message || 'Erreur lors de la suppression', 'error');
            }
          });
      }
    );
  }

  toggleUserStatus(user: User) {
    const newStatus = user.isActive ? 'INACTIVE' : 'ACTIVE';
    const action = user.isActive ? 'désactiver' : 'activer';
    this.openConfirmModal(
      `${action === 'activer' ? 'Activation' : 'Désactivation'} de compte`,
      `Voulez-vous vraiment ${action} l'utilisateur ${user.username} ?`,
      () => {
        this.http.put(`${environment.apiUrl}/users/${user.id}`,
          { accountStatus: newStatus },
          { headers: this.getHeaders() }
        ).subscribe({
          next: () => {
            this.showNotification('Statut modifié', `Utilisateur ${user.username} ${action} avec succès.`, 'success');
            this.loadUsers();
          },
          error: (err) => {
            this.showNotification('Erreur', err.error?.message || `Erreur lors de la ${action}`, 'error');
          }
        });
      }
    );
  }

  setTab(tab: 'active' | 'pending') { this.activeTab = tab; this.searchTerm = ''; this.applyFilter(); }

  applyFilter() {
    const source = this.activeTab === 'pending' ? this.pendingUsers : this.activeUsers;
    const term = this.searchTerm.toLowerCase();
    this.filteredUsers = term
      ? source.filter(u => u.username.toLowerCase().includes(term) || u.email.toLowerCase().includes(term))
      : [...source];
  }

  filterUsers() { this.applyFilter(); }

  updateRoleStats() {
    this.roleStats = {
      ADMIN: this.activeUsers.filter(u => u.role === 'ADMIN').length,
      GESTIONNAIRE: this.activeUsers.filter(u => u.role === 'GESTIONNAIRE').length,
      OPERATEUR: this.activeUsers.filter(u => u.role === 'OPERATEUR').length,
      LECTEUR: this.activeUsers.filter(u => u.role === 'LECTEUR').length
    };
  }

  closeModal() { this.showEditModal = false; this.selectedUser = null; }

  refresh() { this.loadUsers(); this.showNotification('Actualisation', 'Liste des utilisateurs actualisée !', 'success'); }

  getRoleLabel(role: string): string {
    return { ADMIN: 'Administrateur', GESTIONNAIRE: 'Gestionnaire', OPERATEUR: 'Opérateur', LECTEUR: 'Lecteur' }[role] || role;
  }

  getRoleClass(role: string): string {
    return { ADMIN: 'role-admin', GESTIONNAIRE: 'role-gestionnaire', OPERATEUR: 'role-operateur', LECTEUR: 'role-lecteur' }[role] || '';
  }
}