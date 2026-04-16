import { Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DatePipe } from '@angular/common';
import { TicketResponse, TicketStatus } from '../../models/ticket.model';

@Component({
  selector: 'app-ticket-card',
  standalone: true,
  imports: [MatCardModule, MatChipsModule, MatButtonModule, MatIconModule, DatePipe],
  templateUrl: './ticket-card.component.html',
  styleUrl: './ticket-card.component.scss',
})
export class TicketCardComponent {
  readonly ticket = input.required<TicketResponse>();
  readonly cancelled = output<number>();

  protected statusColor(status: TicketStatus): string {
    switch (status) {
      case 'RESERVED': return 'warn';
      case 'CONFIRMED': return 'primary';
      default: return '';
    }
  }

  protected statusLabel(status: TicketStatus): string {
    switch (status) {
      case 'RESERVED': return 'Reserved';
      case 'CONFIRMED': return 'Confirmed';
      case 'PAID': return 'Paid';
      case 'CANCELLED': return 'Cancelled';
    }
  }
}
