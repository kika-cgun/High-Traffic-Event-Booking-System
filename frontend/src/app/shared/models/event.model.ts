export interface EventResponse {
  id: number;
  name: string;
  date: string; // ISO date string (LocalDateTime serialized by Spring)
  availableSeats: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
