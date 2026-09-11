import { isValidEmail } from './validate';

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
