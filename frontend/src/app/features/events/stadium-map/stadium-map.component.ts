import { Component, input, output, signal, computed } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SeatGridComponent } from '../../../shared/components/seat-grid/seat-grid.component';
import { SeatResponse } from '../../../shared/models/seat.model';

@Component({
  selector: 'app-stadium-map',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, SeatGridComponent],
  templateUrl: './stadium-map.component.html',
  styleUrl: './stadium-map.component.scss',
})
export class StadiumMapComponent {
  readonly seats = input.required<SeatResponse[]>();
  readonly maxSeats = input<number>(4);
  readonly seatsSelected = output<number[]>();

  protected readonly selectedSection = signal<string | null>(null);

  protected readonly sections = computed(() => {
    const unique = new Set(this.seats().map(s => s.section));
    return Array.from(unique);
  });

  protected readonly sectionSeats = computed(() => {
    const sec = this.selectedSection();
    if (!sec) return [];
    return this.seats().filter(s => s.section === sec);
  });

  protected readonly sectionAvailability = computed(() => {
    const map: Record<string, { total: number; available: number }> = {};
    for (const seat of this.seats()) {
      if (!map[seat.section]) map[seat.section] = { total: 0, available: 0 };
      map[seat.section].total++;
      if (!seat.reserved) map[seat.section].available++;
    }
    return map;
  });

  protected selectSection(section: string): void {
    this.selectedSection.set(section);
  }

  protected back(): void {
    this.selectedSection.set(null);
  }

  protected onSeatsSelected(ids: number[]): void {
    this.seatsSelected.emit(ids);
  }
}
