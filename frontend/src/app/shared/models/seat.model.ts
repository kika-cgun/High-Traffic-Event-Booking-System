export interface SeatResponse {
  id: number;
  seatNumber: number;
  rowNumber: number;
  section: string;
  reserved: boolean;
}

export interface SeatStatusUpdate {
  eventId: number;
  seatId: number;
  ticketId: number;
  action: 'RESERVED' | 'CONFIRMED' | 'CANCELLED';
  timestamp: string;
}
