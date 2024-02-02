import { ArrowDownIcon, ArrowUpIcon, CaretSortIcon } from '@radix-ui/react-icons';
import { Column } from '@tanstack/react-table';

import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';

interface DataTableColumnHeaderProps<TData, TValue> extends React.HTMLAttributes<HTMLDivElement> {
  column: Column<TData, TValue>;
  title: string;
}

export function DataTableColumnHeader<TData, TValue>({ column, title, className }: Readonly<DataTableColumnHeaderProps<TData, TValue>>) {
  if (!column.getCanSort()) {
    return <div className={cn(className)}>{title}</div>;
  }

  let icon;
  if (column.getIsSorted() === 'desc') {
    icon = <ArrowDownIcon className='ml-2 h-4 w-4' />;
  } else if (column.getIsSorted() === 'asc') {
    icon = <ArrowUpIcon className='ml-2 h-4 w-4' />;
  } else {
    icon = <CaretSortIcon className='ml-2 h-4 w-4' />;
  }

  return (
    <div className={cn('flex items-center space-x-2', className)}>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant='ghost'
            size='sm'
            className='-ml-3 h-8 data-[state=open]:bg-accent min-w-max'
          >
            <span>{title}</span>
            {icon}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align='start'>
          <DropdownMenuItem onClick={() => column.toggleSorting(false)}>
            <ArrowUpIcon className='mr-2 h-3.5 w-3.5 text-muted-foreground/70' />
            Asc
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => column.toggleSorting(true)}>
            <ArrowDownIcon className='mr-2 h-3.5 w-3.5 text-muted-foreground/70' />
            Desc
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
