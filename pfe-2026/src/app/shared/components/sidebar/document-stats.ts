import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DocumentStatsService {
  private totalDocs = new BehaviorSubject<number>(0);
  private pendingDocs = new BehaviorSubject<number>(0);

  totalDocuments$ = this.totalDocs.asObservable();
  pendingValidation$ = this.pendingDocs.asObservable();

  setTotalDocuments(n: number) { this.totalDocs.next(n); }
  setPendingValidation(n: number) { this.pendingDocs.next(n); }
  decrementPending() { this.pendingDocs.next(Math.max(0, this.pendingDocs.value - 1)); }

  get totalValue() { return this.totalDocs.value; }
  get pendingValue() { return this.pendingDocs.value; }
}
