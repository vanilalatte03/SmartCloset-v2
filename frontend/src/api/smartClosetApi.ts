import { ApiClientError, apiBaseUrl, fetchWithAuthRetry, request } from './client';
import type {
  AccountDeletionRequest,
  AccountDeletionResponse,
  ClothingAnalysisResponse,
  AuthResponse,
  ClothingArchiveResponse,
  ClothingRequest,
  ClothingResponse,
  EmailVerificationConfirmRequest,
  EmailVerificationConfirmResponse,
  EmailVerificationRequest,
  EmailVerificationRequestedResponse,
  ErrorResponse,
  CurrentUserResponse,
  LoginRequest,
  LocationResolveRequest,
  LocationResolveResponse,
  LocationOptionResponse,
  LogoutResponse,
  OAuthProvidersResponse,
  PasswordResetConfirmRequest,
  PasswordResetConfirmResponse,
  PasswordResetRequest,
  PasswordResetRequestedResponse,
  RecommendationFeedbackRequest,
  RecommendationFeedbackResponse,
  RecommendationRequest,
  RecommendationResponse,
  RecommendationWornResponse,
  SignupRequest,
  SignupResponse,
  UpdateCurrentUserRequest,
  UpdateUserLocationRequest,
  UpdateUserPreferencesRequest,
  UserPreferencesResponse,
  UserLocationResponse,
  WeatherResponse,
} from '../types/api';

export function getApiBaseUrl(): string {
  return apiBaseUrl;
}

export function signup(body: SignupRequest): Promise<SignupResponse> {
  return request<SignupResponse>('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function login(body: LoginRequest): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    body: JSON.stringify(body),
  });
}

export function refreshSession(): Promise<AuthResponse> {
  // refresh endpoint 자체가 401이면 세션 복구 실패이므로 다시 refresh를 시도하지 않는다.
  return request<AuthResponse>('/api/auth/refresh', {
    method: 'POST',
    credentials: 'include',
    retryOnUnauthorized: false,
  });
}

export function logout(): Promise<LogoutResponse> {
  // logout은 refresh cookie 정리가 목적이므로 access token 없이 cookie credentials만 보낸다.
  return request<LogoutResponse>('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    retryOnUnauthorized: false,
  });
}

export function requestEmailVerification(
  body: EmailVerificationRequest
): Promise<EmailVerificationRequestedResponse> {
  return request<EmailVerificationRequestedResponse>('/api/auth/email-verification/request', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function confirmEmailVerification(
  body: EmailVerificationConfirmRequest
): Promise<EmailVerificationConfirmResponse> {
  return request<EmailVerificationConfirmResponse>('/api/auth/email-verification/confirm', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function requestPasswordReset(
  body: PasswordResetRequest
): Promise<PasswordResetRequestedResponse> {
  return request<PasswordResetRequestedResponse>('/api/auth/password-reset/request', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function confirmPasswordReset(
  body: PasswordResetConfirmRequest
): Promise<PasswordResetConfirmResponse> {
  return request<PasswordResetConfirmResponse>('/api/auth/password-reset/confirm', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function getOAuthProviders(): Promise<OAuthProvidersResponse> {
  return request<OAuthProvidersResponse>('/api/auth/oauth2/providers');
}

export function getCurrentUser(accessToken: string): Promise<CurrentUserResponse> {
  return request<CurrentUserResponse>('/api/users/me', { accessToken });
}

export function updateCurrentUser(
  accessToken: string,
  body: UpdateCurrentUserRequest
): Promise<CurrentUserResponse> {
  return request<CurrentUserResponse>('/api/users/me', {
    method: 'PATCH',
    accessToken,
    body: JSON.stringify(body),
  });
}

export function deleteAccount(
  accessToken: string,
  body: AccountDeletionRequest
): Promise<AccountDeletionResponse> {
  // 계정 삭제 성공 응답의 refresh cookie 만료 Set-Cookie를 브라우저가 반영해야 한다.
  return request<AccountDeletionResponse>('/api/users/me', {
    method: 'DELETE',
    accessToken,
    credentials: 'include',
    body: JSON.stringify(body),
  });
}

export function searchLocations(
  accessToken: string,
  keyword?: string
): Promise<LocationOptionResponse[]> {
  const params = new URLSearchParams();
  if (keyword && keyword.trim()) {
    params.set('keyword', keyword.trim());
  }

  const query = params.toString();
  return request<LocationOptionResponse[]>(`/api/locations${query ? `?${query}` : ''}`, {
    accessToken,
  });
}

export const getLocations = searchLocations;

export function resolveLocation(
  accessToken: string,
  body: LocationResolveRequest
): Promise<LocationResolveResponse> {
  return request<LocationResolveResponse>('/api/locations/resolve', {
    method: 'POST',
    accessToken,
    body: JSON.stringify(body),
  });
}

export function getUserLocation(accessToken: string): Promise<UserLocationResponse> {
  return request<UserLocationResponse>('/api/users/me/location', { accessToken });
}

export function updateUserLocation(
  accessToken: string,
  requestOrLocationCode: UpdateUserLocationRequest | string
): Promise<UserLocationResponse> {
  // 예전 호출부의 string 인자를 유지하면서, source를 담는 새 request shape도 함께 지원한다.
  const body =
    typeof requestOrLocationCode === 'string'
      ? { locationCode: requestOrLocationCode }
      : requestOrLocationCode;

  return request<UserLocationResponse>('/api/users/me/location', {
    method: 'PUT',
    accessToken,
    body: JSON.stringify(body),
  });
}

export function getUserPreferences(accessToken: string): Promise<UserPreferencesResponse> {
  return request<UserPreferencesResponse>('/api/users/me/preferences', { accessToken });
}

export function updateUserPreferences(
  accessToken: string,
  body: UpdateUserPreferencesRequest
): Promise<UserPreferencesResponse> {
  return request<UserPreferencesResponse>('/api/users/me/preferences', {
    method: 'PUT',
    accessToken,
    body: JSON.stringify(body),
  });
}

export function getCurrentWeather(accessToken: string): Promise<WeatherResponse> {
  return request<WeatherResponse>('/api/weather/current', { accessToken });
}

export function getClothes(accessToken: string): Promise<ClothingResponse[]> {
  return request<ClothingResponse[]>('/api/clothes', { accessToken });
}

export function getArchivedClothes(accessToken: string): Promise<ClothingResponse[]> {
  return request<ClothingResponse[]>('/api/clothes/archived', { accessToken });
}

export function createClothing(
  accessToken: string,
  body: ClothingRequest
): Promise<ClothingResponse> {
  return request<ClothingResponse>('/api/clothes', {
    method: 'POST',
    accessToken,
    body: JSON.stringify(body),
  });
}

export function updateClothing(
  accessToken: string,
  clothingId: number,
  body: ClothingRequest
): Promise<ClothingResponse> {
  return request<ClothingResponse>(`/api/clothes/${clothingId}`, {
    method: 'PUT',
    accessToken,
    body: JSON.stringify(body),
  });
}

export async function analyzeClothingImage(
  accessToken: string,
  file: File
): Promise<ClothingAnalysisResponse> {
  const formData = new FormData();
  formData.append('image', file);

  const response = await fetchWithAuthRetry('/api/clothes/analyze-image', accessToken, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
    body: formData,
  });

  if (!response.ok) {
    throw new ApiClientError(response.status, await parseImageErrorResponse(response));
  }

  const payload = (await response.json()) as { data?: ClothingAnalysisResponse };
  if (!payload.data) {
    throw new ApiClientError(response.status, {
      code: 'INVALID_RESPONSE',
      message: 'SmartCloset 분석 API 응답 형식이 올바르지 않습니다.',
      details: [],
    });
  }

  return payload.data;
}

export function archiveClothing(
  accessToken: string,
  clothingId: number
): Promise<ClothingArchiveResponse> {
  return request<ClothingArchiveResponse>(`/api/clothes/${clothingId}/archive`, {
    method: 'PATCH',
    accessToken,
  });
}

export function unarchiveClothing(
  accessToken: string,
  clothingId: number
): Promise<ClothingArchiveResponse> {
  return request<ClothingArchiveResponse>(`/api/clothes/${clothingId}/unarchive`, {
    method: 'PATCH',
    accessToken,
  });
}

async function parseImageErrorResponse(response: Response): Promise<ErrorResponse> {
  try {
    const payload = (await response.json()) as Partial<ErrorResponse>;
    if (
      typeof payload.code === 'string' &&
      typeof payload.message === 'string' &&
      Array.isArray(payload.details)
    ) {
      return payload as ErrorResponse;
    }
  } catch {
    // Use the fallback below for non-JSON image errors.
  }

  return {
    code: 'INVALID_RESPONSE',
    message: 'SmartCloset 이미지 API 요청이 실패했습니다.',
    details: [],
  };
}

export async function uploadClothingImage(
  accessToken: string,
  clothingId: number,
  file: File
): Promise<ClothingResponse> {
  const formData = new FormData();
  formData.append('image', file);

  // multipart 요청은 브라우저가 boundary를 설정해야 하므로 Content-Type을 직접 넣지 않는다.
  const response = await fetchWithAuthRetry(`/api/clothes/${clothingId}/image`, accessToken, {
    method: 'PUT',
    headers: {
      Accept: 'application/json',
    },
    body: formData,
  });

  if (!response.ok) {
    throw new ApiClientError(response.status, await parseImageErrorResponse(response));
  }

  const payload = (await response.json()) as { data?: ClothingResponse };
  if (!payload.data) {
    throw new ApiClientError(response.status, {
      code: 'INVALID_RESPONSE',
      message: 'SmartCloset 이미지 API 응답 형식이 올바르지 않습니다.',
      details: [],
    });
  }

  return payload.data;
}

export async function deleteClothingImage(
  accessToken: string,
  clothingId: number
): Promise<ClothingResponse> {
  return request<ClothingResponse>(`/api/clothes/${clothingId}/image`, {
    method: 'DELETE',
    accessToken,
  });
}

export async function getClothingImageBlob(
  accessToken: string,
  imageUrl: string
): Promise<Blob> {
  // 보호 이미지 URL은 <img src>로 직접 열 수 없어서 Authorization header를 붙여 blob으로 가져온다.
  const response = await fetchWithAuthRetry(imageUrl, accessToken);

  if (!response.ok) {
    throw new ApiClientError(response.status, await parseImageErrorResponse(response));
  }

  return response.blob();
}

export function createRecommendation(
  accessToken: string,
  body?: RecommendationRequest
): Promise<RecommendationResponse> {
  return request<RecommendationResponse>('/api/recommendations', {
    method: 'POST',
    accessToken,
    body: body ? JSON.stringify(body) : undefined,
  });
}

export function getRecommendationHistory(
  accessToken: string,
  limit = 20
): Promise<RecommendationResponse[]> {
  const query = new URLSearchParams({ limit: String(limit) }).toString();
  return request<RecommendationResponse[]>(`/api/recommendations?${query}`, {
    accessToken,
  });
}

export function markRecommendationWorn(
  accessToken: string,
  recommendationId: number
): Promise<RecommendationWornResponse> {
  return request<RecommendationWornResponse>(
    `/api/recommendations/${recommendationId}/worn`,
    {
      method: 'PATCH',
      accessToken,
    }
  );
}

export function replaceRecommendationFeedback(
  accessToken: string,
  recommendationId: number,
  body: RecommendationFeedbackRequest
): Promise<RecommendationFeedbackResponse> {
  return request<RecommendationFeedbackResponse>(
    `/api/recommendations/${recommendationId}/feedback`,
    {
      method: 'PUT',
      accessToken,
      body: JSON.stringify(body),
    }
  );
}
