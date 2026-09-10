/**
 * Server-side validation helpers.
 *
 * Kept as a standalone copy of the client-side isValidEmail (src/utils/validate.ts) rather than
 * an import, since the server (src/server) and the Angular client (src/forms, src/utils) are
 * separate compilation units in this repo.
 */

/**
 * Validates an email address against a standard email pattern.
 * Requires a local part, an "@", a domain, and a TLD of at least two letters
 * (e.g. rejects "a@b", which has no dot/TLD). Regex-based — no new dependencies.
 */
export function isValidEmail(email: string): boolean {
  const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return EMAIL_REGEX.test(email.trim());
}
