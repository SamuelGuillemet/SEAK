import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent } from '@testing-library/react';
import { Mock } from 'vitest';

import { Dialog } from '@/components/ui/dialog';
import * as original from '@/openapi-codegen/apiComponents';

export const mockPush = vi.fn();

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false
      }
    }
  });

export function createWrapper() {
  const testQueryClient = createTestQueryClient();
  // eslint-disable-next-line react/display-name
  return ({ children }: { children: React.ReactNode }) => <QueryClientProvider client={testQueryClient}>{children}</QueryClientProvider>;
}

export function createDialogWrapper() {
  const testQueryClient = createTestQueryClient();
  // eslint-disable-next-line react/display-name
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={testQueryClient}>
      <Dialog open={true}>{children}</Dialog>
    </QueryClientProvider>
  );
}

export function spyOnApi<T extends keyof typeof original>(apiName: T, mockMutateFn: Mock, mockMutateAsyncFn: Mock = vi.fn()) {
  const spyedMethod = vi.spyOn(original, apiName);
  spyedMethod.mockImplementation(() => {
    return {
      mutate: mockMutateFn,
      mutateAsync: mockMutateAsyncFn,
      isLoading: false
    };
  });
}

export function spyOnApiCall<T extends keyof typeof original>(apiName: T, data: any, isLoading = false, isError = false) {
  vi.spyOn(original, apiName).mockImplementation(() => {
    return {
      data,
      isLoading,
      isError
    };
  });
}

export function dropDownClick(dropdownTrigger: HTMLElement) {
  fireEvent.keyDown(dropdownTrigger, { key: 'Enter' });
}
