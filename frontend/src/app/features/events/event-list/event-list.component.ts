import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { DatePipe } from '@angular/common';
import { EventResponse, PageResponse } from '../../../shared/models/event.model';
import { EventService } from '../../../core/services/event.service';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatPaginatorModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatIconModule,
    DatePipe,
  ],
  templateUrl: './event-list.component.html',
  styleUrl: './event-list.component.scss',
})
export class EventListComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);

  protected readonly page = signal<PageResponse<EventResponse> | null>(null);
  protected readonly loading = signal(true);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = 20;

  ngOnInit(): void {
    this.load(0);
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.load(event.pageIndex);
  }

  protected openEvent(id: number): void {
    this.router.navigate(['/events', id]);
  }

  private load(page: number): void {
    this.loading.set(true);
    this.eventService.getEvents(page, this.pageSize).subscribe({
      next: data => {
        this.page.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
