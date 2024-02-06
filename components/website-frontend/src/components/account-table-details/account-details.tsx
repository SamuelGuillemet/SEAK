import { Row } from '@tanstack/react-table';

import { BalanceDetails } from '@/components/account-table-details/account-details-balance';
import { StockDetails } from '@/components/account-table-details/account-details-stock';
import { Separator } from '@/components/ui/separator';
import { Account } from '@/openapi-codegen/apiSchemas';

export interface AccountDetailsProps {
  row: Row<Account>;
}

export function AccountDetails({ row }: Readonly<AccountDetailsProps>) {
  const account = row.original;
  if (!account.enabled) {
    return <></>;
  }

  return (
    <div className='grid gap-2 px-4'>
      <BalanceDetails account={account} />
      <Separator />
      <StockDetails account={account} />
    </div>
  );
}
