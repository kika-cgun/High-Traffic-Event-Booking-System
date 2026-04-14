import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventResponse, PageResponse } from '../../shared/models/event.model';
import { SeatResponse } from '../../shared/models/seat.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);

  getEvents(page: number, size: number): Observable<PageResponse<EventResponse>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'date');
    return this.http.get<PageResponse<EventResponse>>('/api/events', { params });
  }

  getSeats(eventId: number): Observable<SeatResponse[]> {
    return this.http.get<SeatResponse[]>(`/api/events/${eventId}/seats`);
  }

  getEvent(eventId: number): Observable<EventResponse> {
    return this.http.get<EventResponse>(`/api/events/${eventId}`);
  }
}
