import { useMemo } from 'react';

import { RankedAccount } from '@/openapi-codegen/apiSchemas';

interface UseTotalAccountProps {
  account: RankedAccount;
  marketDataPrices: Record<string, number>;
}

export function calculateTotalAccount(account: RankedAccount, marketDataPrices: Record<string, number>) {
  return (
    (account.balance ?? 0) +
    (account.stocks?.reduce((acc, stock) => {
      const price = marketDataPrices[stock.symbol] ?? 0;
      return acc + stock.quantity * price;
    }, 0) ?? 0)
  );
}

export function useTotalAccount(props: Readonly<UseTotalAccountProps>) {
  return useMemo(() => {
    return calculateTotalAccount(props.account, props.marketDataPrices);
  }, [props.account, props.marketDataPrices]);
}
