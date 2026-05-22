import { ApiClientError } from './client';
import type { ErrorResponse } from '../types/api';

export function isUnauthorizedError(caught: unknown): boolean {
  return caught instanceof ApiClientError && caught.status === 401;
}

export function toErrorResponse(caught: unknown, fallbackMessage: string): ErrorResponse {
  if (caught instanceof ApiClientError) {
    return caught.response;
  }

  return {
    code: 'NETWORK_ERROR',
    message: fallbackMessage,
    details: [],
  };
}
