import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { EventService } from './event.service';

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EventService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/events with page, size, sort params', () => {
    service.getEvents(0, 20).subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/events');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.get('sort')).toBe('date');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });
  });

  it('should GET /api/events/{id}/seats', () => {
    service.getSeats(42).subscribe();
    const req = httpMock.expectOne('/api/events/42/seats');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
