import { Component, input } from '@angular/core';

/** Reusable spinner with a message. Usage: <app-loading message="Loading your cart..." /> */
@Component({
  selector: 'app-loading',
  template: `
    <div class="loading" role="status" aria-live="polite">
      <div class="spinner"></div>
      <span>{{ message() }}</span>
    </div>
  `
})
export class Loading {
  message = input('Loading...');
}
