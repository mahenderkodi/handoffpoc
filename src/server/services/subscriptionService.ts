import { isValidEmail } from '../utils/validate';

export type SubscribeResult =
  | { status: 'subscribed'; email: string }
  | { status: 'duplicate'; email: string }
  | { status: 'invalid'; email: string };

/**
 * In-memory subscriber store. Resets on process restart — there's no existing persistence
 * pattern in this repo, and the task only asked for "in-memory (or whatever the repo's existing
 * storage pattern is)".
 */
const subscribedEmails = new Set<string>();

/** Normalizes for dedupe purposes (case-insensitive, trimmed) without altering stored casing. */
function normalize(email: string): string {
  return email.trim().toLowerCase();
}

export function subscribe(rawEmail: string): SubscribeResult {
  const email = rawEmail.trim();

  if (!isValidEmail(email)) {
    return { status: 'invalid', email };
  }

  const key = normalize(email);
  if (subscribedEmails.has(key)) {
    return { status: 'duplicate', email };
  }

  subscribedEmails.add(key);
  return { status: 'subscribed', email };
}

/** Test-only helper to reset state between test cases. */
export function _resetSubscriptionsForTests(): void {
  subscribedEmails.clear();
}

export function subscriberCountForTests(): number {
  return subscribedEmails.size;
}
