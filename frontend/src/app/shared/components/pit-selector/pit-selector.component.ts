import { Component, input, output, signal, computed } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-pit-selector',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, CurrencyPipe],
  templateUrl: './pit-selector.component.html',
  styleUrl: './pit-selector.component.scss',
})
export class PitSelectorComponent {
  readonly maxSeats = input<number>(6);
  readonly pricePerSeat = input<number>(200);
  readonly pitCount = output<number>();

  protected readonly count = signal(0);
  protected readonly total = computed(() => this.count() * this.pricePerSeat());

  protected increment(): void {
    if (this.count() < this.maxSeats()) {
      this.count.update(n => n + 1);
    }
  }

  protected decrement(): void {
    if (this.count() > 0) {
      this.count.update(n => n - 1);
    }
  }

  protected reserve(): void {
    if (this.count() > 0) {
      this.pitCount.emit(this.count());
    }
  }
}
