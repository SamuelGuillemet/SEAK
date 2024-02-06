import { useState } from 'react';

import { CountdownTimerIcon, Pencil2Icon } from '@radix-ui/react-icons';

import { AddStockRow } from '@/components/account-table-details/account-details-add-stock';
import { StockDetailDeleteButton } from '@/components/account-table-details/account-details-delete-stock';
import { EditStockRow } from '@/components/account-table-details/account-details-edit-stock';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import useOnEchap from '@/hooks/useOnEchap';
import { Account } from '@/openapi-codegen/apiSchemas';

interface StockDetailProps {
  account: Account;
}

export function StockDetails(props: Readonly<StockDetailProps>) {
  const [isEditing, setIsEditing] = useState(false);

  useOnEchap(() => {
    setIsEditing(false);
  });

  const { account } = props;
  return (
    <>
      <p className='text-lg font-medium'>Actions:</p>
      <div className='border rounded-lg w-full'>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className='text-start w-[40%]'>Nom</TableHead>
              <TableHead className='text-start w-[50%]'>Quantité</TableHead>
              <TableHead className='text-start w-[10%]'>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {account.stocks?.map((stock) => (
              <TableRow key={stock.symbol}>
                <TableCell className='font-medium'>{stock.symbol}</TableCell>
                <TableCell className='font-medium'>
                  {isEditing ? (
                    <EditStockRow
                      accountId={account.id}
                      stock={stock}
                      setIsEditing={setIsEditing}
                    />
                  ) : (
                    stock.quantity
                  )}
                </TableCell>
                <TableCell>
                  <Button
                    size='icon'
                    variant='outline'
                    className='mr-2'
                    onClick={() => setIsEditing(!isEditing)}
                  >
                    {isEditing ? <CountdownTimerIcon className='h-4 w-4' /> : <Pencil2Icon className='h-4 w-4' />}
                    <span className='sr-only'>Edit</span>
                  </Button>
                  <StockDetailDeleteButton
                    accountId={account.id}
                    stock={stock}
                  />
                </TableCell>
              </TableRow>
            ))}
            {account.stocks?.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={3}
                  className='h-4 text-center'
                >
                  Aucune action.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      <Separator />
      <p className='text-lg font-medium'>Ajouter une action:</p>
      <AddStockRow accountId={account.id} />
    </>
  );
}
