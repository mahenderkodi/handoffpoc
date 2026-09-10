import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { isValidEmail } from '../utils/validate';

@Component({
  selector: 'app-subscribe-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './subscribe-form.html',
})
export class SubscribeFormComponent {
  emailError: string | null = null;

  form = new FormGroup({
    email: new FormControl('', Validators.required),
  });

  /** Client-side validation on blur, reusing the same isValidEmail as signup.ts. */
  onEmailBlur(): void {
    const email = this.form.controls.email.value ?? '';

    if (email.trim().length === 0) {
      this.emailError = null;
      return;
    }

    this.emailError = isValidEmail(email) ? null : 'Please enter a valid email address.';
  }

  // TODO (not yet implemented): wire this up to POST /api/subscribe.
  //   - inject HttpClient (or a SubscribeService wrapping it)
  //   - guard on this.form.valid / !this.emailError before sending
  //   - on success (201): show a success message, e.g. "Subscribed successfully"
  //   - on duplicate (409): show "This email is already subscribed"
  //   - on invalid (400): show the server-side validation error
  //   - reset or disable the form appropriately after a successful submit
  onSubmit(): void {
    this.onEmailBlur();
  }
}
