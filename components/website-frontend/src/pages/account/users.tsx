import { GetServerSideProps, InferGetServerSidePropsType, NextPage } from 'next';

import { AccountPageAddAccount } from '@/components/account-page/account-page-add-account';
import { Tabs } from '@/components/account-page/account-page-tabs';
import { accountColumns } from '@/components/account-table/account-columns';
import { AccountDetails } from '@/components/account-table-details/account-details';
import { DataTable } from '@/components/table/data-table';
import Base from '@/layouts/base';
import { logger } from '@/lib/logger';
import { fetchReadAccounts, useReadAccounts } from '@/openapi-codegen/apiComponents';
import { Account } from '@/openapi-codegen/apiSchemas';
import { pages } from '@/utils/pages';
import { verifyScopes, verifySession } from '@/utils/verify-session';

export const getServerSideProps: GetServerSideProps<{
  accounts: Account[];
}> = async (context) => {
  const result = await verifySession(context, pages.account.users);

  if (result.status === 'unauthenticated') {
    return result.redirection;
  }

  const resultScopes = verifyScopes(pages.account.users, result.session);
  if (resultScopes.status === 'unauthorized') {
    return resultScopes.redirection;
  }
  let accounts: Account[];
  try {
    accounts = await fetchReadAccounts({
      headers: {
        authorization: `Bearer ${result.session.token}`
      }
    });
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

const UsersPage: NextPage<InferGetServerSidePropsType<typeof getServerSideProps>> = (props: InferGetServerSidePropsType<typeof getServerSideProps>) => {
  const { data: accounts } = useReadAccounts({}, { placeholderData: props.accounts });

  return (
    <Base title='Gestions des utilisateurs'>
      <div className='flex md:flex-row flex-col flex-grow'>
        <Tabs />
        <div className='flex flex-col flex-grow gap-4'>
          <AccountPageAddAccount />
          <DataTable
            columns={accountColumns}
            data={accounts ?? []}
            CollapsedRow={AccountDetails}
          />
        </div>
      </div>
    </Base>
  );
};

export default UsersPage;
