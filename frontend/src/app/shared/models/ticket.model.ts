import { SeatInfo } from './seat.model';

export type TicketStatus = 'RESERVED' | 'CONFIRMED' | 'PAID' | 'CANCELLED';
export type VenueType = 'CINEMA' | 'STADIUM' | 'CONCERT_ARENA';

export interface TicketResponse {
  id: number;
  status: TicketStatus;
  createdAt: string;
  eventName: string;
  eventDate: string;
  venueType: VenueType;
  seats: SeatInfo[];
  totalPrice: number;
}
