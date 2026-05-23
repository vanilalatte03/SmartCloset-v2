import { apiBaseUrl, request } from './client';
import type {
  AuthResponse,
  ClothingArchiveResponse,
  ClothingRequest,
  ClothingResponse,
  CurrentUserResponse,
  LoginRequest,
  LocationOptionResponse,
  RecommendationResponse,
  RecommendationWornResponse,
  SignupRequest,
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

export function getLocations(
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

export function getUserLocation(accessToken: string): Promise<UserLocationResponse> {
  return request<UserLocationResponse>('/api/users/me/location', { accessToken });
}

export function updateUserLocation(
  accessToken: string,
  locationCode: string
): Promise<UserLocationResponse> {
  return request<UserLocationResponse>('/api/users/me/location', {
    method: 'PUT',
    accessToken,
    body: JSON.stringify({ locationCode }),
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

export function createRecommendation(accessToken: string): Promise<RecommendationResponse> {
  return request<RecommendationResponse>('/api/recommendations', {
    method: 'POST',
    accessToken,
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
