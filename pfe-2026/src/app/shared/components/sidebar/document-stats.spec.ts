import { TestBed } from '@angular/core/testing';

import { DocumentStats } from './document-stats';

describe('DocumentStats', () => {
  let service: DocumentStats;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DocumentStats);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
