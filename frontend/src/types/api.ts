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

export type LoginRequest = {
  email: string;
  password: string;
};

export type SignupRequest = LoginRequest & {
  name: string;
};

export type CurrentUserResponse = {
  email: string;
  name: string;
  role: 'USER';
  createdAt: string;
  updatedAt: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  user: CurrentUserResponse;
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

export type ClothingImageResponse = {
  url: string;
  contentType: 'image/jpeg' | 'image/png' | 'image/webp';
  sizeBytes: number;
  uploadedAt: string;
};

export type ClothingResponse = ClothingRequest & {
  id: number;
  archived: boolean;
  image: ClothingImageResponse | null;
  createdAt: string;
  updatedAt: string;
};

export type ClothingArchiveResponse = {
  id: number;
  archived: boolean;
};

export type WeatherType = 'SUNNY' | 'CLOUDY' | 'R\u0041INY' | 'SNOWY' | 'WINDY';

export type RecommendationFailureCode =
  | 'NO_TOP_AV\u0041ILABLE'
  | 'NO_BOTTOM_AV\u0041ILABLE'
  | 'OUTER_REQUIRED_BUT_NOT_AV\u0041ILABLE'
  | 'NO_WEATHER_SUITABLE_ITEM'
  | 'INSUFFICIENT_CLOSET_ITEMS';

export type LocationOptionResponse = {
  code: string;
  name: string;
  nx: number;
  ny: number;
};

export type UserLocationResponse = {
  code: string;
  name: string;
  nx: number;
  ny: number;
  updatedAt: string;
};

export type UpdateUserLocationRequest = {
  locationCode: string;
};

export type UserPreferencesResponse = {
  preferredColors: ClothingColor[];
  preferredMaterials: ClothingMaterial[];
  styleTags: string[];
};

export type UpdateUserPreferencesRequest = UserPreferencesResponse;

export type WeatherResponse = {
  temperature: number;
  weatherType: WeatherType;
  rainy: boolean;
  windy: boolean;
};

export type OutfitItemResponse = {
  id: number;
  name: string;
  category: ClothingCategory;
  color: ClothingColor;
  material: ClothingMaterial;
  image: ClothingImageResponse | null;
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
  preferenceScore: number;
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
