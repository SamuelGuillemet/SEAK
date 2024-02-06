import { useCallback, useEffect, useMemo, useState } from 'react';

import { UpdateIcon } from '@radix-ui/react-icons';
import { useQueryClient } from '@tanstack/react-query';

import { RankingPageUserCard } from '@/components/ranking-page/ranking-page-user-card';
import { Button } from '@/components/ui/button';
import { calculateTotalAccount } from '@/hooks/useTotalAccount';
import { useReadMarketData } from '@/openapi-codegen/apiComponents';
import { RankedAccount } from '@/openapi-codegen/apiSchemas';

interface RankingPageContainerProps {
  accounts: RankedAccount[];
}

export function RankingPageContainer(props: Readonly<RankingPageContainerProps>): React.JSX.Element {
  const [marketDataPrices, setMarketDataPrices] = useState<Record<string, number>>({});
  const query = useQueryClient();

  const symbols = useMemo(
    () =>
      new Set(
        props.accounts
          .map((account) => account.stocks?.map((stock) => stock.symbol))
          .flat()
          .filter((symbol): symbol is string => !!symbol)
      ),
    [props.accounts]
  );

  const readMarketData = useReadMarketData({
    onSuccess: (data) => {
      setMarketDataPrices(data.marketData);
    }
  });

  const fetchMarketData = useCallback(() => {
    readMarketData.mutate({
      body: {
        symbols: Array.from(symbols)
      }
    });
  }, [readMarketData, symbols]);

  useEffect(() => {
    fetchMarketData();
  }, [symbols]);

  const rankings = useMemo(() => {
    return props.accounts
      .map((account) => {
        const total = calculateTotalAccount(account, marketDataPrices);
        return {
          account,
          total
        };
      })
      .sort((a, b) => b.total - a.total);
  }, [props.accounts, marketDataPrices]);

  const onClick = () => {
    fetchMarketData();
    query.invalidateQueries({ stale: true });
  };

  return (
    <>
      <div className='flex items-center justify-center h-full mb-4'>
        <h1 className='text-4xl font-bold text-primary'>Classement</h1>
        <Button
          className='rounded-full mx-4'
          size='icon'
          variant='outline'
          onClick={onClick}
        >
          <UpdateIcon className='h-4 w-4' />
          <span className='sr-only'>Reload</span>
        </Button>
      </div>
      <div className='flex flex-col gap-3'>
        <div className='grid gap-4 grid-cols-[repeat(auto-fill,_minmax(350px,1fr))]'>
          {rankings.map((ranking, rank) => (
            <RankingPageUserCard
              key={ranking.account.username}
              account={ranking.account}
              marketDataPrices={marketDataPrices}
              rank={rank + 1}
            />
          ))}
        </div>
      </div>
    </>
  );
}
