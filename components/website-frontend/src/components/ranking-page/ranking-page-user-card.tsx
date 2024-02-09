import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Card, CardFooter, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { useTotalAccount } from '@/hooks/useTotalAccount';
import { RankedAccount } from '@/openapi-codegen/apiSchemas';
import { formatPrice } from '@/utils/utils';

interface RankingPageUserCardProps {
  account: RankedAccount;
  marketDataPrices: Record<string, number>;
  rank: number;
}

export function RankingPageUserCard(props: Readonly<RankingPageUserCardProps>) {
  const total = useTotalAccount(props);

  return (
    <Card className='flex flex-col'>
      <CardHeader>
        <div className='flex justify-between'>
          <div className='flex items-center'>
            <Avatar className='h-10 w-10'>
              <AvatarFallback>
                {props.account.firstName[0]}
                {props.account.lastName[0]}
              </AvatarFallback>
            </Avatar>
            <div className='ml-4'>
              <CardTitle className='text-lg'>
                {props.account.firstName} {props.account.lastName}
              </CardTitle>
              <CardDescription className='text-sm text-gray-500 dark:text-gray-400'>Username: {props.account.username}</CardDescription>
            </div>
          </div>
          <span className='text-primary font-bold text-4xl'>#{props.rank}</span>
        </div>
      </CardHeader>
      <CardContent className='flex flex-col grow'>
        <div className='flex flex-col'>
          <div className='grid gap-4'>
            <div className='grid gap-2'>
              <div className='flex justify-between'>
                <span className='text-sm'>Solde:</span>
                <span className='text-sm font-medium'>{formatPrice(props.account.balance ?? 0)}</span>
              </div>
            </div>
            <Separator />
            <div className='border shadow-sm rounded-lg'>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nom</TableHead>
                    <TableHead>Quantité</TableHead>
                    <TableHead>Prix actuel</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {props.account.stocks?.map((stock) => (
                    <TableRow key={stock.symbol}>
                      <TableCell>{stock.symbol}</TableCell>
                      <TableCell>{stock.quantity}</TableCell>
                      <TableCell>{formatPrice(props.marketDataPrices[stock.symbol])}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </div>
          <div className='grow'></div>
        </div>
      </CardContent>
      <Separator />
      <CardFooter>
        <div className='flex flex-grow justify-between pt-2'>
          <span className='text-md'>Total:</span>
          <span className='text-md font-medium'>{formatPrice(total)}</span>
        </div>
      </CardFooter>
    </Card>
  );
}
