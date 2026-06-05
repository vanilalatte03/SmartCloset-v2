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

export type AuthProvider = 'PASSWORD' | 'GOOGLE';

export type CurrentUserResponse = {
  email: string;
  name: string;
  role: 'USER';
  emailVerified: boolean;
  passwordLoginEnabled: boolean;
  authProviders: AuthProvider[];
  createdAt: string;
  updatedAt: string;
};

export type UpdateCurrentUserRequest = {
  name: string;
};

export type SignupResponse = {
  email: string;
  emailVerificationRequired: boolean;
  message: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  user: CurrentUserResponse;
};

export type LogoutResponse = {
  loggedOut: boolean;
};

export type EmailVerificationRequest = {
  email: string;
};

export type EmailVerificationConfirmRequest = {
  token: string;
};

export type EmailVerificationRequestedResponse = {
  requested: boolean;
};

export type EmailVerificationConfirmResponse = {
  emailVerified: boolean;
};

export type PasswordResetRequest = {
  email: string;
};

export type PasswordResetConfirmRequest = {
  token: string;
  newPassword: string;
};

export type PasswordResetRequestedResponse = {
  requested: boolean;
};

export type PasswordResetConfirmResponse = {
  passwordReset: boolean;
};

export type OAuthProvidersResponse = {
  google: {
    enabled: boolean;
    loginUrl: string | null;
  };
};

export type AccountDeletionRequest = {
  confirmation: 'DELETE';
  password?: string;
};

export type AccountDeletionResponse = {
  deleted: boolean;
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
  styleTags: string[];
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
  updatedAt: string;
};

export type WeatherType = 'SUNNY' | 'CLOUDY' | 'R\u0041INY' | 'SNOWY' | 'WINDY';

export type RecommendationFailureCode =
  | 'NO_TOP_AV\u0041ILABLE'
  | 'NO_BOTTOM_AV\u0041ILABLE'
  | 'OUTER_REQUIRED_BUT_NOT_AV\u0041ILABLE'
  | 'NO_WEATHER_SUITABLE_ITEM'
  | 'INSUFFICIENT_CLOSET_ITEMS';

export type LocationSource = 'MANUAL_SEARCH' | 'BROWSER_GEOLOCATION';

export type LocationOptionResponse = {
  code: string;
  name: string;
  fullName: string;
  region1: string;
  region2: string | null;
  region3: string | null;
  nx: number;
  ny: number;
  latitude: number | null;
  longitude: number | null;
};

export type LocationResolveRequest = {
  latitude: number;
  longitude: number;
};

export type LocationGridResponse = {
  nx: number;
  ny: number;
};

export type LocationResolveResponse = {
  grid: LocationGridResponse;
  nearest: LocationOptionResponse | null;
  candidates: LocationOptionResponse[];
};

export type UserLocationResponse = {
  code: string;
  name: string;
  fullName: string;
  region1: string;
  region2: string | null;
  region3: string | null;
  nx: number;
  ny: number;
  source: LocationSource;
  updatedAt: string;
};

export type UpdateUserLocationRequest = {
  locationCode: string;
  source?: LocationSource;
};

export type UserPreferencesResponse = {
  preferredColors: ClothingColor[];
  preferredMaterials: ClothingMaterial[];
  styleTags: string[];
};

export type UpdateUserPreferencesRequest = UserPreferencesResponse;

export type ForecastPeriod = 'CURRENT' | 'MORNING' | 'AFTERNOON' | 'EVENING';

export type WeatherLocationSnapshotResponse = {
  code: string;
  name: string;
  fullName: string;
  nx: number;
  ny: number;
  source: LocationSource;
};

export type WeatherProvider = 'KMA_VILAGE_FORECAST' | 'STATIC_FALLBACK';

export type WeatherSourceResponse = {
  provider: WeatherProvider;
  kmaUsed: boolean;
  fallbackUsed: boolean;
  baseDate: string | null;
  baseTime: string | null;
  forecastDate: string | null;
  forecastTime: string | null;
};

export type WeatherResponse = {
  temperature: number;
  weatherType: WeatherType;
  rainy: boolean;
  windy: boolean;
  location: WeatherLocationSnapshotResponse;
  source: WeatherSourceResponse;
};

export type OutfitItemResponse = {
  id: number;
  name: string;
  category: ClothingCategory;
  color: ClothingColor;
  material: ClothingMaterial;
  styleTags: string[];
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

export type RecommendationSituation = 'WORK' | 'CASUAL' | 'WORKOUT' | 'DATE' | 'FORMAL';

export type RecommendationFeedbackSentiment = 'LIKED' | 'DISLIKED';

export type RecommendationThermalFeedback = 'TOO_COLD' | 'TOO_HOT';

export type RecommendationRequest = {
  situation?: RecommendationSituation;
  forecastPeriod?: ForecastPeriod;
};

export type RecommendationFeedbackRequest = {
  sentiment?: RecommendationFeedbackSentiment | null;
  thermal?: RecommendationThermalFeedback | null;
};

export type RecommendationFeedbackStateResponse = {
  sentiment: RecommendationFeedbackSentiment | null;
  thermal: RecommendationThermalFeedback | null;
  updatedAt: string;
};

export type RecommendationFeedbackResponse = {
  recommendationId: number;
  feedback: RecommendationFeedbackStateResponse | null;
};

export type RecommendationResponse = {
  recommendationId: number;
  situation: RecommendationSituation;
  forecastPeriod: ForecastPeriod;
  weather: WeatherResponse;
  outfit: RecommendationOutfitResponse;
  score: RecommendationScoreResponse;
  reasons: string[];
  worn: boolean;
  wornAt: string | null;
  feedback: RecommendationFeedbackStateResponse | null;
  createdAt: string;
};

export type RecommendationWornResponse = {
  recommendationId: number;
  worn: boolean;
  wornAt: string;
};
