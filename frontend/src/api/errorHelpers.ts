import { ApiClientError } from './client';
import type { ErrorResponse } from '../types/api';
import {
  getRecommendationFailureCta,
  type RecommendationFailureCta,
} from '../utils/displayMappings';

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

export function toRecommendationFailureCta(
  error: ErrorResponse
): RecommendationFailureCta | null {
  return getRecommendationFailureCta(error.code);
}
