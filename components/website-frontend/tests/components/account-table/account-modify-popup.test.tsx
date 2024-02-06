import { render, screen, within } from '@testing-library/react';

import { AccountModifyPopup } from '@/components/account-table/account-modify-popup';
import { Account } from '@/openapi-codegen/apiSchemas';
import { createDialogWrapper } from 'tests/utils';

const rowAccount: Account = {
  username: 'admin',
  lastName: 'Admin',
  firstName: 'Admin',
  balance: 0,
  id: 1,
  scope: 'admin',
  enabled: true
};

describe('AccountModifyPopup', () => {
  it('renders the component', async () => {
    render(
      <AccountModifyPopup
        rowAccount={rowAccount}
        isOpen={true}
        setIsOpen={vi.fn()}
      />,
      {
        wrapper: createDialogWrapper()
      }
    );

    const trigger = screen.getByRole('combobox', {
      name: 'Scope'
    });

    expect(screen.getByText('Modifier le compte :')).not.toBeNull();
    expect(within(trigger).getByText('Admin')).not.toBeNull();
    expect(screen.getByLabelText<HTMLInputElement>('Compte actif').value).toBe('on');
    expect(screen.getByRole('button', { name: 'Modifier le compte' })).not.toBeNull();
  });
});
