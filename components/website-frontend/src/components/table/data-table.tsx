import { useState } from 'react';

import { ColumnDef, Row, SortingState, VisibilityState, flexRender, getCoreRowModel, getFacetedRowModel, getFacetedUniqueValues, getFilteredRowModel, getPaginationRowModel, getSortedRowModel, useReactTable } from '@tanstack/react-table';

import { DataTablePagination } from '@/components/table/data-table-pagination';
import { Collapsible, CollapsibleContent } from '@/components/ui/collapsible';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

interface DataTableProps<TData extends { id: number }, TValue> {
  columns: ColumnDef<TData, TValue>[];
  data: TData[];
  pagination?: boolean;
  CollapsedRow?: ({ row }: { row: Row<TData> }) => React.ReactNode;
}

function BasicTableRows<TData extends { id: number }>({ row }: Readonly<{ row: Row<TData> }>) {
  return (
    <TableRow key={row.id}>
      {row.getVisibleCells().map((cell) => (
        <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
      ))}
    </TableRow>
  );
}

function RowWithCollapsibleDetails<TData extends { id: number }>({ row, CollapsedRow }: Readonly<{ row: Row<TData>; CollapsedRow: ({ row }: { row: Row<TData> }) => React.ReactNode }>) {
  return (
    <Collapsible
      key={row.id}
      asChild
    >
      <>
        <BasicTableRows row={row} />
        <CollapsibleContent asChild>
          <TableRow
            key={row.id + '-collapsed'}
            className='bg-muted/10'
          >
            <TableCell colSpan={row.getVisibleCells().length}>{CollapsedRow({ row })}</TableCell>
          </TableRow>
        </CollapsibleContent>
      </>
    </Collapsible>
  );
}

export function DataTable<TData extends { id: number }, TValue>({ columns, data, pagination = true, CollapsedRow }: Readonly<DataTableProps<TData, TValue>>) {
  const [rowSelection, setRowSelection] = useState({});
  const columnVisibility: VisibilityState = { id: false };
  const [sorting, setSorting] = useState<SortingState>([{ id: 'id', desc: true }]);

  const table = useReactTable({
    sortDescFirst: true,
    data,
    columns,
    state: {
      sorting,
      columnVisibility,
      rowSelection
    },
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFacetedRowModel: getFacetedRowModel(),
    getFacetedUniqueValues: getFacetedUniqueValues()
  });

  if (!pagination) {
    table.getState().pagination.pageSize = data.length;
  }

  return (
    <div className='space-y-4 lg:mx-8 mx-2'>
      <div className='rounded-md border'>
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  return <TableHead key={header.id}>{header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}</TableHead>;
                })}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {table.getRowModel().rows?.length ? (
              table.getRowModel().rows.map((row) =>
                CollapsedRow ? (
                  <RowWithCollapsibleDetails
                    key={row.id}
                    row={row}
                    CollapsedRow={CollapsedRow}
                  />
                ) : (
                  <BasicTableRows
                    key={row.id}
                    row={row}
                  />
                )
              )
            ) : (
              <TableRow>
                <TableCell
                  colSpan={columns.length}
                  className='h-24 text-center'
                >
                  No results.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      <div className='flex items-center justify-between'>
        <div className='flex-1 text-sm text-muted-foreground hidden sm:block'>
          {table.getFilteredSelectedRowModel().rows.length} sur {table.getFilteredRowModel().rows.length} lignes(s) sélectionnée.
        </div>
        {pagination && <DataTablePagination table={table} />}
      </div>
    </div>
  );
}
