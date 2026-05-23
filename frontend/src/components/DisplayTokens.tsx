import type { ClothingColor, ClothingMaterial, WeatherType } from '../types/api';
import {
  clothingColorMetadata,
  clothingMaterialLabels,
  weatherTypeLabels,
} from '../utils/displayMappings';

type ColorSwatchProps = {
  color: ClothingColor;
  showLabel?: boolean;
};

export function ColorSwatch({ color, showLabel = true }: ColorSwatchProps) {
  const colorMetadata = clothingColorMetadata[color];

  return (
    <span className="color-token">
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

type MaterialChipProps = {
  material: ClothingMaterial;
};

export function MaterialChip({ material }: MaterialChipProps) {
  return <span className="material-chip">{clothingMaterialLabels[material]}</span>;
}

type WeatherLabelProps = {
  weatherType: WeatherType;
};

export function WeatherLabel({ weatherType }: WeatherLabelProps) {
  return <span className="weather-token">{weatherTypeLabels[weatherType]}</span>;
}
