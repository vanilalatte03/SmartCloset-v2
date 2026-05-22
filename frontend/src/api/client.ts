import type { ApiResponse, ErrorResponse } from '../types/api';

const defaultApiBaseUrl = 'http://localhost:8080';

export const apiBaseUrl = (
  import.meta.env.VITE_API_BASE_URL?.trim() || defaultApiBaseUrl
).replace(/\/$/, '');

export class ApiClientError extends Error {
  readonly response: ErrorResponse;
  readonly status: number;

  constructor(status: number, response: ErrorResponse) {
    super(response.message);
    this.name = 'ApiClientError';
    this.status = status;
    this.response = response;
  }
}

type ApiRequestInit = RequestInit & {
  accessToken?: string;
};

function isErrorResponse(value: unknown): value is ErrorResponse {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as ErrorResponse;
  return (
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string' &&
    Array.isArray(candidate.details)
  );
}

function toErrorResponse(value: unknown, fallbackMessage: string): ErrorResponse {
  if (isErrorResponse(value)) {
    return value;
  }

  return {
    code: 'INVALID_RESPONSE',
    message: fallbackMessage,
    details: [],
  };
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) {
    return undefined;
  }

  try {
    return JSON.parse(text) as unknown;
  } catch {
    return undefined;
  }
}

export async function request<T>(path: string, init: ApiRequestInit = {}): Promise<T> {
  const { accessToken, ...requestInit } = init;
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...requestInit,
    headers,
  });
  const payload = await parseJson(response);

  if (!response.ok) {
    throw new ApiClientError(
      response.status,
      toErrorResponse(payload, 'The SmartCloset API returned an error.')
    );
  }

  const apiResponse = payload as ApiResponse<T> | undefined;
  if (!apiResponse || !('data' in apiResponse)) {
    throw new ApiClientError(
      response.status,
      toErrorResponse(payload, 'The SmartCloset API returned an invalid response.')
    );
  }

  return apiResponse.data;
}
