import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { toErrorResponse } from '../../api/errorHelpers';
import { createClothing, getClothes } from '../../api/smartClosetApi';
import type {
  ClothingCategory,
  ClothingColor,
  ClothingMaterial,
  ClothingRequest,
  ClothingResponse,
  ErrorResponse,
} from '../../types/api';

const categoryOptions: ClothingCategory[] = ['TOP', 'BOTTOM', 'OUTER'];
const colorOptions: ClothingColor[] = [
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
];
const materialOptions: ClothingMaterial[] = [
  'COTTON',
  'DENIM',
  'KNIT',
  'WOOL',
  'POLYESTER',
  'NYLON',
  'UNKNOWN',
];

const defaultForm: ClothingRequest = {
  name: '',
  category: 'TOP',
  color: 'GRAY',
  material: 'COTTON',
  minTemperature: 5,
  maxTemperature: 18,
  rainSuitable: false,
};

type ClosetPanelProps = {
  userId: number;
};

function validationError(message: string): ErrorResponse {
  return {
    code: 'INVALID_REQUEST',
    message,
    details: [],
  };
}

export function ClosetPanel({ userId }: ClosetPanelProps) {
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
      const activeClothes = await getClothes(userId);
      setClothes(activeClothes);
    } catch (caught) {
      setClothes([]);
      setError(toErrorResponse(caught, 'Unable to load active clothes.'));
    } finally {
      setLoading(false);
    }
  }, [userId]);

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
      const created = await createClothing(userId, {
        ...form,
        name: trimmedName,
      });
      setStatus(`${created.name} registered.`);
      setForm(defaultForm);
      await loadClothes();
    } catch (caught) {
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
                  <span>
                    {item.category} - {item.color} - {item.material}
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
              {categoryOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
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
              {colorOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
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
              {materialOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
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

      {error ? (
        <p className="panel-error" role="status">
          <strong>{error.code}</strong> {error.message}
        </p>
      ) : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}
    </article>
  );
}
