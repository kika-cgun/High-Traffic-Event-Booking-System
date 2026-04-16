import { Component, input, output, signal, computed } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SeatGridComponent } from '../../../shared/components/seat-grid/seat-grid.component';
import { PitSelectorComponent } from '../../../shared/components/pit-selector/pit-selector.component';
import { SeatResponse } from '../../../shared/models/seat.model';

type ArenaView = 'map' | 'pit' | 'section';

@Component({
  selector: 'app-arena-map',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, SeatGridComponent, PitSelectorComponent],
  templateUrl: './arena-map.component.html',
  styleUrl: './arena-map.component.scss',
})
export class ArenaMapComponent {
  readonly seats = input.required<SeatResponse[]>();
  readonly maxSeats = input<number>(6);
  readonly seatsSelected = output<number[]>();

  protected readonly view = signal<ArenaView>('map');
  protected readonly selectedSection = signal<string | null>(null);

  protected readonly nonPitSections = computed(() => {
    const unique = new Set(
      this.seats()
        .filter(s => s.section !== 'PIT')
        .map(s => s.section)
    );
    return Array.from(unique);
  });

  protected readonly pitSeats = computed(() =>
    this.seats().filter(s => s.section === 'PIT' && !s.reserved)
  );

  protected readonly pitPrice = computed(() => {
    const first = this.seats().find(s => s.section === 'PIT');
    return first?.price ?? 200;
  });

  protected readonly sectionSeats = computed(() => {
    const sec = this.selectedSection();
    if (!sec) return [];
    return this.seats().filter(s => s.section === sec);
  });

  protected readonly sectionAvailability = computed(() => {
    const map: Record<string, { total: number; available: number }> = {};
    for (const seat of this.seats()) {
      if (seat.section === 'PIT') continue;
      if (!map[seat.section]) map[seat.section] = { total: 0, available: 0 };
      map[seat.section].total++;
      if (!seat.reserved) map[seat.section].available++;
    }
    return map;
  });

  protected selectPit(): void {
    this.view.set('pit');
  }

  protected selectSection(section: string): void {
    this.selectedSection.set(section);
    this.view.set('section');
  }

  protected back(): void {
    this.view.set('map');
    this.selectedSection.set(null);
  }

  protected onPitCount(count: number): void {
    const pitIds = this.pitSeats().slice(0, count).map(s => s.id);
    this.seatsSelected.emit(pitIds);
  }

  protected onSectionSeatsSelected(ids: number[]): void {
    this.seatsSelected.emit(ids);
  }
}
