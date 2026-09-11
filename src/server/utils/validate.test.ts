import { isValidEmail } from './validate';
import { _resetSubscriptionsForTests, subscribe, subscriberCountForTests } from '../services/subscriptionService';

describe('isValidEmail', () => {
  it('accepts a well-formed email address', () => {
    expect(isValidEmail('user@example.com')).toBe(true);
  });

  it('accepts an email with a plus tag and multi-level domain', () => {
    expect(isValidEmail('user+tag@mail.example.co.uk')).toBe(true);
  });

  it('rejects an address missing a TLD', () => {
    expect(isValidEmail('a@b')).toBe(false);
  });

  it('rejects an address missing the @ symbol', () => {
    expect(isValidEmail('userexample.com')).toBe(false);
  });

  it('rejects an address containing spaces', () => {
    expect(isValidEmail('user name@example.com')).toBe(false);
  });

  it('rejects an empty string', () => {
    expect(isValidEmail('')).toBe(false);
  });

  it('trims surrounding whitespace before validating', () => {
    expect(isValidEmail('  user@example.com  ')).toBe(true);
  });
});

describe('subscribe (duplicate-submission handling)', () => {
  beforeEach(() => {
    _resetSubscriptionsForTests();
  });

  it('accepts a first-time valid email', () => {
    const result = subscribe('new@example.com');
    expect(result).toEqual({ status: 'subscribed', email: 'new@example.com' });
    expect(subscriberCountForTests()).toBe(1);
  });

  it('rejects an invalid email without storing it', () => {
    const result = subscribe('not-an-email');
    expect(result.status).toBe('invalid');
    expect(subscriberCountForTests()).toBe(0);
  });

  it('flags a duplicate submission of the same email', () => {
    subscribe('repeat@example.com');
    const result = subscribe('repeat@example.com');

    expect(result).toEqual({ status: 'duplicate', email: 'repeat@example.com' });
    expect(subscriberCountForTests()).toBe(1);
  });

  it('treats emails as case-insensitive duplicates', () => {
    subscribe('Person@Example.com');
    const result = subscribe('person@example.com');

    expect(result.status).toBe('duplicate');
    expect(subscriberCountForTests()).toBe(1);
  });

  it('does not treat two different valid emails as duplicates', () => {
    subscribe('first@example.com');
    const result = subscribe('second@example.com');

    expect(result.status).toBe('subscribed');
    expect(subscriberCountForTests()).toBe(2);
  });
});
