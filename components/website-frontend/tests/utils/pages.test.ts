import { pages } from '@/utils/pages';

describe('pages', () => {
  it('should have the correct routes', () => {
    expect(pages.signin).toBe('/login');
    expect(pages.signout).toBe('/logout');
    expect(pages.signup).toBe('/register');
    expect(pages.index).toBe('/');
    expect(pages.account.index).toBe('/account');
    expect(pages.account.users).toBe('/account/users');
  });
});
