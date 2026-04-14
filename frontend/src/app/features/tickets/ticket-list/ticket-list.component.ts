import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { TicketResponse } from '../../../shared/models/ticket.model';
import { TicketService } from '../../../core/services/ticket.service';
import { TicketCardComponent } from '../../../shared/components/ticket-card/ticket-card.component';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [RouterLink, TicketCardComponent, MatProgressSpinnerModule, MatSnackBarModule, MatIconModule],
  templateUrl: './ticket-list.component.html',
  styleUrl: './ticket-list.component.scss',
})
export class TicketListComponent implements OnInit {
  private readonly ticketService = inject(TicketService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly tickets = signal<TicketResponse[]>([]);
  protected readonly loading = signal(true);

  ngOnInit(): void {
    this.loadTickets();
  }

  protected onCancel(ticketId: number): void {
    this.ticketService.cancelTicket(ticketId).subscribe({
      next: () => {
        this.tickets.update(list =>
          list.map(t => (t.id === ticketId ? { ...t, status: 'CANCELLED' as const } : t))
        );
        this.snackBar.open('Reservation cancelled.', 'OK', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Failed to cancel. Try again.', 'OK', { duration: 3000 });
      },
    });
  }

  private loadTickets(): void {
    this.ticketService.getMyTickets().subscribe({
      next: tickets => {
        this.tickets.set(tickets);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
