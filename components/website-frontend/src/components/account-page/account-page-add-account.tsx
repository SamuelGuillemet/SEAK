import { useState } from 'react';
import toast from 'react-hot-toast';

import { useQueryClient } from '@tanstack/react-query';

import Register from '@/components/register';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTrigger } from '@/components/ui/dialog';

export function AccountPageAddAccount(): React.JSX.Element {
  const query = useQueryClient();
  const [isOpen, setIsOpen] = useState(false);

  const onSuccess = () => {
    toast.success('Compte ajouté avec succès !');
    setIsOpen(false);
    query.invalidateQueries({ stale: true });
  };

  return (
    <Dialog
      open={isOpen}
      onOpenChange={setIsOpen}
    >
      <DialogTrigger asChild>
        <Button className='place-self-end m-4'>Ajouter un compte</Button>
      </DialogTrigger>
      <DialogContent>
        <Register onSuccess={onSuccess} />
      </DialogContent>
    </Dialog>
  );
}
