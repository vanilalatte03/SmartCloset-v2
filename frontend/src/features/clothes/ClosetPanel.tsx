import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import {
  archiveClothing,
  createClothing,
  getClothes,
  updateClothing,
} from '../../api/smartClosetApi';
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

type CategoryFilter = 'ALL' | ClothingCategory;

type TemperaturePreset = {
  id: string;
  label: string;
  minTemperature: number;
  maxTemperature: number;
  rainSuitable: boolean;
};

const defaultForm: ClothingRequest = {
  name: '',
  category: 'TOP',
  color: 'GRAY',
  material: 'COTTON',
  minTemperature: 5,
  maxTemperature: 18,
  rainSuitable: false,
};

const categoryFilterOptions: Array<{
  value: CategoryFilter;
  label: string;
}> = [
  { value: 'ALL', label: '전체' },
  { value: 'TOP', label: clothingCategoryLabels.TOP },
  { value: 'BOTTOM', label: clothingCategoryLabels.BOTTOM },
  { value: 'OUTER', label: clothingCategoryLabels.OUTER },
];

const temperaturePresets: TemperaturePreset[] = [
  {
    id: 'deep-winter',
    label: '한겨울',
    minTemperature: -10,
    maxTemperature: 5,
    rainSuitable: false,
  },
  {
    id: 'cool-day',
    label: '쌀쌀한 날',
    minTemperature: 0,
    maxTemperature: 12,
    rainSuitable: false,
  },
  {
    id: 'between-seasons',
    label: '간절기',
    minTemperature: 8,
    maxTemperature: 20,
    rainSuitable: false,
  },
  {
    id: 'warm-day',
    label: '따뜻한 날',
    minTemperature: 17,
    maxTemperature: 28,
    rainSuitable: false,
  },
  {
    id: 'rainy-day',
    label: '비 오는 날',
    minTemperature: 5,
    maxTemperature: 24,
    rainSuitable: true,
  },
];

function validationError(message: string): ErrorResponse {
  return {
    code: 'INVALID_REQUEST',
    message,
    details: [],
  };
}

function toClothingRequest(item: ClothingResponse): ClothingRequest {
  return {
    name: item.name,
    category: item.category,
    color: item.color,
    material: item.material,
    minTemperature: item.minTemperature,
    maxTemperature: item.maxTemperature,
    rainSuitable: item.rainSuitable,
  };
}

function getActiveCategoryCounts(clothes: ClothingResponse[]): Record<ClothingCategory, number> {
  return clothes.reduce<Record<ClothingCategory, number>>(
    (counts, item) => {
      if (!item.archived) {
        counts[item.category] += 1;
      }
      return counts;
    },
    {
      TOP: 0,
      BOTTOM: 0,
      OUTER: 0,
    }
  );
}

function matchesPreset(form: ClothingRequest, preset: TemperaturePreset): boolean {
  return (
    form.minTemperature === preset.minTemperature &&
    form.maxTemperature === preset.maxTemperature &&
    form.rainSuitable === preset.rainSuitable
  );
}

type ClosetPanelProps = {
  accessToken: string;
  onAuthExpired: () => void;
};

export function ClosetPanel({ accessToken, onAuthExpired }: ClosetPanelProps) {
  const [clothes, setClothes] = useState<ClothingResponse[]>([]);
  const [form, setForm] = useState<ClothingRequest>(defaultForm);
  const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>('ALL');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [archivingId, setArchivingId] = useState<number | null>(null);
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
      setError(toErrorResponse(caught, '활성 옷 목록을 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  }, [accessToken, onAuthExpired]);

  useEffect(() => {
    void loadClothes();
  }, [loadClothes]);

  const activeClothes = useMemo(
    () => clothes.filter((item) => !item.archived),
    [clothes]
  );
  const activeCategoryCounts = useMemo(
    () => getActiveCategoryCounts(activeClothes),
    [activeClothes]
  );
  const filteredClothes = useMemo(() => {
    if (categoryFilter === 'ALL') {
      return activeClothes;
    }

    return activeClothes.filter((item) => item.category === categoryFilter);
  }, [activeClothes, categoryFilter]);
  const editingItem = editingId
    ? activeClothes.find((item) => item.id === editingId) ?? null
    : null;

  const resetForm = () => {
    setForm(defaultForm);
    setEditingId(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setStatus(null);

    const trimmedName = form.name.trim();
    if (!trimmedName) {
      setError(validationError('옷 이름을 입력해주세요.'));
      return;
    }
    if (form.minTemperature > form.maxTemperature) {
      setError(validationError('최저 기온은 최고 기온보다 낮거나 같아야 합니다.'));
      return;
    }

    const requestBody: ClothingRequest = {
      ...form,
      name: trimmedName,
    };

    setSubmitting(true);
    try {
      if (editingId !== null) {
        const updated = await updateClothing(accessToken, editingId, requestBody);
        setStatus(`${updated.name} 수정이 저장되었습니다.`);
      } else {
        const created = await createClothing(accessToken, requestBody);
        setStatus(`${created.name} 등록이 완료되었습니다.`);
      }

      resetForm();
      await loadClothes();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, '옷 정보를 저장하지 못했습니다.'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (item: ClothingResponse) => {
    setError(null);
    setStatus(null);
    setEditingId(item.id);
    setForm(toClothingRequest(item));
  };

  const handleArchive = async (item: ClothingResponse) => {
    setError(null);
    setStatus(null);
    setArchivingId(item.id);

    try {
      await archiveClothing(accessToken, item.id);
      setClothes((current) => current.filter((candidate) => candidate.id !== item.id));
      if (editingId === item.id) {
        resetForm();
      }
      setStatus(`${item.name}을 보관했습니다.`);
      await loadClothes();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, '옷을 보관하지 못했습니다.'));
    } finally {
      setArchivingId(null);
    }
  };

  const applyPreset = (preset: TemperaturePreset) => {
    setForm((current) => ({
      ...current,
      minTemperature: preset.minTemperature,
      maxTemperature: preset.maxTemperature,
      rainSuitable: preset.rainSuitable,
    }));
  };

  return (
    <article className="panel closet-panel">
      <h2>옷장</h2>

      <dl className="metric-list closet-counts" aria-label="활성 옷 수">
        {clothingCategoryOptions.map((category) => (
          <div key={category}>
            <dt>{clothingCategoryLabels[category]}</dt>
            <dd>{activeCategoryCounts[category]}개</dd>
          </div>
        ))}
      </dl>

      <section className="panel-section" aria-label="활성 옷 목록">
        <div className="section-title-row">
          <h3>활성 옷 목록</h3>
          <button
            className="secondary-button"
            type="button"
            onClick={() => void loadClothes()}
            disabled={loading || submitting || archivingId !== null}
          >
            새로고침
          </button>
        </div>

        <div className="category-filter" role="group" aria-label="카테고리 필터">
          {categoryFilterOptions.map((option) => (
            <button
              className={categoryFilter === option.value ? 'filter-chip active' : 'filter-chip'}
              type="button"
              key={option.value}
              aria-pressed={categoryFilter === option.value}
              onClick={() => setCategoryFilter(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>

        {loading ? (
          <p className="muted">활성 옷을 확인하고 있어요.</p>
        ) : activeClothes.length > 0 ? (
          filteredClothes.length > 0 ? (
            <div className="item-list closet-list">
              {filteredClothes.map((item) => (
                <div className="item-row closet-item-row" key={item.id}>
                  <div className="closet-item-main">
                    <strong className="closet-item-name">{item.name}</strong>
                    <span className="token-row closet-token-row">
                      <span className="category-pill">{clothingCategoryLabels[item.category]}</span>
                      <ColorSwatch color={item.color} />
                      <MaterialChip material={item.material} />
                    </span>
                    <span className="closet-item-detail">
                      {item.minTemperature}C부터 {item.maxTemperature}C까지
                      {item.rainSuitable ? ' · 비 오는 날 적합' : ' · 비 적합성 없음'}
                    </span>
                  </div>
                  <div className="closet-item-actions">
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => handleEdit(item)}
                      disabled={submitting || archivingId !== null}
                    >
                      수정
                    </button>
                    <button
                      className="secondary-button danger-button"
                      type="button"
                      onClick={() => void handleArchive(item)}
                      disabled={submitting || archivingId !== null}
                    >
                      {archivingId === item.id ? '보관 중' : '보관'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">선택한 카테고리에 활성 옷이 없어요.</p>
          )
        ) : (
          <p className="muted">첫 추천을 위해 상의, 하의, 아우터를 하나씩 등록해주세요.</p>
        )}
      </section>

      <form className="panel-form closet-form" onSubmit={handleSubmit}>
        <div className="section-title-row closet-form-heading">
          <div>
            <h3>{editingItem ? '옷 수정' : '빠른 등록'}</h3>
            <p className="muted closet-form-note">
              {editingItem
                ? `${editingItem.name} 정보를 전체 수정합니다.`
                : '추천 준비에 필요한 옷을 ClothingRequest 계약 그대로 등록합니다.'}
            </p>
          </div>
          {editingItem ? (
            <button
              className="secondary-button"
              type="button"
              onClick={resetForm}
              disabled={submitting}
            >
              수정 취소
            </button>
          ) : null}
        </div>

        <label className="field wide">
          <span>옷 이름</span>
          <input
            value={form.name}
            maxLength={50}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            placeholder="그레이 후드"
          />
        </label>

        <div className="preset-list" role="group" aria-label="계절과 기온 프리셋">
          {temperaturePresets.map((preset) => (
            <button
              className={matchesPreset(form, preset) ? 'preset-button active' : 'preset-button'}
              type="button"
              key={preset.id}
              aria-pressed={matchesPreset(form, preset)}
              onClick={() => applyPreset(preset)}
            >
              <strong>{preset.label}</strong>
              <span>
                {preset.minTemperature}C..{preset.maxTemperature}C
                {preset.rainSuitable ? ' · 비 가능' : ''}
              </span>
            </button>
          ))}
        </div>

        <div className="field-grid closet-form-grid">
          <label className="field">
            <span>카테고리</span>
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
            <span>색상</span>
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
            <span>소재</span>
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
            <span>최저 기온</span>
            <input
              type="number"
              value={form.minTemperature}
              onChange={(event) =>
                setForm({ ...form, minTemperature: Number(event.target.value) })
              }
            />
          </label>
          <label className="field">
            <span>최고 기온</span>
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
            <span>비 오는 날 입기 좋음</span>
          </label>
        </div>

        <div className="closet-form-actions">
          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? '저장 중' : editingItem ? '수정 저장' : '등록하기'}
          </button>
          {editingItem ? (
            <span className="muted">보관 여부는 별도 보관 버튼으로만 변경합니다.</span>
          ) : null}
        </div>
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
