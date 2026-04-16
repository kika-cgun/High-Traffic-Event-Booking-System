export interface SeatResponse {
  id: number;
  seatNumber: number;
  rowNumber: number;
  section: string;
  category: string;
  price: number;
  reserved: boolean;
}

export interface SeatInfo {
  id: number;
  seatNumber: number;
  rowNumber: number;
  section: string;
  category: string;
  price: number;
}

export interface SeatStatusUpdate {
  eventId: number;
  seatId: number;
  ticketId: number;
  action: 'RESERVED' | 'CONFIRMED' | 'CANCELLED';
  timestamp: string;
}
