/**
 * Object containing all the routes of the application.
 */
export const pages = {
  signin: '/login',
  signout: '/logout',
  signup: '/register',
  index: '/',
  account: {
    index: '/account',
    users: '/account/users'
  }
} as const;
