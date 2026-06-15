import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const adminGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) return true;

  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.getCurrentUser();
  if (user?.role === 'ADMIN') return true;
  router.navigate(['/dashboard']);
  return false;
};

export const gestionnaireGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) return true;

  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.getCurrentUser();
  const role = user?.role ?? '';
  if (['ADMIN', 'GESTIONNAIRE'].includes(role)) return true;
  router.navigate(['/dashboard']);
  return false;
};

export const operateurGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) return true;

  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.getCurrentUser();
  const role = user?.role ?? '';
  if (['ADMIN', 'GESTIONNAIRE', 'OPERATEUR'].includes(role)) return true;
  router.navigate(['/dashboard']);
  return false;
};