import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TicketService } from './ticket.service';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TicketService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/tickets/my', () => {
    service.getMyTickets().subscribe();
    const req = httpMock.expectOne('/api/tickets/my');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should POST /api/reservations and parse ticket ID from plain-text response', () => {
    let ticketId: number | undefined;
    service.reserveSeat(7).subscribe(id => (ticketId = id));
    const req = httpMock.expectOne('/api/reservations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ seatId: 7 });
    req.flush('Reservation successful. Ticket ID: 42', {
      headers: { 'Content-Type': 'text/plain' },
    });
    expect(ticketId).toBe(42);
  });

  it('should POST /api/tickets/{id}/confirm', () => {
    service.confirmTicket(5).subscribe();
    const req = httpMock.expectOne('/api/tickets/5/confirm');
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should DELETE /api/tickets/{id}', () => {
    service.cancelTicket(3).subscribe();
    const req = httpMock.expectOne('/api/tickets/3');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
