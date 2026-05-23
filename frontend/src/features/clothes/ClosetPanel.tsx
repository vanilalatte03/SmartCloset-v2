import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import { createClothing, getClothes } from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import { ColorSwatch, MaterialChip } from '../../components/DisplayTokens';
import type {
  ClothingCategory,
  ClothingColor,
  ClothingMaterial,
  ClothingRequest,
  ClothingResponse,
  ErrorResponse,
} from '../../types/api';
import {
  clothingCategoryLabels,
  clothingCategoryOptions,
  clothingColorMetadata,
  clothingColorOptions,
  clothingMaterialLabels,
  clothingMaterialOptions,
} from '../../utils/displayMappings';

const defaultForm: ClothingRequest = {
  name: '',
  category: 'TOP',
  color: 'GRAY',
  material: 'COTTON',
  minTemperature: 5,
  maxTemperature: 18,
  rainSuitable: false,
};

function validationError(message: string): ErrorResponse {
  return {
    code: 'INVALID_REQUEST',
    message,
    details: [],
  };
}

type ClosetPanelProps = {
  accessToken: string;
  onAuthExpired: () => void;
};

export function ClosetPanel({ accessToken, onAuthExpired }: ClosetPanelProps) {
  const [clothes, setClothes] = useState<ClothingResponse[]>([]);
  const [form, setForm] = useState<ClothingRequest>(defaultForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const loadClothes = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const activeClothes = await getClothes(accessToken);
      setClothes(activeClothes);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setClothes([]);
      setError(toErrorResponse(caught, 'Unable to load active clothes.'));
    } finally {
      setLoading(false);
    }
  }, [accessToken, onAuthExpired]);

  useEffect(() => {
    void loadClothes();
  }, [loadClothes]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setStatus(null);

    const trimmedName = form.name.trim();
    if (!trimmedName) {
      setError(validationError('Clothing name is required.'));
      return;
    }
    if (form.minTemperature > form.maxTemperature) {
      setError(validationError('Minimum temperature must be less than or equal to maximum.'));
      return;
    }

    setSubmitting(true);
    try {
      const created = await createClothing(accessToken, {
        ...form,
        name: trimmedName,
      });
      setStatus(`${created.name} registered.`);
      setForm(defaultForm);
      await loadClothes();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to register clothing.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <article className="panel">
      <h2>Closet</h2>
      <section className="panel-section" aria-label="Active clothes">
        <div className="section-title-row">
          <h3>Active items</h3>
          <button
            className="secondary-button"
            type="button"
            onClick={() => void loadClothes()}
            disabled={loading || submitting}
          >
            Refresh
          </button>
        </div>

        {loading ? (
          <p className="muted">Loading active clothes.</p>
        ) : clothes.length > 0 ? (
          <div className="item-list">
            {clothes.map((item) => (
              <div className="item-row" key={item.id}>
                <div>
                  <strong>{item.name}</strong>
                  <span className="token-row">
                    <span>{clothingCategoryLabels[item.category]}</span>
                    <ColorSwatch color={item.color} />
                    <MaterialChip material={item.material} />
                  </span>
                </div>
                <span className="item-meta">
                  {item.minTemperature}C to {item.maxTemperature}C
                  {item.rainSuitable ? ' - rain suitable' : ''}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">No active clothes loaded.</p>
        )}
      </section>

      <form className="panel-form" onSubmit={handleSubmit}>
        <h3>Register clothing</h3>
        <label className="field wide">
          <span>Name</span>
          <input
            value={form.name}
            maxLength={50}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            placeholder="Gray hoodie"
          />
        </label>
        <div className="field-grid">
          <label className="field">
            <span>Category</span>
            <select
              value={form.category}
              onChange={(event) =>
                setForm({ ...form, category: event.target.value as ClothingCategory })
              }
            >
              {clothingCategoryOptions.map((option) => (
                <option key={option} value={option}>
                  {clothingCategoryLabels[option]}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Color</span>
            <select
              value={form.color}
              onChange={(event) =>
                setForm({ ...form, color: event.target.value as ClothingColor })
              }
            >
              {clothingColorOptions.map((option) => (
                <option key={option} value={option}>
                  {clothingColorMetadata[option].label}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Material</span>
            <select
              value={form.material}
              onChange={(event) =>
                setForm({ ...form, material: event.target.value as ClothingMaterial })
              }
            >
              {clothingMaterialOptions.map((option) => (
                <option key={option} value={option}>
                  {clothingMaterialLabels[option]}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Min C</span>
            <input
              type="number"
              value={form.minTemperature}
              onChange={(event) =>
                setForm({ ...form, minTemperature: Number(event.target.value) })
              }
            />
          </label>
          <label className="field">
            <span>Max C</span>
            <input
              type="number"
              value={form.maxTemperature}
              onChange={(event) =>
                setForm({ ...form, maxTemperature: Number(event.target.value) })
              }
            />
          </label>
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={form.rainSuitable}
              onChange={(event) =>
                setForm({ ...form, rainSuitable: event.target.checked })
              }
            />
            <span>Rain suitable</span>
          </label>
        </div>
        <button className="primary-button" type="submit" disabled={submitting}>
          {submitting ? 'Registering' : 'Register'}
        </button>
      </form>

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}
    </article>
  );
}
