import type { ClothingColor, ClothingMaterial, WeatherResponse, WeatherType } from '../types/api';
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
