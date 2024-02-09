import { useState } from 'react';
import { FieldErrors, ResolverResult, useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

import { useQueryClient } from '@tanstack/react-query';

import { Button as DataButton } from '@/components/button';
import { Button } from '@/components/ui/button';
import { Form, FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import useOnEchap from '@/hooks/useOnEchap';
import { useUpdateBalance } from '@/openapi-codegen/apiComponents';
import { generateApiErrorMessage } from '@/openapi-codegen/apiFetcher';
import { Account, BalanceUpdate } from '@/openapi-codegen/apiSchemas';
import { formatPrice, patchEmptyString } from '@/utils/utils';

export function balanceUpdateResolver(data: BalanceUpdate): ResolverResult<BalanceUpdate> {
  const errors: FieldErrors<BalanceUpdate> = {};

  if (data.balance === null) {
    errors.balance = {
      type: 'required',
      message: 'Le solde est requis.'
    };
  } else if (data.balance < 0) {
    errors.balance = {
      type: 'min',
      message: 'Le solde doit être supérieur ou égal à 0.'
    };
  }

  return {
    values: patchEmptyString(data),
    errors
  };
}
export interface BalanceDetailProps {
  account: Account;
}

export function BalanceDetails(props: Readonly<BalanceDetailProps>) {
  const { account } = props;
  const queryClient = useQueryClient();
  const [isEditingBalance, setIsEditingBalance] = useState(false);

  const updateBalance = useUpdateBalance({
    onSuccess() {
      setIsEditingBalance(false);
      toast.success('Modification du solde effectué avec succès');
      queryClient.invalidateQueries({ stale: true });
    },
    onError(error, variables, context) {
      const detail = generateApiErrorMessage(error);
      toast.error(`Erreur lors de la modification du solde. ${detail}`);
    }
  });

  const form = useForm<BalanceUpdate>({
    defaultValues: {
      balance: account.balance ?? 0
    },
    resolver: balanceUpdateResolver,
    mode: 'all'
  });

  useOnEchap(() => {
    setIsEditingBalance(false);
  });

  const onSubmit = (data: BalanceUpdate) => {
    updateBalance.mutate({
      body: {
        balance: data.balance
      },
      pathParams: {
        accountId: account.id
      }
    });
  };

  return (
    <Form {...form}>
      <form
        className='grid grow gap-4 items-baseline mt-2'
        onSubmit={form.handleSubmit(onSubmit)}
      >
        <div className='flex items-center'>
          <span className='text-lg font-medium'>Solde:</span>
          <div className='flex flex-col ml-2'>
            {isEditingBalance ? (
              <FormField
                control={form.control}
                name='balance'
                render={({ field }) => (
                  <FormItem className='flex flex-row items-center space-y-0'>
                    <FormControl>
                      <Input
                        className='w-24 remove-arrows'
                        type='number'
                        step='any'
                        min={0}
                        {...field}
                        value={field.value ?? 0}
                      />
                    </FormControl>
                    <span className='font-medium text-lg mx-2'>€</span>
                    <FormMessage />
                  </FormItem>
                )}
              />
            ) : (
              <span className='font-medium text-lg'>{formatPrice(account.balance ?? 0)}</span>
            )}
          </div>
        </div>
        <div className='flex items-end mb-2 grow'>
          <Button
            className={isEditingBalance ? 'hidden' : ''}
            type='button'
            onClick={() => setIsEditingBalance(true)}
          >
            Modifier le solde
          </Button>
          <DataButton
            loading={updateBalance.isPending}
            type='submit'
            hidden={!isEditingBalance}
          >
            Enregistrer
          </DataButton>
        </div>
      </form>
    </Form>
  );
}
