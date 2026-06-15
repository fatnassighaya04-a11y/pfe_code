import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';

interface AppSettings {
  organisation: {
    nom: string;
    email: string;
    telephone: string;
    pays: string;
  };
  extraction: {
    seuilAuto: number;
    langue: string;
    scoreMin: number;
  };
  notifications: {
    email: boolean;
    alerteErreur: boolean;
    apprentissageContinu: boolean;
    ameliorerModele: boolean;
  };
  securite: {
    deuxFacteurs: boolean;
    deuxFacteursType: 'TOTP' | 'SMS';
    dureeSession: number; // en heures
  };
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './settings.html',
  styleUrls: ['./settings.scss']
})
export class Settings implements OnInit {
  settingsForm!: FormGroup;
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  paysList = ['Tunisie', 'France', 'Maroc', 'Algérie', 'Sénégal', 'Côte d\'Ivoire'];
  languesList = ['Français', 'Anglais', 'Arabe', 'Espagnol'];

  constructor(private fb: FormBuilder) {}

  ngOnInit() {
    this.initForm();
    this.loadSettings();
  }

  private initForm() {
    this.settingsForm = this.fb.group({
      organisation: this.fb.group({
        nom: [''],
        email: [''],
        telephone: [''],
        pays: ['Tunisie']
      }),
      extraction: this.fb.group({
        seuilAuto: [85],
        langue: ['Français'],
        scoreMin: [50]
      }),
      notifications: this.fb.group({
        email: [false],
        alerteErreur: [false],
        apprentissageContinu: [false],
        ameliorerModele: [false]
      }),
      securite: this.fb.group({
        deuxFacteurs: [false],
        deuxFacteursType: ['TOTP'],
        dureeSession: [8]
      })
    });
  }

  loadSettings() {
    const stored = localStorage.getItem('app_settings');
    if (stored) {
      try {
        const settings = JSON.parse(stored);
        this.settingsForm.patchValue(settings);
      } catch(e) {
        console.error('Erreur chargement settings', e);
      }
    } else {
      
      this.saveSettings(); 
    }
  }

  saveSettings() {
    const settings = this.settingsForm.value;
    localStorage.setItem('app_settings', JSON.stringify(settings));
    this.showSuccess('Paramètres sauvegardés avec succès !');
  }

  resetToDefault() {
    this.settingsForm.reset({
      organisation: { nom: '', email: '', telephone: '', pays: 'Tunisie' },
      extraction: { seuilAuto: 85, langue: 'Français', scoreMin: 50 },
      notifications: { email: false, alerteErreur: false, apprentissageContinu: false, ameliorerModele: false },
      securite: { deuxFacteurs: false, deuxFacteursType: 'TOTP', dureeSession: 8 }
    });
    this.showSuccess('Paramètres réinitialisés aux valeurs par défaut');
  }

  showSuccess(msg: string) {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 3000);
  }

  showError(msg: string) {
    this.errorMessage = msg;
    setTimeout(() => this.errorMessage = '', 3000);
  }
}