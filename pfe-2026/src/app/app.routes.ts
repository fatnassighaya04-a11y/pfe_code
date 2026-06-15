import { Routes } from '@angular/router';
import { AuthLayout } from './shared/layouts/auth-layout/auth-layout';
import { MainLayout } from './shared/layouts/main-layout/main-layout';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { Upload } from './pages/upload/upload';
import { Documents } from './pages/documents/documents';
import { UsersComponent } from './pages/users/users';
import { Settings } from './pages/settings/settings';
import { Validation } from './pages/validation/validation';
import { Analytics } from './pages/analytics/analytics';
import { ValidationList } from './pages/validation-list/validation-list';
import { ApiIntegrations } from './pages/api-integrations/api-integrations';
import { authGuard } from './core/guards/auth-guard';
import { adminGuard, gestionnaireGuard, operateurGuard } from './core/guards/role-guard';
import { LogActivity } from './pages/log-activity/log-activity';
export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: '',
    component: AuthLayout,
    children: [
      { path: 'login', component: Login }
    ]
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'documents', component: Documents },
      { path: 'upload', component: Upload, canActivate: [operateurGuard] },
      { path: 'validation', component: ValidationList, canActivate: [operateurGuard] },
      { path: 'validation/:id', component: Validation, canActivate: [operateurGuard] },
      { path: 'analytics', component: Analytics, canActivate: [gestionnaireGuard] },
      { path: 'settings', component: Settings, canActivate: [gestionnaireGuard] },
      { path: 'users', component: UsersComponent, canActivate: [adminGuard] },
      { path: 'api-integrations', component: ApiIntegrations, canActivate: [adminGuard] },
      { path: 'log-activity', component: LogActivity, canActivate: [adminGuard] }
    ]
  },
  { path: '**', redirectTo: 'login' }
];