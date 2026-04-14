import { VenueType } from './ticket.model';

export interface EventResponse {
  id: number;
  name: string;
  date: string;
  venueType: VenueType;
  maxSeatsPerBooking: number;
  availableSeats: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
