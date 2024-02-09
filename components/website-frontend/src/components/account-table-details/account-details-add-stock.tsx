import { FieldErrors, ResolverResult, useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

import { useQueryClient } from '@tanstack/react-query';

import { Button } from '@/components/ui/button';
import { Form, FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { useUpdateStock } from '@/openapi-codegen/apiComponents';
import { generateApiErrorMessage } from '@/openapi-codegen/apiFetcher';
import { Stock } from '@/openapi-codegen/apiSchemas';
import { patchEmptyString } from '@/utils/utils';

export function addStockResolver(data: Stock): ResolverResult<Stock> {
  const errors: FieldErrors<Stock> = {};

  if (!data.symbol) {
    errors.symbol = {
      type: 'required',
      message: 'Le nom est requis.'
    };
  }

  if (data.quantity === undefined || data.quantity <= 0) {
    errors.quantity = {
      type: 'min',
      message: 'La quantité doit être supérieur à 0.'
    };
  }

  return {
    values: patchEmptyString(data),
    errors
  };
}

export function AddStockRow(props: Readonly<{ accountId: number }>) {
  const queryClient = useQueryClient();

  const form = useForm<Stock>({
    defaultValues: {
      symbol: '',
      quantity: 0
    },
    resolver: addStockResolver
  });

  const addStock = useUpdateStock({
    onSuccess() {
      toast.success('Action ajoutée avec succès');
      queryClient.invalidateQueries({ stale: true });
      // Clear the form
      form.reset();
    },
    onError(error, variables, context) {
      const detail = generateApiErrorMessage(error);
      toast.error(`Erreur lors de l'ajout de l'action. ${detail}`);
    }
  });

  const onSubmit = (data: Stock) => {
    addStock.mutate({
      pathParams: {
        accountId: props.accountId,
        symbol: data.symbol
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
          name='symbol'
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <Input
                  placeholder='Entrer le nom'
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
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
        >
          Ajouter
        </Button>
      </form>
    </Form>
  );
}
