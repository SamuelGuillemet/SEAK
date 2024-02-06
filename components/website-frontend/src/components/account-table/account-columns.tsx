import { ColumnDef } from '@tanstack/react-table';

import { Checkbox } from '../ui/checkbox';

import { DataTableRowActions } from '@/components/account-table/account-table-row-actions';
import { DataTableCollapsibleTrigger } from '@/components/table/data-table-collapsible-triger';
import { DataTableColumnHeader } from '@/components/table/data-table-column-header';
import { Account } from '@/openapi-codegen/apiSchemas';

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
      <div className='flex items-center w-fit'>
        <Checkbox
          checked={row.getIsSelected()}
          onCheckedChange={(value) => row.toggleSelected(!!value)}
          aria-label='Select row'
          className='hidden sm:block'
        />
        <DataTableCollapsibleTrigger enabled={row.original.enabled} />
      </div>
    ),
    enableSorting: false,
    enableHiding: false
  },
  {
    id: 'id',
    accessorKey: 'id'
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
        return <span className='text-xl text-green-600 font-bold'>✓</span>;
      } else {
        return <span className='text-xl text-destructive font-bold'>✗</span>;
      }
    },
    enableSorting: false
  },
  {
    id: 'actionsDesktop',
    cell: ({ row }) => (
      <div className='items-center'>
        <DataTableRowActions row={row} />
      </div>
    )
  }
];
