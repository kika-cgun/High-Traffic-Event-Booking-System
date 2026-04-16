import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SeatResponse } from '../../../shared/models/seat.model';
import { EventResponse } from '../../../shared/models/event.model';
import { EventService } from '../../../core/services/event.service';
import { TicketService } from '../../../core/services/ticket.service';
import { StompService } from '../../../core/services/stomp.service';
import { AuthService } from '../../../core/services/auth.service';
import { SeatGridComponent } from '../../../shared/components/seat-grid/seat-grid.component';
import { StadiumMapComponent } from '../stadium-map/stadium-map.component';
import { ArenaMapComponent } from '../arena-map/arena-map.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [
    SeatGridComponent,
    StadiumMapComponent,
    ArenaMapComponent,
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
  protected readonly event = signal<EventResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly reserving = signal(false);

  protected eventId!: number;
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.eventId = +this.route.snapshot.paramMap.get('id')!;
    this.loadData();
    this.subscribeToWebSocket();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onSeatsSelected(seatIds: number[]): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: `/events/${this.eventId}` },
      });
      return;
    }

    this.reserving.set(true);
    this.ticketService.reserveSeats(seatIds).subscribe({
      next: ticketId => {
        this.reserving.set(false);
        this.router.navigate(['/checkout', ticketId]);
      },
      error: err => {
        this.reserving.set(false);
        const msg =
          err.status === 409 ? 'One of the seats is already taken. Choose another.' :
          err.status === 423 ? 'Seat is currently reserved. Please try again.' :
          'Booking failed. Please try again.';
        this.snackBar.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  private loadData(): void {
    this.loading.set(true);
    this.eventService.getSeats(this.eventId).subscribe({
      next: seats => {
        this.seats.set(seats);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.eventService.getEvent(this.eventId).subscribe({
      next: ev => this.event.set(ev),
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
