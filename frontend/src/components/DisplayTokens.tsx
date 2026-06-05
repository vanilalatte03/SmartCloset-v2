import { useEffect, useState } from 'react';
import { isUnauthorizedError } from '../api/errorHelpers';
import { getClothingImageBlob } from '../api/smartClosetApi';
import type {
  ClothingCategory,
  ClothingColor,
  ClothingImageResponse,
  ClothingMaterial,
  WeatherResponse,
  WeatherType,
} from '../types/api';
import {
  clothingColorMetadata,
  clothingMaterialLabels,
  weatherTypeLabels,
} from '../utils/displayMappings';

type ColorSwatchProps = {
  color: ClothingColor;
  className?: string;
  showLabel?: boolean;
  size?: 'small' | 'large';
};

export function ColorSwatch({
  color,
  className,
  showLabel = true,
  size = 'small',
}: ColorSwatchProps) {
  const colorMetadata = clothingColorMetadata[color];
  const classNames = ['color-token', `color-token-${size}`, className]
    .filter(Boolean)
    .join(' ');

  return (
    <span className={classNames}>
      <span
        className="color-swatch"
        style={{
          backgroundColor: colorMetadata.swatch,
          borderColor: colorMetadata.borderColor,
        }}
        aria-hidden="true"
      />
      {showLabel ? <span>{colorMetadata.label}</span> : null}
    </span>
  );
}

type ColorSwatchPlaceholderProps = {
  label?: string;
};

export function ColorSwatchPlaceholder({ label = '색상 대기' }: ColorSwatchPlaceholderProps) {
  return (
    <span className="color-token color-token-placeholder">
      <span className="color-swatch placeholder" aria-hidden="true" />
      <span>{label}</span>
    </span>
  );
}

type MaterialChipProps = {
  material: ClothingMaterial;
};

export function MaterialChip({ material }: MaterialChipProps) {
  return <span className="material-chip">{clothingMaterialLabels[material]}</span>;
}

type MaterialChipPlaceholderProps = {
  label?: string;
};

export function MaterialChipPlaceholder({
  label = '소재 대기',
}: MaterialChipPlaceholderProps) {
  return <span className="material-chip muted-token">{label}</span>;
}

type WeatherLabelProps = {
  weatherType: WeatherType;
};

export function WeatherLabel({ weatherType }: WeatherLabelProps) {
  return <span className="weather-token">{weatherTypeLabels[weatherType]}</span>;
}

type WeatherBadgeProps = {
  weather: WeatherResponse;
};

export function WeatherBadge({ weather }: WeatherBadgeProps) {
  const rainLabel = weather.rainy ? '비 가능' : '비 없음';
  const windLabel = weather.windy ? '바람 강함' : '바람 잔잔';

  return (
    <span className="weather-badge">
      <strong>{weather.temperature}°C</strong>
      <WeatherLabel weatherType={weather.weatherType} />
      <span>{rainLabel}</span>
      <span>{windLabel}</span>
    </span>
  );
}

type AuthenticatedClothingThumbnailProps = {
  accessToken: string;
  image: ClothingImageResponse | null;
  alt: string;
  fallbackLabel: string;
  category?: ClothingCategory;
  color?: ClothingColor;
  className?: string;
  onAuthExpired: () => void;
};

export function AuthenticatedClothingThumbnail({
  accessToken,
  image,
  alt,
  fallbackLabel,
  category,
  color,
  className,
  onAuthExpired,
}: AuthenticatedClothingThumbnailProps) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  const classNames = [
    'clothing-thumbnail-frame',
    image && objectUrl && !failed ? 'loaded' : 'fallback',
    category ? category.toLowerCase() : null,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  useEffect(() => {
    let active = true;
    let nextObjectUrl: string | null = null;

    setObjectUrl(null);
    setFailed(false);

    if (!image) {
      return undefined;
    }

    // 이미지 API도 보호 API라 access token을 붙여 blob으로 받은 뒤 임시 object URL로 렌더링한다.
    getClothingImageBlob(accessToken, image.url)
      .then((blob) => {
        if (!active) {
          return;
        }
        nextObjectUrl = URL.createObjectURL(blob);
        setObjectUrl(nextObjectUrl);
      })
      .catch((caught) => {
        if (!active) {
          return;
        }
        if (isUnauthorizedError(caught)) {
          onAuthExpired();
          return;
        }
        setFailed(true);
      });

    return () => {
      active = false;
      if (nextObjectUrl) {
        // object URL은 자동 해제되지 않으므로 다른 이미지로 바뀌거나 unmount될 때 정리한다.
        URL.revokeObjectURL(nextObjectUrl);
      }
    };
  }, [accessToken, image, onAuthExpired]);

  if (image && objectUrl && !failed) {
    return (
      <div className={classNames}>
        <img src={objectUrl} alt={alt} className="clothing-thumbnail-image" />
      </div>
    );
  }

  return (
    <div className={classNames}>
      <span
        className={category ? `closet-category-visual ${category.toLowerCase()}` : 'slot-glyph'}
        aria-hidden="true"
      >
        {fallbackLabel}
      </span>
      {color ? <ColorSwatch color={color} showLabel={false} /> : null}
    </div>
  );
}
