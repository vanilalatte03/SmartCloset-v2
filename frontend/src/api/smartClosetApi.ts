import { apiBaseUrl, request } from './client';
import type {
  ClothingRequest,
  ClothingResponse,
  LocationOptionResponse,
  RecommendationResponse,
  RecommendationWornResponse,
  UpdateUserLocationRequest,
  UserLocationResponse,
} from '../types/api';

function userIdParam(userId: number): string {
  return new URLSearchParams({ userId: String(userId) }).toString();
}

export function getApiBaseUrl(): string {
  return apiBaseUrl;
}

export function getLocations(keyword?: string): Promise<LocationOptionResponse[]> {
  const params = new URLSearchParams();
  if (keyword && keyword.trim()) {
    params.set('keyword', keyword.trim());
  }

  const query = params.toString();
  return request<LocationOptionResponse[]>(`/api/locations${query ? `?${query}` : ''}`);
}

export function getUserLocation(userId: number): Promise<UserLocationResponse> {
  return request<UserLocationResponse>(`/api/users/location?${userIdParam(userId)}`);
}

export function updateUserLocation(
  userId: number,
  body: UpdateUserLocationRequest
): Promise<UserLocationResponse> {
  return request<UserLocationResponse>(`/api/users/location?${userIdParam(userId)}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export function getClothes(userId: number): Promise<ClothingResponse[]> {
  return request<ClothingResponse[]>(`/api/clothes?${userIdParam(userId)}`);
}

export function createClothing(
  userId: number,
  body: ClothingRequest
): Promise<ClothingResponse> {
  return request<ClothingResponse>(`/api/clothes?${userIdParam(userId)}`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function createRecommendation(userId: number): Promise<RecommendationResponse> {
  return request<RecommendationResponse>(`/api/recommendations?${userIdParam(userId)}`, {
    method: 'POST',
  });
}

export function markRecommendationWorn(
  userId: number,
  recommendationId: number
): Promise<RecommendationWornResponse> {
  return request<RecommendationWornResponse>(
    `/api/recommendations/${recommendationId}/worn?${userIdParam(userId)}`,
    {
      method: 'PATCH',
    }
  );
}
