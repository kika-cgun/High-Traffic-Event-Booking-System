import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { TicketResponse } from '../../../shared/models/ticket.model';
import { TicketService } from '../../../core/services/ticket.service';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    DatePipe,
    CurrencyPipe,
  ],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.scss',
})
export class TicketDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly ticket = signal<TicketResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly cancelling = signal(false);

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.ticketService.getTicket(id).subscribe({
      next: t => {
        this.ticket.set(t);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Nie znaleziono biletu.', 'OK', { duration: 3000 });
        this.router.navigate(['/tickets']);
      },
    });
  }

  protected downloadPdf(): void {
    const id = this.ticket()?.id;
    if (!id) return;
    window.open(`/api/tickets/${id}/pdf`, '_blank');
  }

  protected cancel(): void {
    const id = this.ticket()?.id;
    if (!id) return;
    this.cancelling.set(true);
    this.ticketService.cancelTicket(id).subscribe({
      next: () => {
        this.ticket.update(t => t ? { ...t, status: 'CANCELLED' } : t);
        this.cancelling.set(false);
        this.snackBar.open('Rezerwacja anulowana.', 'OK', { duration: 3000 });
      },
      error: () => {
        this.cancelling.set(false);
        this.snackBar.open('Blad anulowania. Sprobuj ponownie.', 'OK', { duration: 3000 });
      },
    });
  }

  protected canCancel(): boolean {
    return this.ticket()?.status === 'RESERVED';
  }

  protected statusColor(status: string): 'primary' | 'accent' | 'warn' {
    switch (status) {
      case 'RESERVED': return 'primary';
      case 'CONFIRMED': return 'accent';
      case 'CANCELLED': return 'warn';
      default: return 'primary';
    }
  }
}
