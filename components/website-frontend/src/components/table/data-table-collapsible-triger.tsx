import { ChevronDownIcon } from '@radix-ui/react-icons';

import { Button } from '@/components/ui/button';
import { CollapsibleTrigger } from '@/components/ui/collapsible';

export function DataTableCollapsibleTrigger(props: Readonly<{ enabled: boolean }>) {
  if (!props.enabled) {
    return null;
  }
  return (
    <CollapsibleTrigger asChild>
      <Button
        variant='ghost'
        size='sm'
        className='h-8'
      >
        <ChevronDownIcon className='h-4 w-4' />
        <span className='sr-only'>Toggle details</span>
      </Button>
    </CollapsibleTrigger>
  );
}
