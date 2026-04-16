import { Component, computed, input, output, signal, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe } from '@angular/common';
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
  imports: [MatButtonModule, MatTooltipModule, MatSnackBarModule, CurrencyPipe],
  templateUrl: './seat-grid.component.html',
  styleUrl: './seat-grid.component.scss',
})
export class SeatGridComponent {
  readonly seats = input.required<SeatResponse[]>();
  readonly maxSeats = input<number>(10);
  readonly seatsSelected = output<number[]>();

  private readonly snackBar = inject(MatSnackBar);

  protected readonly selectedIds = signal<Set<number>>(new Set());

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

  protected readonly selectedCount = computed(() => this.selectedIds().size);

  protected readonly totalPrice = computed(() => {
    const ids = this.selectedIds();
    return this.seats()
      .filter(s => ids.has(s.id) && !s.reserved)
      .reduce((sum, s) => sum + (s.price ?? 0), 0);
  });

  protected isSelected(seat: SeatResponse): boolean {
    return this.selectedIds().has(seat.id);
  }

  protected toggle(seat: SeatResponse): void {
    if (seat.reserved) return;

    const current = new Set(this.selectedIds());
    if (current.has(seat.id)) {
      current.delete(seat.id);
    } else {
      if (current.size >= this.maxSeats()) {
        this.snackBar.open(`Osiagnięto limit ${this.maxSeats()} miejsc`, 'OK', { duration: 3000 });
        return;
      }
      current.add(seat.id);
    }
    this.selectedIds.set(current);
  }

  protected reserve(): void {
    const unreservedIds = this.seats()
      .filter(s => this.selectedIds().has(s.id) && !s.reserved)
      .map(s => s.id);
    if (unreservedIds.length === 0) return;
    this.seatsSelected.emit(unreservedIds);
  }
}
