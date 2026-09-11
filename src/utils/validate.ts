/**
 * Validation helpers for form inputs.
 * No external dependencies — hand-rolled regex checks only.
 */

/**
 * Validates an email address against a standard email pattern.
 * Requires a local part, an "@", a domain, and a TLD of at least two letters
 * (e.g. rejects "a@b", which has no dot/TLD).
 */
export function isValidEmail(email: string): boolean {
  const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return EMAIL_REGEX.test(email.trim());
}
