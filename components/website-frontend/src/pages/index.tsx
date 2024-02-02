import type { NextPage } from 'next';

import Base from '@/layouts/base';

const Home: NextPage = () => {
  return (
    <Base
      title='Accueil'
      description="Page d'accueil"
    >
      <div className='flex flex-col items-center justify-center h-full'>
        <h1 className='text-4xl font-bold text-primary'>Bienvenue !</h1>
        <h2 className='text-2xl font-medium text-gray-500 dark:text-gray-100'>Le site pour la gestion des utilisateurs.</h2>
      </div>
    </Base>
  );
};

export default Home;
