import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/** Counts HTTP calls in flight so the app can show a global progress bar. */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private pending = 0;
  private subject = new BehaviorSubject<boolean>(false);
  active$ = this.subject.asObservable();

  start(): void { this.pending++; this.publish(); }
  stop(): void { this.pending = Math.max(0, this.pending - 1); this.publish(); }

  // emitted in a microtask so it never changes a binding in the middle of change detection
  private publish(): void {
    Promise.resolve().then(() => this.subject.next(this.pending > 0));
  }
}
