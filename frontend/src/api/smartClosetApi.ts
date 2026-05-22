import { apiBaseUrl, request } from './client';
import type {
  AuthResponse,
  ClothingRequest,
  ClothingResponse,
  CurrentUserResponse,
  LocationOptionResponse,
  LoginRequest,
  RecommendationResponse,
  RecommendationWornResponse,
  SignupRequest,
  UserLocationResponse,
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
    accessToken,
    method: 'PUT',
    body: JSON.stringify({ locationCode }),
  });
}

export function getClothes(accessToken: string): Promise<ClothingResponse[]> {
  return request<ClothingResponse[]>('/api/clothes', { accessToken });
}

export function createClothing(
  accessToken: string,
  body: ClothingRequest
): Promise<ClothingResponse> {
  return request<ClothingResponse>('/api/clothes', {
    accessToken,
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function createRecommendation(accessToken: string): Promise<RecommendationResponse> {
  return request<RecommendationResponse>('/api/recommendations', {
    accessToken,
    method: 'POST',
  });
}

export function markRecommendationWorn(
  accessToken: string,
  recommendationId: number
): Promise<RecommendationWornResponse> {
  return request<RecommendationWornResponse>(
    `/api/recommendations/${recommendationId}/worn`,
    {
      accessToken,
      method: 'PATCH',
    }
  );
}

export function getRecommendationHistory(
  accessToken: string,
  limit = 20
): Promise<RecommendationResponse[]> {
  return request<RecommendationResponse[]>(`/api/recommendations?limit=${limit}`, {
    accessToken,
  });
}
