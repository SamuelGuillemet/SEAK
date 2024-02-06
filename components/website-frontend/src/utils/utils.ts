import { Session } from 'next-auth';
import { JWT } from 'next-auth/jwt';

import { LinksType } from '@/utils/constant';

/**
 * Parses a JWT token and returns the information inside it, or null if the token is invalid.
 * @param token - The JWT token to parse.
 * @returns The information inside the token or null if the token is invalid.
 */
export function parseJwt(token: string): JWT | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url?.replace(/-/g, '+').replace(/_/g, '/');
    const buffer = Buffer.from(base64, 'base64');
    const jsonPayload = decodeURIComponent(
      buffer
        .toString('utf-8')
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

/**
 * Formats a number as a price in EUR.
 * @param amount - The amount to format.
 * @param signDisplay - Whether to display the currency sign always, never, or only when the amount is negative or zero.
 * @returns The formatted price as a string.
 */
export function formatPrice(amount: number | null | undefined, signDisplay: 'always' | 'never' | 'auto' | 'exceptZero' = 'auto'): string {
  if (amount === undefined) return '';
  if (amount === null) return '';
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    signDisplay: signDisplay
  }).format(amount);
}

/**
 * Replaces empty strings in an object with null.
 * @param obj - The object to patch.
 * @returns The patched object.
 */
export function patchEmptyString(obj: Record<string, unknown>): Record<string, unknown> {
  Object.keys(obj).forEach((key) => {
    if (obj[key] === '') obj[key] = null;
  });
  return obj;
}

export function findActiveLink(links: LinksType, pathname: string) {
  return links.find((link) => link.href === pathname);
}

export function filterLinks(links: LinksType, session: Session | null) {
  if (session) {
    const userRoles = session.scopes;
    return links.filter((link) => link.scopes.some((role) => userRoles.includes(role)));
  } else {
    return [];
  }
}
