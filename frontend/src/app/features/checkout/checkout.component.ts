import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { TicketResponse } from '../../shared/models/ticket.model';
import { TicketService } from '../../core/services/ticket.service';

type CheckoutState = 'loading' | 'ready' | 'paying' | 'success' | 'error';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatDividerModule,
  ],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly ticketService = inject(TicketService);

  protected readonly state = signal<CheckoutState>('loading');
  protected readonly ticket = signal<TicketResponse | null>(null);
  protected ticketId!: number;

  ngOnInit(): void {
    this.ticketId = +this.route.snapshot.paramMap.get('ticketId')!;
    this.ticketService.getMyTickets().subscribe({
      next: tickets => {
        const found = tickets.find(t => t.id === this.ticketId) ?? null;
        this.ticket.set(found);
        this.state.set(found ? 'ready' : 'error');
      },
      error: () => this.state.set('error'),
    });
  }

  protected pay(): void {
    this.state.set('paying');
    setTimeout(() => {
      this.ticketService.confirmTicket(this.ticketId).subscribe({
        next: () => this.state.set('success'),
        error: () => this.state.set('error'),
      });
    }, 2000);
  }
}
