import type {
  ClothingCategory,
  ClothingColor,
  ClothingMaterial,
  RecommendationFailureCode,
  RecommendationFeedbackSentiment,
  RecommendationSituation,
  RecommendationThermalFeedback,
  WeatherType,
} from '../types/api';

export const clothingCategoryOptions = [
  'TOP',
  'BOTTOM',
  'OUTER',
] as const satisfies readonly ClothingCategory[];

export const clothingCategoryLabels: Record<ClothingCategory, string> = {
  TOP: '상의',
  BOTTOM: '하의',
  OUTER: '아우터',
};

export const clothingColorOptions = [
  'BLACK',
  'WHITE',
  'GRAY',
  'NAVY',
  'BLUE',
  'BROWN',
  'BEIGE',
  'RED',
  'GREEN',
  'YELLOW',
  'UNKNOWN',
] as const satisfies readonly ClothingColor[];

export type ClothingColorMetadata = {
  label: string;
  swatch: string;
  borderColor: string;
};

export const clothingColorMetadata: Record<ClothingColor, ClothingColorMetadata> = {
  BLACK: {
    label: '블랙',
    swatch: '#1f2933',
    borderColor: '#1f2933',
  },
  WHITE: {
    label: '화이트',
    swatch: '#ffffff',
    borderColor: '#c9d1dc',
  },
  GRAY: {
    label: '그레이',
    swatch: '#8a94a6',
    borderColor: '#667085',
  },
  NAVY: {
    label: '네이비',
    swatch: '#243b63',
    borderColor: '#243b63',
  },
  BLUE: {
    label: '블루',
    swatch: '#2563eb',
    borderColor: '#1d4ed8',
  },
  BROWN: {
    label: '브라운',
    swatch: '#8b5e34',
    borderColor: '#704822',
  },
  BEIGE: {
    label: '베이지',
    swatch: '#d8c3a5',
    borderColor: '#b59c7b',
  },
  RED: {
    label: '레드',
    swatch: '#dc2626',
    borderColor: '#b91c1c',
  },
  GREEN: {
    label: '그린',
    swatch: '#16a34a',
    borderColor: '#15803d',
  },
  YELLOW: {
    label: '옐로우',
    swatch: '#facc15',
    borderColor: '#ca8a04',
  },
  UNKNOWN: {
    label: '기타',
    swatch: '#eef2f6',
    borderColor: '#b8c0cc',
  },
};

export const clothingMaterialOptions = [
  'COTTON',
  'DENIM',
  'KNIT',
  'WOOL',
  'POLYESTER',
  'NYLON',
  'UNKNOWN',
] as const satisfies readonly ClothingMaterial[];

export const clothingMaterialLabels: Record<ClothingMaterial, string> = {
  COTTON: '면',
  DENIM: '데님',
  KNIT: '니트',
  WOOL: '울',
  POLYESTER: '폴리에스터',
  NYLON: '나일론',
  UNKNOWN: '기타',
};

export const weatherTypeLabels: Record<WeatherType, string> = {
  SUNNY: '맑음',
  CLOUDY: '흐림',
  ['R\u0041INY']: '비',
  SNOWY: '눈',
  WINDY: '바람 강함',
};

export type RecommendationFailureCta = {
  message: string;
  ctaLabel: string;
  targetView: 'closet';
  category?: ClothingCategory;
};

export const recommendationFailureCtas: Record<
  RecommendationFailureCode,
  RecommendationFailureCta
> = {
  ['NO_TOP_AV\u0041ILABLE']: {
    message: '현재 날씨에 맞는 상의가 부족해요.',
    ctaLabel: '상의 등록하기',
    targetView: 'closet',
    category: 'TOP',
  },
  ['NO_BOTTOM_AV\u0041ILABLE']: {
    message: '현재 날씨에 맞는 하의가 부족해요.',
    ctaLabel: '하의 등록하기',
    targetView: 'closet',
    category: 'BOTTOM',
  },
  ['OUTER_REQUIRED_BUT_NOT_AV\u0041ILABLE']: {
    message: '오늘은 아우터가 필요한 날씨예요.',
    ctaLabel: '아우터 등록하기',
    targetView: 'closet',
    category: 'OUTER',
  },
  NO_WEATHER_SUITABLE_ITEM: {
    message: '현재 기온에 맞는 옷이 부족해요.',
    ctaLabel: '옷장 확인하기',
    targetView: 'closet',
  },
  INSUFFICIENT_CLOSET_ITEMS: {
    message: '추천을 만들려면 옷을 더 등록해야 해요.',
    ctaLabel: '빠른 등록하기',
    targetView: 'closet',
  },
};

export const styleTagLabels = {
  title: '스타일 태그',
  inputLabel: '태그',
  placeholder: '미니멀',
  addCta: '추가',
  empty: '저장된 스타일 태그가 없어요.',
} as const;

export const recommendationSituationOptions = [
  'WORK',
  'CASUAL',
  'WORKOUT',
  'DATE',
  'FORMAL',
] as const satisfies readonly RecommendationSituation[];

export const recommendationSituationLabels: Record<RecommendationSituation, string> = {
  WORK: '출근',
  CASUAL: '캐주얼',
  WORKOUT: '운동',
  DATE: '데이트',
  FORMAL: '격식',
};

export const recommendationFeedbackSentimentLabels: Record<
  RecommendationFeedbackSentiment,
  string
> = {
  LIKED: '마음에 들어요',
  DISLIKED: '별로예요',
};

export const recommendationThermalFeedbackLabels: Record<
  RecommendationThermalFeedback,
  string
> = {
  TOO_COLD: '추웠어요',
  TOO_HOT: '더웠어요',
};

export function getClothingCategoryLabel(category: ClothingCategory): string {
  return clothingCategoryLabels[category];
}

export function getClothingColorLabel(color: ClothingColor): string {
  return clothingColorMetadata[color].label;
}

export function getClothingMaterialLabel(material: ClothingMaterial): string {
  return clothingMaterialLabels[material];
}

export function getWeatherTypeLabel(weatherType: WeatherType): string {
  return weatherTypeLabels[weatherType];
}

export function isRecommendationFailureCode(
  code: string
): code is RecommendationFailureCode {
  return code in recommendationFailureCtas;
}

export function getRecommendationFailureCta(
  code: string
): RecommendationFailureCta | null {
  return isRecommendationFailureCode(code) ? recommendationFailureCtas[code] : null;
}

export function formatStyleTagLabel(tag: string): string {
  return tag.trim();
}
