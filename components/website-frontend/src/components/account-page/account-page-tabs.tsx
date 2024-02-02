import Link from 'next/link';
import { useRouter } from 'next/router';
import type { Session } from 'next-auth';
import { useSession } from 'next-auth/react';

import { SecurityScopes } from '@/openapi-codegen/apiSchemas';
import { pages } from '@/utils/pages';

type LinksType = Array<{
  label: string;
  href: string;
  scopes: SecurityScopes[];
}>;

export const links: LinksType = [
  { label: 'Accueil', href: pages.account.index, scopes: ['user', 'admin'] },
  { label: 'Comptes utilisateurs', href: pages.account.users, scopes: ['admin'] }
];

export function findActiveLink(pathname: string) {
  return links.find((link) => link.href.search(pathname) !== -1);
}

export function filterLinks(session: Session | null) {
  if (session) {
    const userRoles = session.scopes;
    return links.filter((link) => link.scopes.some((role) => userRoles.includes(role)));
  } else {
    return [];
  }
}

export function Tabs() {
  const { pathname } = useRouter();
  const { data: session } = useSession();

  const activeLink = findActiveLink(pathname);
  const filteredLinks = filterLinks(session);

  return (
    <div className='flex justify-center pr-2 md:border-r md:border-b-0 border-b md:mr-4 mb-4 md:mb-0'>
      <div className='w-full max-w-md md:w-max flex justify-start items-center md:items-start md:flex-col flex-row mb-2 md:mb-0'>
        {filteredLinks.map((link) => (
          <Link
            href={link.href}
            key={link.href}
            className={`px-3 py-2 text-sm w-full font-medium ${activeLink === link ? 'bg-slate-400 dark:bg-gray-800' : 'text-gray-900 hover:bg-slate-300 dark:text-gray-300 dark:hover:bg-gray-700'}`}
          >
            {link.label}
          </Link>
        ))}
      </div>
    </div>
  );
}
