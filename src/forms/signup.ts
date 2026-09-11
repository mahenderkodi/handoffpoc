import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { isValidEmail } from '../utils/validate';

@Component({
  selector: 'app-signup-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './signup.html',
})
export class SignupFormComponent {
  emailError: string | null = null;

  form = new FormGroup({
    email: new FormControl('', Validators.required),
  });

  /** Wired to the email input's (blur) event in signup.html. */
  onEmailBlur(): void {
    const email = this.form.controls.email.value ?? '';

    if (email.trim().length === 0) {
      // Let Validators.required own the empty-input case.
      this.emailError = null;
      return;
    }

    this.emailError = isValidEmail(email)
      ? null
      : 'Please enter a valid email address.';
  }
}
