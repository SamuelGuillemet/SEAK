import { useState } from 'react';

import dynamic from 'next/dynamic';
import Link from 'next/link';

import LoginButton from '@/components/navbar/login-button';
import { useLinks } from '@/hooks/useLinks';
import { navLinks } from '@/utils/constant';
import { pages } from '@/utils/pages';

// Import Dark mode dynamically to avoid SSR issues
const DarkMode = dynamic(() => import('@/components/navbar/dark-mode-switch').then((mod) => mod.DarkMode), { ssr: false });

export default function Navbar(): React.JSX.Element {
  const [isOpen, setIsOpen] = useState(false);

  const { activeLink, filteredLinks } = useLinks(navLinks);

  return (
    <header className='flex flex-col lg:flex-row'>
      <div className='flex justify-between items-center'>
        <button
          id='hamburger-menu'
          aria-label='hamburger-menu'
          onClick={() => setIsOpen(!isOpen)}
          className={(isOpen ? 'active ' : '') + 'lg:hidden'}
        >
          <span className='bg-gray-600 dark:bg-gray-300'></span>
          <span className='bg-gray-600 dark:bg-gray-300'></span>
          <span className='bg-gray-600 dark:bg-gray-300'></span>
        </button>
      </div>
      <div className={(!isOpen ? 'hidden ' : '') + 'lg:flex flex-col lg:flex-row items-center flex-grow'}>
        <div className={'flex p-2 justify-start flex-grow self-start lg:self-center h-full flex-col lg:flex-row pb-4 space-y-2 lg:space-y-0 lg:items-end lg:mb-2'}>
          {filteredLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`text-xl hover:underline mx-4 w-max xl:mx-6 ${activeLink === link ? 'text-primary' : 'text-gray-500 dark:text-gray-400'}`}
            >
              {link.label}
            </Link>
          ))}
        </div>
        <div className='flex flex-row justify-end flex-wrap lg:flex-nowrap'>
          <DarkMode />
          <div className='justify-center self-center mr-4'>
            <LoginButton />
          </div>
        </div>
      </div>
    </header>
  );
}
