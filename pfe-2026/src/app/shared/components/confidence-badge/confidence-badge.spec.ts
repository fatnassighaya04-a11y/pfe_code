import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfidenceBadge } from './confidence-badge';

describe('ConfidenceBadge', () => {
  let component: ConfidenceBadge;
  let fixture: ComponentFixture<ConfidenceBadge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfidenceBadge]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfidenceBadge);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
