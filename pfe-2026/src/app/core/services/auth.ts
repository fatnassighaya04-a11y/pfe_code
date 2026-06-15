import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  role: string;
}

export interface User {
  email: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private tokenKey = 'accessToken';
  private userKey = 'user';
  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  // ========== AUTHENTIFICATION ==========
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(tap(response => this.storeTokens(response)));
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, userData)
      .pipe(tap(response => this.storeTokens(response)));
  }

  // ========== MOT DE PASSE OUBLIÉ ==========
  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(email: string, code: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, { email, code, newPassword });
  }

  // ========== DEMANDE D'ACCÈS ==========
  requestAccess(username: string, email: string, role: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/request-access`, { username, email, role });
  }

  // ========== GESTION DES TOKENS ==========
  private storeTokens(response: AuthResponse): void {
    if (!this.isBrowser) return;
    try {
      localStorage.setItem(this.tokenKey, response.token);
      localStorage.setItem(this.userKey, JSON.stringify({
        email: response.email,
        role: response.role
      }));
    } catch (e) {
      console.warn('localStorage bloqué, utilisation sessionStorage');
    }
    sessionStorage.setItem(this.tokenKey, response.token);
    sessionStorage.setItem(this.userKey, JSON.stringify({
      email: response.email,
      role: response.role
    }));
  }

  private clearTokens(): void {
    if (!this.isBrowser) return;
    try { localStorage.removeItem(this.tokenKey); } catch (e) {}
    try { localStorage.removeItem(this.userKey); } catch (e) {}
    sessionStorage.removeItem(this.tokenKey);
    sessionStorage.removeItem(this.userKey);
  }

  getToken(): string | null {
    if (!this.isBrowser) return null;
    try {
      const localToken = localStorage.getItem(this.tokenKey);
      if (localToken) return localToken;
    } catch (e) {}
    return sessionStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    if (!this.isBrowser) return false;
    return !!this.getToken();
  }

  getUserRole(): string | null {
    if (!this.isBrowser) return null;
    let userStr: string | null = null;
    try { userStr = localStorage.getItem(this.userKey); } catch (e) {}
    if (!userStr) userStr = sessionStorage.getItem(this.userKey);
    if (userStr) {
      try { return JSON.parse(userStr).role; } catch (e) { return null; }
    }
    return null;
  }

  getCurrentUser(): User | null {
    if (!this.isBrowser) return null;
    let userStr: string | null = null;
    try { userStr = localStorage.getItem(this.userKey); } catch (e) {}
    if (!userStr) userStr = sessionStorage.getItem(this.userKey);
    if (userStr) {
      try { return JSON.parse(userStr); } catch (e) { return null; }
    }
    return null;
  }

  logout(): void {
    this.clearTokens();
  }
}