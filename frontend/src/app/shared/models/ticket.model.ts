export type TicketStatus = 'RESERVED' | 'CONFIRMED' | 'PAID' | 'CANCELLED';

export interface TicketResponse {
  id: number;
  status: TicketStatus;
  createdAt: string; // ISO date string
  eventName: string;
  seatNumber: number;
  rowNumber: number;
  section: string;
}
