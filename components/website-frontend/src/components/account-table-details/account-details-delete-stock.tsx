import toast from 'react-hot-toast';

import { TrashIcon } from '@radix-ui/react-icons';
import { useQueryClient } from '@tanstack/react-query';

import { Button } from '@/components/ui/button';
import { useUpdateStock } from '@/openapi-codegen/apiComponents';
import { generateApiErrorMessage } from '@/openapi-codegen/apiFetcher';
import { Stock } from '@/openapi-codegen/apiSchemas';

export function StockDetailDeleteButton(props: Readonly<{ accountId: number; stock: Stock }>) {
  const queryClient = useQueryClient();

  const deleteStock = useUpdateStock({
    onSuccess() {
      toast.success('Action supprimé avec succès');
      queryClient.invalidateQueries({ stale: true });
    },
    onError(error, variables, context) {
      const detail = generateApiErrorMessage(error);
      toast.error(`Erreur lors de la suppression de l'action. ${detail}`);
    }
  });

  const onClickDelete = () => {
    deleteStock.mutate({
      pathParams: {
        accountId: props.accountId,
        symbol: props.stock.symbol
      },
      body: {
        quantity: 0
      }
    });
  };

  return (
    <Button
      size='icon'
      variant='destructive'
      onClick={onClickDelete}
    >
      <TrashIcon className='h-4 w-4' />
      <span className='sr-only'>Delete</span>
    </Button>
  );
}
