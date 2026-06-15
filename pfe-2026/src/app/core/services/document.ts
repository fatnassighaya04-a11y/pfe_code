import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private apiUrl = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient) {}

  getDocuments(page: number = 0, size: number = 10, search?: string): Observable<any> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    return this.http.get(this.apiUrl, { params });
  }

  getDocument(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}`);
  }

  uploadDocument(formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/upload`, formData);
  }

  validateDocument(id: string, validation: { status: string; comment?: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/validate`, validation);
  }
}
