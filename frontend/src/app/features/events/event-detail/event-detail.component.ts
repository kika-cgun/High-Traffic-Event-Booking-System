import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SeatResponse } from '../../../shared/models/seat.model';
import { EventService } from '../../../core/services/event.service';
import { TicketService } from '../../../core/services/ticket.service';
import { StompService } from '../../../core/services/stomp.service';
import { AuthService } from '../../../core/services/auth.service';
import { SeatGridComponent } from '../../../shared/components/seat-grid/seat-grid.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [
    SeatGridComponent,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.scss',
})
export class EventDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  protected readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly ticketService = inject(TicketService);
  private readonly stompService = inject(StompService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly seats = signal<SeatResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly reserving = signal(false);

  protected eventId!: number;
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.eventId = +this.route.snapshot.paramMap.get('id')!;
    this.loadSeats();
    this.subscribeToWebSocket();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onSeatSelected(seat: SeatResponse): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: `/events/${this.eventId}` },
      });
      return;
    }

    this.reserving.set(true);
    this.ticketService.reserveSeats([seat.id]).subscribe({
      next: (ticketId: number) => {
        this.reserving.set(false);
        this.router.navigate(['/checkout', ticketId]);
      },
      error: (err: { status: number }) => {
        this.reserving.set(false);
        const msg =
          err.status === 409
            ? 'That seat was just taken. Please choose another.'
            : 'Reservation failed. Try again.';
        this.snackBar.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  private loadSeats(): void {
    this.loading.set(true);
    this.eventService.getSeats(this.eventId).subscribe({
      next: seats => {
        this.seats.set(seats);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private subscribeToWebSocket(): void {
    this.stompService
      .watchEvent(this.eventId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(update => {
        this.seats.update(current =>
          current.map(s =>
            s.id === update.seatId
              ? { ...s, reserved: update.action !== 'CANCELLED' }
              : s
          )
        );
      });
  }
}
