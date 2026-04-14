import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { TicketResponse } from '../../shared/models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);

  getMyTickets(): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>('/api/tickets/my');
  }

  getTicket(ticketId: number): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`/api/tickets/${ticketId}`);
  }

  reserveSeats(seatIds: number[]): Observable<number> {
    return this.http
      .post('/api/reservations', { seatIds }, { responseType: 'text' })
      .pipe(
        map(response => {
          const match = response.match(/Ticket ID: (\d+)/);
          if (!match) throw new Error(`Unexpected reservation response: ${response}`);
          return parseInt(match[1], 10);
        })
      );
  }

  confirmTicket(ticketId: number): Observable<void> {
    return this.http.post<void>(`/api/tickets/${ticketId}/confirm`, {});
  }

  cancelTicket(ticketId: number): Observable<void> {
    return this.http.delete<void>(`/api/tickets/${ticketId}`);
  }
}
