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
  // 추천 실패 코드 중 사용자가 바로 해결할 수 있는 케이스만 화면 이동 CTA로 변환한다.
  return getRecommendationFailureCta(error.code);
}
