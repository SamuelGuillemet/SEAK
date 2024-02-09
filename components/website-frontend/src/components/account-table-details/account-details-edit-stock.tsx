import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

import { CheckIcon } from '@radix-ui/react-icons';
import { useQueryClient } from '@tanstack/react-query';

import { Button } from '@/components/ui/button';
import { Form, FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { useUpdateStock } from '@/openapi-codegen/apiComponents';
import { generateApiErrorMessage } from '@/openapi-codegen/apiFetcher';
import { Stock, StockUpdate } from '@/openapi-codegen/apiSchemas';

export function EditStockRow(props: Readonly<{ accountId: number; stock: Stock; setIsEditing: (isEditing: boolean) => void }>) {
  const queryClient = useQueryClient();

  const updateStock = useUpdateStock({
    onSuccess() {
      toast.success('Action modifiée avec succès');
      queryClient.invalidateQueries({ stale: true });
      props.setIsEditing(false);
    },
    onError(error, variables, context) {
      const detail = generateApiErrorMessage(error);
      toast.error(`Erreur lors de la modification de l'action. ${detail}`);
    }
  });

  const form = useForm<StockUpdate>({
    defaultValues: {
      quantity: props.stock.quantity
    }
  });

  const onSubmit = (data: StockUpdate) => {
    updateStock.mutate({
      pathParams: {
        accountId: props.accountId,
        symbol: props.stock.symbol
      },
      body: {
        quantity: data.quantity
      }
    });
  };

  return (
    <Form {...form}>
      <form
        className='grid grid-flow-col gap-x-4 items-baseline'
        onSubmit={form.handleSubmit(onSubmit)}
      >
        <FormField
          control={form.control}
          name='quantity'
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <Input
                  placeholder='Entrer la quantité'
                  type='number'
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button
          type='submit'
          variant='outline'
          size='icon'
        >
          <CheckIcon className='h-4 w-4' />
        </Button>
      </form>
    </Form>
  );
}
