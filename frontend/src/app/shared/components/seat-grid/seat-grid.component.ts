import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SeatResponse } from '../../models/seat.model';

interface SeatRow {
  rowNumber: number;
  seats: SeatResponse[];
}

interface SeatSection {
  section: string;
  rows: SeatRow[];
}

@Component({
  selector: 'app-seat-grid',
  standalone: true,
  imports: [MatButtonModule, MatTooltipModule],
  templateUrl: './seat-grid.component.html',
  styleUrl: './seat-grid.component.scss',
})
export class SeatGridComponent {
  readonly seats = input.required<SeatResponse[]>();
  readonly seatSelected = output<SeatResponse>();

  protected readonly sections = computed<SeatSection[]>(() => {
    const map = new Map<string, Map<number, SeatResponse[]>>();
    for (const seat of this.seats()) {
      if (!map.has(seat.section)) map.set(seat.section, new Map());
      const rows = map.get(seat.section)!;
      if (!rows.has(seat.rowNumber)) rows.set(seat.rowNumber, []);
      rows.get(seat.rowNumber)!.push(seat);
    }

    return Array.from(map.entries()).map(([section, rowMap]) => ({
      section,
      rows: Array.from(rowMap.entries())
        .sort(([a], [b]) => a - b)
        .map(([rowNumber, seats]) => ({
          rowNumber,
          seats: [...seats].sort((a, b) => a.seatNumber - b.seatNumber),
        })),
    }));
  });

  protected select(seat: SeatResponse): void {
    if (!seat.reserved) {
      this.seatSelected.emit(seat);
    }
  }
}
