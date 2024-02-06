import Link from 'next/link';

import { useLinks } from '@/hooks/useLinks';
import { tabLinks } from '@/utils/constant';

export function Tabs() {
  const { activeLink, filteredLinks } = useLinks(tabLinks);
  return (
    <div className='flex justify-start pr-2 md:border-r md:border-b-0 border-b md:mr-4 mb-4 md:mb-0'>
      <div className='w-full max-w-md md:w-max flex justify-start items-center md:items-start md:flex-col flex-row mb-2 md:mb-0 gap-4'>
        {filteredLinks.map((link) => (
          <Link
            href={link.href}
            key={link.href}
          >
            <div className={`flex items-center gap-2 p-2 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 ${activeLink === link ? 'underline' : ''}`}>
              {link.icon}
              <span className='md:text-lg font-semibold'>{link.label}</span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
