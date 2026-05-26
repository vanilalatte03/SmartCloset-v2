import { ApiClientError, apiBaseUrl, request } from './client';
import type {
  AuthResponse,
  ClothingArchiveResponse,
  ClothingRequest,
  ClothingResponse,
  ErrorResponse,
  CurrentUserResponse,
  LoginRequest,
  LocationResolveRequest,
  LocationResolveResponse,
  LocationOptionResponse,
  RecommendationFeedbackRequest,
  RecommendationFeedbackResponse,
  RecommendationRequest,
  RecommendationResponse,
  RecommendationWornResponse,
  SignupRequest,
  UpdateUserLocationRequest,
  UpdateUserPreferencesRequest,
  UserPreferencesResponse,
  UserLocationResponse,
  WeatherResponse,
} from '../types/api';

export function getApiBaseUrl(): string {
  return apiBaseUrl;
}

export function signup(body: SignupRequest): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function login(body: LoginRequest): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function getCurrentUser(accessToken: string): Promise<CurrentUserResponse> {
  return request<CurrentUserResponse>('/api/users/me', { accessToken });
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

export function archiveClothing(
  accessToken: string,
  clothingId: number
): Promise<ClothingArchiveResponse> {
  return request<ClothingArchiveResponse>(`/api/clothes/${clothingId}/archive`, {
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

  const response = await fetch(`${apiBaseUrl}/api/clothes/${clothingId}/image`, {
    method: 'PUT',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
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
  const response = await fetch(`${apiBaseUrl}${imageUrl}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

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
