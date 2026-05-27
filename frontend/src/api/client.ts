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
  retryOnUnauthorized?: boolean;
};

type RefreshAccessTokenHandler = () => Promise<string | null>;

let refreshAccessTokenHandler: RefreshAccessTokenHandler | null = null;
let refreshAccessTokenPromise: Promise<string | null> | null = null;

export function setRefreshAccessTokenHandler(handler: RefreshAccessTokenHandler | null) {
  refreshAccessTokenHandler = handler;
  if (!handler) {
    refreshAccessTokenPromise = null;
  }
}

function refreshAccessTokenOnce(): Promise<string | null> {
  if (!refreshAccessTokenHandler) {
    return Promise.resolve(null);
  }

  if (!refreshAccessTokenPromise) {
    refreshAccessTokenPromise = refreshAccessTokenHandler().finally(() => {
      refreshAccessTokenPromise = null;
    });
  }

  return refreshAccessTokenPromise;
}

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

export async function parseJson(response: Response): Promise<unknown> {
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
  const { accessToken, retryOnUnauthorized = true, ...requestInit } = init;
  const response = await send(path, accessToken, requestInit);
  const payload = await parseJson(response);

  if (
    response.status === 401 &&
    accessToken &&
    retryOnUnauthorized &&
    refreshAccessTokenHandler
  ) {
    const nextAccessToken = await refreshAccessTokenOnce();
    if (nextAccessToken) {
      const retryResponse = await send(path, nextAccessToken, requestInit);
      const retryPayload = await parseJson(retryResponse);
      return unwrapApiResponse<T>(retryResponse, retryPayload);
    }
  }

  return unwrapApiResponse<T>(response, payload);
}

async function send(
  path: string,
  accessToken: string | undefined,
  requestInit: RequestInit
): Promise<Response> {
  const headers = new Headers(requestInit.headers);
  headers.set('Accept', 'application/json');
  if (requestInit.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...requestInit,
    headers,
  });
  return response;
}

function unwrapApiResponse<T>(response: Response, payload: unknown): T {
  if (!response.ok) {
    throw new ApiClientError(
      response.status,
      toErrorResponse(payload, 'SmartCloset API 요청이 실패했습니다.')
    );
  }

  const apiResponse = payload as ApiResponse<T> | undefined;
  if (!apiResponse || !('data' in apiResponse)) {
    throw new ApiClientError(
      response.status,
      toErrorResponse(payload, 'SmartCloset API 응답 형식이 올바르지 않습니다.')
    );
  }

  return apiResponse.data;
}

export async function fetchWithAuthRetry(
  path: string,
  accessToken: string,
  init: RequestInit = {}
): Promise<Response> {
  const response = await fetchWithAccessToken(path, accessToken, init);
  if (response.status !== 401 || !refreshAccessTokenHandler) {
    return response;
  }

  const nextAccessToken = await refreshAccessTokenOnce();
  if (!nextAccessToken) {
    return response;
  }

  return fetchWithAccessToken(path, nextAccessToken, init);
}

async function fetchWithAccessToken(
  path: string,
  accessToken: string,
  init: RequestInit
): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${accessToken}`);

  return fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
  });
}
