import { Component, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { timeout, catchError, finalize } from 'rxjs/operators';
import { throwError, Subscription } from 'rxjs';
import Swal from 'sweetalert2';

interface EmailPreview {
  subject: string;
  intro: string;
  body: string;
  closing: string;
}

function passwordMatch(group: AbstractControl) {
  return group.get('password')?.value === group.get('confirmPassword')?.value
    ? null : { passwordMismatch: true };
}
function resetPasswordMatch(group: AbstractControl) {
  return group.get('newPassword')?.value === group.get('confirmPassword')?.value
    ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class Login implements OnDestroy {
  loginForm: FormGroup;
  loading = false;
  error = '';
  showPassword = false;

  showRegisterModal = false;
  registerForm: FormGroup;
  registerLoading = false;
  registerError = '';
  registerSuccess = false;
  showRegisterPassword = false;
  showConfirmPassword = false;

  showForgotModal = false;
  forgotStep = 1;
  forgotLoading = false;
  forgotError = '';
  forgotSuccess = false;
  forgotEmail = '';
  forgotCode = '';
  showNewPassword = false;
  showConfirmNewPassword = false;
  resendCountdown = 0;
  private resendTimer: any;
  private httpSub: Subscription | null = null;
  private autoCloseTimer: any;
  approvalEmailPreview: EmailPreview = {
    subject: 'Chargement...',
    intro: '',
    body: '',
    closing: ''
  };
  rejectionEmailPreview: EmailPreview = {
    subject: 'Chargement...',
    intro: '',
    body: '',
    closing: ''
  };
  forgotEmailForm: FormGroup;
  resetPasswordForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(4)]]
    });

    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required],
      role: ['OPERATEUR', Validators.required]
    }, { validators: passwordMatch });

    this.forgotEmailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.resetPasswordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: resetPasswordMatch });
  }

  ngOnDestroy() {
    if (this.resendTimer) clearInterval(this.resendTimer);
    if (this.autoCloseTimer) clearTimeout(this.autoCloseTimer);
    if (this.httpSub) this.httpSub.unsubscribe();
  }

  ngOnInit() {
    this.loadEmailPreviews();
  }

  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }
  get rUsername() { return this.registerForm.get('username'); }
  get rEmail() { return this.registerForm.get('email'); }
  get rPassword() { return this.registerForm.get('password'); }
  get rConfirm() { return this.registerForm.get('confirmPassword'); }
  get rRole() { return this.registerForm.get('role'); }
  get fEmail() { return this.forgotEmailForm.get('email'); }
  get fNewPassword() { return this.resetPasswordForm.get('newPassword'); }
  get fConfirm() { return this.resetPasswordForm.get('confirmPassword'); }

  onSubmit() {
    if (this.loginForm.invalid) return;
    this.loading = true;
    this.error = '';

    const credentials = {
      email: String(this.loginForm.value.email ?? '').trim(),
      password: String(this.loginForm.value.password ?? '')
    };

    Swal.fire({
      title: 'Connexion...',
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.loading = false;
        Swal.close();
        Swal.fire('Succès', 'Connexion réussie', 'success');
        this.router.navigate([response.role === 'ADMIN' ? '/users' : '/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        Swal.close();
        this.error = err.error?.message || 'Email ou mot de passe incorrect.';
        Swal.fire('Erreur', this.error, 'error');
      }
    });
  }

  openRegisterModal() {
    this.showRegisterModal = true;
    this.registerSuccess = false;
    this.registerError = '';
    this.registerForm.reset({ role: 'OPERATEUR' });
    this.cdr.detectChanges();
  }

  closeRegisterModal() {
    this.showRegisterModal = false;
    this.registerSuccess = false;
    this.cdr.detectChanges();
  }

  // ✅ DEMANDE D'ACCÈS CORRIGÉE
  onRegister() {
    if (this.registerForm.invalid) return;
    this.registerLoading = true;
    this.registerError = '';

    Swal.fire({
      title: 'Envoi...',
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });

    // Utilisation du bon endpoint : /register (existant dans votre backend)
    this.http.post(`${environment.apiUrl}/auth/register`, {
      username: this.registerForm.value.username,
      email: this.registerForm.value.email,
      password: this.registerForm.value.password,
      role: this.registerForm.value.role
    }).subscribe({
      next: () => {
        this.registerLoading = false;
        Swal.close();
        this.registerSuccess = true;
        Swal.fire('Succès', 'Demande envoyée', 'success');
        setTimeout(() => {
          this.closeRegisterModal();
          this.cdr.detectChanges();
        }, 2000);
      },
      error: (err) => {
        this.registerLoading = false;
        Swal.close();
        
        let errorMsg = 'Erreur lors de l\'inscription';
        if (err.error?.message) {
          errorMsg = err.error.message;
        } else if (typeof err.error === 'string') {
          errorMsg = err.error;
        } else if (err.message) {
          errorMsg = err.message;
        }
        this.registerError = errorMsg;
        Swal.fire('Erreur', this.registerError, 'error');
        this.cdr.detectChanges();
      }
    });
  }

  openForgotModal() {
    if (this.autoCloseTimer) clearTimeout(this.autoCloseTimer);
    this.showForgotModal = true;
    this.forgotStep = 1;
    this.forgotError = '';
    this.forgotSuccess = false;
    this.forgotEmail = '';
    this.forgotCode = '';
    this.resendCountdown = 0;
    this.forgotEmailForm.reset();
    this.resetPasswordForm.reset();
    if (this.resendTimer) clearInterval(this.resendTimer);
    if (this.httpSub) this.httpSub.unsubscribe();
    this.cdr.detectChanges();
  }

  closeForgotModal() {
    this.showForgotModal = false;
    if (this.resendTimer) clearInterval(this.resendTimer);
    if (this.autoCloseTimer) clearTimeout(this.autoCloseTimer);
    this.cdr.detectChanges();
  }

  sendResetCode() {
    if (this.forgotEmailForm.invalid) return;
    this.forgotLoading = true;
    this.forgotError = '';

    Swal.fire({
      title: 'Envoi du code...',
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });

    this.http.post(`${environment.apiUrl}/auth/forgot-password`,
      { email: this.forgotEmailForm.value.email })
      .subscribe({
        next: () => {
          this.forgotLoading = false;
          Swal.close();
          this.forgotStep = 2;
          this.forgotEmail = this.forgotEmailForm.value.email;
          this.startResendTimer();
          Swal.fire('Succès', 'Code envoyé', 'success');
          this.cdr.detectChanges();
        },
        error: () => {
          this.forgotLoading = false;
          Swal.close();
          this.forgotError = 'Email introuvable';
          Swal.fire('Erreur', this.forgotError, 'error');
          this.cdr.detectChanges();
        }
      });
  }

  startResendTimer() {
    this.resendCountdown = 30;
    if (this.resendTimer) clearInterval(this.resendTimer);
    this.resendTimer = setInterval(() => {
      this.resendCountdown--;
      if (this.resendCountdown <= 0) clearInterval(this.resendTimer);
      this.cdr.detectChanges();
    }, 1000);
  }

  resendCode() {
    if (this.resendCountdown > 0) return;
    this.forgotError = '';
    if (this.httpSub) this.httpSub.unsubscribe();
    this.httpSub = this.http.post(`${environment.apiUrl}/auth/forgot-password`,
      { email: this.forgotEmail }, { responseType: 'text' })
      .pipe(
        timeout(15000),
        catchError((err) => {
          this.forgotError = err.error?.message || 'Erreur renvoi code.';
          this.cdr.detectChanges();
          return throwError(() => err);
        })
      )
      .subscribe({
        next: () => {
          this.startResendTimer();
          this.cdr.detectChanges();
        },
        error: () => {}
      });
  }

  verifyCode() {
    const cleanCode = this.forgotCode.trim();
    if (!/^\d{4,6}$/.test(cleanCode)) {
      this.forgotError = 'Le code doit contenir entre 4 et 6 chiffres.';
      this.cdr.detectChanges();
      return;
    }
    this.forgotCode = cleanCode;
    this.forgotError = '';
    this.forgotStep = 3;
    this.cdr.detectChanges();
  }

  resetPassword() {
    if (this.resetPasswordForm.invalid) {
      if (this.resetPasswordForm.get('newPassword')?.hasError('minlength')) {
        this.forgotError = 'Le mot de passe doit contenir au moins 6 caractères.';
      } else if (this.resetPasswordForm.hasError('passwordMismatch')) {
        this.forgotError = 'Les mots de passe ne correspondent pas.';
      } else {
        this.forgotError = 'Formulaire invalide.';
      }
      this.cdr.detectChanges();
      return;
    }

    this.forgotLoading = true;
    this.forgotError = '';

    const resetUrl = `${environment.apiUrl}/auth/reset-password`;
    if (this.httpSub) this.httpSub.unsubscribe();
    this.httpSub = this.http.post(resetUrl, {
      email: this.forgotEmail,
      code: this.forgotCode,
      newPassword: this.resetPasswordForm.value.newPassword
    }, { responseType: 'text' })
      .pipe(
        timeout(15000),
        catchError((err: HttpErrorResponse) => {
          let errorMsg = 'Erreur lors de la réinitialisation.';
          if (err.error && typeof err.error === 'string') errorMsg = err.error;
          else if (err.error?.message) errorMsg = err.error.message;
          else if (err.status === 400) errorMsg = 'Code invalide, expiré ou mot de passe non conforme.';
          this.forgotError = errorMsg;
          this.cdr.detectChanges();
          return throwError(() => err);
        }),
        finalize(() => {
          this.forgotLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.forgotSuccess = true;
          Swal.fire('Succès', 'Mot de passe réinitialisé', 'success');
          if (this.autoCloseTimer) clearTimeout(this.autoCloseTimer);
          this.autoCloseTimer = setTimeout(() => {
            this.closeForgotModal();
            this.loginForm.patchValue({ email: this.forgotEmail });
            this.cdr.detectChanges();
          }, 2000);
        },
        error: () => {
          Swal.fire('Erreur', this.forgotError, 'error');
          this.cdr.detectChanges();
        }
      });
  }

  backStep() {
    if (this.forgotStep > 1) {
      this.forgotStep--;
      this.forgotError = '';
      this.cdr.detectChanges();
    }
  }

  private loadEmailPreviews() {
    this.http.get<any>(`${environment.apiUrl}/emails/previews`).subscribe({
      next: (data) => {
        this.approvalEmailPreview = data.approval || this.approvalEmailPreview;
        this.rejectionEmailPreview = data.rejection || this.rejectionEmailPreview;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
      }
    });
  }
}