import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  success(message: string, title?: string) {
    console.log('✅', message);
    alert(message);
  }

  error(message: string, title?: string) {
    console.log('❌', message);
    alert(message);
  }

  warning(message: string, title?: string) {
    console.log('⚠️', message);
    alert(message);
  }

  info(message: string, title?: string) {
    console.log('ℹ️', message);
  }
}