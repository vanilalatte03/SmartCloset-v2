export type ApiResponse<T> = {
  data: T;
};

export type ErrorResponse = {
  code: string;
  message: string;
  details: Array<{
    field: string;
    message: string;
  }>;
};

export type ClothingCategory = 'TOP' | 'BOTTOM' | 'OUTER';

export type ClothingColor =
  | 'BLACK'
  | 'WHITE'
  | 'GRAY'
  | 'NAVY'
  | 'BLUE'
  | 'BROWN'
  | 'BEIGE'
  | 'RED'
  | 'GREEN'
  | 'YELLOW'
  | 'UNKNOWN';

export type ClothingMaterial =
  | 'COTTON'
  | 'DENIM'
  | 'KNIT'
  | 'WOOL'
  | 'POLYESTER'
  | 'NYLON'
  | 'UNKNOWN';

export type ClothingRequest = {
  name: string;
  category: ClothingCategory;
  color: ClothingColor;
  material: ClothingMaterial;
  minTemperature: number;
  maxTemperature: number;
  rainSuitable: boolean;
};

export type ClothingResponse = ClothingRequest & {
  id: number;
  userId: number;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ClothingArchiveResponse = {
  id: number;
  archived: boolean;
};

export type LocationOptionResponse = {
  code: string;
  name: string;
  nx: number;
  ny: number;
};

export type UserLocationResponse = {
  userId: number;
  code: string;
  name: string;
  nx: number;
  ny: number;
  updatedAt: string;
};

export type UpdateUserLocationRequest = {
  locationCode: string;
};

export type WeatherResponse = {
  temperature: number;
  weatherType: 'SUNNY' | 'CLOUDY' | 'RAINY' | 'SNOWY' | 'WINDY';
  rainy: boolean;
  windy: boolean;
};

export type OutfitItemResponse = {
  id: number;
  name: string;
  category: ClothingCategory;
  color: ClothingColor;
  material: ClothingMaterial;
};

export type RecommendationOutfitResponse = {
  top: OutfitItemResponse;
  bottom: OutfitItemResponse;
  outer: OutfitItemResponse | null;
};

export type RecommendationScoreResponse = {
  totalScore: number;
  weatherScore: number;
  colorScore: number;
  wearHistoryScore: number;
  recommendationHistoryScore: number;
  diversityScore: number;
};

export type RecommendationResponse = {
  recommendationId: number;
  weather: WeatherResponse;
  outfit: RecommendationOutfitResponse;
  score: RecommendationScoreResponse;
  reasons: string[];
  worn: boolean;
  createdAt: string;
};

export type RecommendationWornResponse = {
  recommendationId: number;
  worn: boolean;
  wornAt: string;
};
