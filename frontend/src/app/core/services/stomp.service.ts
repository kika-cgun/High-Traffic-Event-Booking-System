import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import SockJS from 'sockjs-client';
import { SeatStatusUpdate } from '../../shared/models/seat.model';

@Injectable({ providedIn: 'root' })
export class StompService implements OnDestroy {
  private readonly client = new RxStomp();

  constructor() {
    this.client.configure({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
    });
    this.client.activate();
  }

  watchEvent(eventId: number): Observable<SeatStatusUpdate> {
    return this.client
      .watch(`/topic/seats/${eventId}`)
      .pipe(map(msg => JSON.parse(msg.body) as SeatStatusUpdate));
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }
}
