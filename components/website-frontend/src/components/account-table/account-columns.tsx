import { ColumnDef } from '@tanstack/react-table';

import { DataTableRowActions } from './account-table-row-actions';

import { DataTableColumnHeader } from '@/components/table/data-table-column-header';
import { Checkbox } from '@/components/ui/checkbox';
import { Account } from '@/openapi-codegen/apiSchemas';
import { formatPrice } from '@/utils/utils';

export const accountColumns: ColumnDef<Account>[] = [
  {
    id: 'select',
    header: ({ table }) => (
      <Checkbox
        checked={table.getIsAllPageRowsSelected() || (table.getIsSomePageRowsSelected() && 'indeterminate')}
        onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
        aria-label='Select all'
        className='hidden sm:block'
      />
    ),
    cell: ({ row }) => (
      <Checkbox
        checked={row.getIsSelected()}
        onCheckedChange={(value) => row.toggleSelected(!!value)}
        aria-label='Select row'
        className='hidden sm:block'
      />
    ),
    enableSorting: false,
    enableHiding: false
  },
  {
    id: 'id',
    accessorKey: 'id'
  },
  {
    id: 'actionsMobile',
    cell: ({ row }) => (
      <div className='sm:hidden flex items-center'>
        <DataTableRowActions row={row} />
      </div>
    )
  },
  {
    id: 'firstName',
    accessorKey: 'firstName',
    header: ({ column }) => (
      <DataTableColumnHeader
        column={column}
        title='Nom'
      />
    )
  },
  {
    id: 'lastName',
    accessorKey: 'lastName',
    header: ({ column }) => (
      <DataTableColumnHeader
        column={column}
        title='Prénom'
      />
    )
  },
  {
    id: 'balance',
    accessorKey: 'balance',
    header: ({ column }) => (
      <DataTableColumnHeader
        column={column}
        title='Solde'
      />
    ),
    cell: ({ row }) => {
      const balance = row.getValue<number>('balance');
      return formatPrice(balance);
    },
    enableSorting: true
  },
  {
    id: 'scope',
    accessorKey: 'scope',
    header: ({ column }) => (
      <DataTableColumnHeader
        column={column}
        title='Rôle'
      />
    )
  },
  {
    accessorKey: 'enabled',
    id: 'enabled',
    header: ({ column }) => (
      <DataTableColumnHeader
        column={column}
        title='Actif'
      />
    ),
    cell: ({ row }) => {
      const enabled = row.getValue<boolean>('enabled');

      if (enabled) {
        return <span className='text-primary font-bold'>✓</span>;
      } else {
        return <span className='text-destructive font-bold'>✗</span>;
      }
    },
    enableSorting: false
  },
  {
    id: 'actionsDesktop',
    cell: ({ row }) => (
      <div className='hidden sm:flex items-center'>
        <DataTableRowActions row={row} />
      </div>
    )
  }
];
