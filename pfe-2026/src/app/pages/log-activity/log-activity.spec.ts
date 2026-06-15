import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogActivity } from './log-activity';

describe('LogActivity', () => {
  let component: LogActivity;
  let fixture: ComponentFixture<LogActivity>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogActivity]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LogActivity);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
