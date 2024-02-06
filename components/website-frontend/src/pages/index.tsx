import type { GetServerSideProps, InferGetServerSidePropsType, NextPage } from 'next';

import { RankingPageContainer } from '@/components/ranking-page/ranking-page-container';
import Base from '@/layouts/base';
import { logger } from '@/lib/logger';
import { fetchReadRankedAccounts, useReadRankedAccounts } from '@/openapi-codegen/apiComponents';
import { RankedAccount } from '@/openapi-codegen/apiSchemas';

export const getServerSideProps: GetServerSideProps<{
  accounts: RankedAccount[];
}> = async (context) => {
  let accounts: RankedAccount[];
  try {
    accounts = await fetchReadRankedAccounts({});
  } catch (error) {
    logger.error(error);
    accounts = [];
  }

  return {
    props: {
      accounts
    }
  };
};

const Home: NextPage<InferGetServerSidePropsType<typeof getServerSideProps>> = (props: InferGetServerSidePropsType<typeof getServerSideProps>) => {
  const { data: accounts } = useReadRankedAccounts({}, { placeholderData: props.accounts });
  return (
    <Base
      title='Accueil'
      description="Page d'accueil"
    >
      <RankingPageContainer accounts={accounts ?? []} />
    </Base>
  );
};

export default Home;
