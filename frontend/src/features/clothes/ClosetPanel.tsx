import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import {
  archiveClothing,
  createClothing,
  deleteClothingImage,
  getClothingImageBlob,
  getClothes,
  updateClothing,
  uploadClothingImage,
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
  formatStyleTagLabel,
  recommendationSituationLabels,
  styleTagSuggestionGroups,
} from '../../utils/displayMappings';
import {
  hasStyleTag,
  maxStyleTagLength,
  mergeStyleTags,
  normalizeStyleTags,
  parseStyleTagInput,
  removeStyleTag,
} from '../../utils/styleTags';

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
  styleTags: [],
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

const categoryVisualLabels: Record<ClothingCategory, string> = {
  TOP: '상',
  BOTTOM: '하',
  OUTER: '겉',
};

const maxImageSizeBytes = 5 * 1024 * 1024;
const allowedImageTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);
const allowedImageExtensions = new Set(['jpg', 'jpeg', 'png', 'webp']);

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

function imageValidationError(message: string): ErrorResponse {
  return {
    code: 'INVALID_REQUEST',
    message,
    details: [{ field: 'image', message }],
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
    styleTags: item.styleTags ?? [],
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

function validateImageFile(file: File): ErrorResponse | null {
  if (file.size <= 0) {
    return imageValidationError('비어 있는 이미지는 업로드할 수 없습니다.');
  }

  if (file.size > maxImageSizeBytes) {
    return imageValidationError('이미지는 5MB 이하만 업로드할 수 있습니다.');
  }

  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  if (!allowedImageExtensions.has(extension)) {
    return imageValidationError('jpg, png, webp 형식의 이미지만 업로드할 수 있습니다.');
  }

  if (!allowedImageTypes.has(file.type)) {
    return imageValidationError('jpg, png, webp 형식의 이미지만 업로드할 수 있습니다.');
  }

  if (
    (extension === 'jpg' || extension === 'jpeg') &&
    file.type !== 'image/jpeg'
  ) {
    return imageValidationError('파일 확장자와 이미지 형식이 일치하지 않습니다.');
  }
  if (extension === 'png' && file.type !== 'image/png') {
    return imageValidationError('파일 확장자와 이미지 형식이 일치하지 않습니다.');
  }
  if (extension === 'webp' && file.type !== 'image/webp') {
    return imageValidationError('파일 확장자와 이미지 형식이 일치하지 않습니다.');
  }

  return null;
}

function ClothingThumbnail({
  accessToken,
  item,
  onAuthExpired,
}: {
  accessToken: string;
  item: ClothingResponse;
  onAuthExpired: () => void;
}) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  const fallbackLabel = categoryVisualLabels[item.category];

  useEffect(() => {
    let active = true;
    let nextObjectUrl: string | null = null;

    setObjectUrl(null);
    setFailed(false);

    if (!item.image) {
      return undefined;
    }

    getClothingImageBlob(accessToken, item.image.url)
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
        URL.revokeObjectURL(nextObjectUrl);
      }
    };
  }, [accessToken, item.image, onAuthExpired]);

  if (item.image && objectUrl && !failed) {
    return (
      <div className="closet-thumbnail-frame">
        <img src={objectUrl} alt={`${item.name} 이미지`} className="closet-thumbnail-image" />
      </div>
    );
  }

  return (
    <div className={`closet-thumbnail-frame fallback ${item.category.toLowerCase()}`}>
      <span
        className={`closet-category-visual ${item.category.toLowerCase()}`}
        aria-hidden="true"
      >
        {fallbackLabel}
      </span>
      <ColorSwatch color={item.color} />
    </div>
  );
}

type ClosetPanelProps = {
  accessToken: string;
  initialCategory?: ClothingCategory | null;
  onAuthExpired: () => void;
};

export function ClosetPanel({
  accessToken,
  initialCategory,
  onAuthExpired,
}: ClosetPanelProps) {
  const [clothes, setClothes] = useState<ClothingResponse[]>([]);
  const [form, setForm] = useState<ClothingRequest>(defaultForm);
  const [tagInput, setTagInput] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>('ALL');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [archivingId, setArchivingId] = useState<number | null>(null);
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [deleteImageRequested, setDeleteImageRequested] = useState(false);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const loadClothes = useCallback(async (preserveError = false) => {
    setLoading(true);
    if (!preserveError) {
      setError(null);
    }

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

  useEffect(() => {
    if (!initialCategory) {
      return;
    }

    setEditingId(null);
    setTagInput('');
    setCategoryFilter(initialCategory);
    setForm({
      ...defaultForm,
      category: initialCategory,
    });
  }, [initialCategory]);

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
    setTagInput('');
    setEditingId(null);
    setSelectedImageFile(null);
    setPreviewUrl(null);
    setDeleteImageRequested(false);
    setFileInputKey((current) => current + 1);
  };

  useEffect(() => {
    if (!selectedImageFile) {
      setPreviewUrl(null);
      return undefined;
    }

    const nextPreviewUrl = URL.createObjectURL(selectedImageFile);
    setPreviewUrl(nextPreviewUrl);

    return () => {
      URL.revokeObjectURL(nextPreviewUrl);
    };
  }, [selectedImageFile]);

  const handleImageFileChange = (file: File | null) => {
    setError(null);
    setStatus(null);

    if (!file) {
      setSelectedImageFile(null);
      return;
    }

    const imageError = validateImageFile(file);
    if (imageError) {
      setSelectedImageFile(null);
      setFileInputKey((current) => current + 1);
      setError(imageError);
      return;
    }

    setDeleteImageRequested(false);
    setSelectedImageFile(file);
  };

  const handleRequestImageDelete = () => {
    setError(null);
    setStatus(null);
    setSelectedImageFile(null);
    setDeleteImageRequested(true);
    setFileInputKey((current) => current + 1);
  };

  const clearImageSelection = () => {
    setSelectedImageFile(null);
    setDeleteImageRequested(false);
    setFileInputKey((current) => current + 1);
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
      styleTags: normalizeStyleTags(form.styleTags),
    };

    setSubmitting(true);
    try {
      if (editingId !== null) {
        const updated = await updateClothing(accessToken, editingId, requestBody);
        let imageUpdated = updated;
        try {
          if (deleteImageRequested) {
            imageUpdated = await deleteClothingImage(accessToken, editingId);
          }
          if (selectedImageFile) {
            imageUpdated = await uploadClothingImage(accessToken, editingId, selectedImageFile);
          }
          setStatus(`${imageUpdated.name} 수정이 저장되었습니다.`);
        } catch (caught) {
          if (isUnauthorizedError(caught)) {
            onAuthExpired();
            return;
          }
          setStatus(`${updated.name} 정보는 저장됐지만 이미지 변경에 실패했습니다.`);
          setError(toErrorResponse(caught, '이미지를 변경하지 못했습니다.'));
        }
      } else {
        const created = await createClothing(accessToken, requestBody);
        if (selectedImageFile) {
          try {
            const imageUpdated = await uploadClothingImage(
              accessToken,
              created.id,
              selectedImageFile
            );
            setStatus(`${imageUpdated.name} 등록과 이미지 저장이 완료되었습니다.`);
          } catch (caught) {
            if (isUnauthorizedError(caught)) {
              onAuthExpired();
              return;
            }
            setStatus(`${created.name}은 등록됐지만 이미지 저장에 실패했습니다.`);
            setError(toErrorResponse(caught, '이미지를 저장하지 못했습니다.'));
          }
        } else {
          setStatus(`${created.name} 등록이 완료되었습니다.`);
        }
      }

      resetForm();
      await loadClothes(true);
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
    setTagInput('');
    setForm(toClothingRequest(item));
    setSelectedImageFile(null);
    setDeleteImageRequested(false);
    setFileInputKey((current) => current + 1);
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

  const addStyleTagsToForm = (nextTags: string[], reportEmpty: boolean): boolean => {
    const normalizedNextTags = normalizeStyleTags(nextTags);
    setError(null);
    setStatus(null);

    if (normalizedNextTags.length === 0) {
      if (reportEmpty) {
        setError(validationError('스타일 태그를 입력해주세요.'));
      }
      return false;
    }
    if (normalizedNextTags.some((tag) => tag.length > maxStyleTagLength)) {
      setError(validationError('스타일 태그는 30자 이하로 입력해주세요.'));
      return false;
    }

    setForm((current) => ({
      ...current,
      styleTags: mergeStyleTags(current.styleTags, normalizedNextTags),
    }));
    return true;
  };

  const handleAddTag = () => {
    if (addStyleTagsToForm(parseStyleTagInput(tagInput), true)) {
      setTagInput('');
    }
  };

  const handleToggleSuggestedTag = (tag: string) => {
    setError(null);
    setStatus(null);
    setForm((current) => ({
      ...current,
      styleTags: hasStyleTag(current.styleTags, tag)
        ? removeStyleTag(current.styleTags, tag)
        : mergeStyleTags(current.styleTags, [tag]),
    }));
    setTagInput('');
  };

  const handleRemoveTag = (tag: string) => {
    setError(null);
    setStatus(null);
    setForm((current) => ({
      ...current,
      styleTags: removeStyleTag(current.styleTags, tag),
    }));
  };

  return (
    <article className="panel closet-panel">
      <div className="closet-panel-header">
        <div>
          <h2>옷장</h2>
          <p className="muted closet-panel-copy">
            추천 후보로 쓰이는 활성 옷을 카드로 확인하고 바로 등록합니다.
          </p>
        </div>
        <dl className="metric-list closet-counts" aria-label="활성 옷 수">
          {clothingCategoryOptions.map((category) => (
            <div key={category}>
              <dt>{clothingCategoryLabels[category]}</dt>
              <dd>{activeCategoryCounts[category]}개</dd>
            </div>
          ))}
        </dl>
      </div>

      <div className="closet-layout">
        <section className="closet-card-section" aria-label="활성 옷 카드">
          <div className="section-title-row">
            <div>
              <h3>활성 옷</h3>
              <p className="muted closet-form-note">보관하지 않은 옷만 추천 후보가 됩니다.</p>
            </div>
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
              <div className="closet-card-grid">
                {filteredClothes.map((item) => (
                  <article className="closet-card" key={item.id}>
                    <div className="closet-card-visual-row">
                      <span
                        className={`closet-category-visual ${item.category.toLowerCase()}`}
                        aria-hidden="true"
                      >
                        {categoryVisualLabels[item.category]}
                      </span>
                      <span className="category-pill">{clothingCategoryLabels[item.category]}</span>
                    </div>
                    <ClothingThumbnail
                      accessToken={accessToken}
                      item={item}
                      onAuthExpired={onAuthExpired}
                    />
                    <div className="closet-card-body">
                      <strong className="closet-item-name">{item.name}</strong>
                      <span className="token-row closet-token-row">
                        <ColorSwatch color={item.color} />
                        <MaterialChip material={item.material} />
                      </span>
                      <span className="closet-weather-row">
                        <span className="weather-fit-badge">
                          {item.minTemperature}°C-{item.maxTemperature}°C
                        </span>
                        <span
                          className={
                            item.rainSuitable
                              ? 'weather-fit-badge rain-ready'
                              : 'weather-fit-badge'
                          }
                        >
                          {item.rainSuitable ? '비 오는 날 적합' : '맑은 날 중심'}
                        </span>
                      </span>
                      <span
                        className="tag-list closet-card-tags"
                        aria-label={`${item.name} 스타일 태그`}
                      >
                        {item.styleTags.length > 0 ? (
                          item.styleTags.map((tag) => (
                            <span className="tag-chip readonly" key={tag}>
                              {tag}
                            </span>
                          ))
                        ) : (
                          <span className="muted">스타일 태그 없음</span>
                        )}
                      </span>
                    </div>
                    <div className="closet-item-actions closet-card-actions">
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
                  </article>
                ))}
              </div>
            ) : (
              <p className="muted">선택한 카테고리에 활성 옷이 없어요.</p>
            )
          ) : (
            <p className="muted">첫 추천을 위해 상의, 하의, 아우터를 하나씩 등록해주세요.</p>
          )}
        </section>

        <form className="panel-form closet-form closet-quick-panel" onSubmit={handleSubmit}>
          <div className="section-title-row closet-form-heading">
            <div>
              <h3>{editingItem ? '옷 수정' : '빠른 등록'}</h3>
              <p className="muted closet-form-note">
                {editingItem
                  ? `${editingItem.name} 정보를 전체 수정합니다.`
                  : '추천 준비에 필요한 옷 정보를 한 번에 등록합니다.'}
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

          <section className="closet-image-editor" aria-label="옷 이미지 관리">
            <div className="closet-image-preview">
              {previewUrl ? (
                <img src={previewUrl} alt="선택한 옷 이미지 미리보기" />
              ) : editingItem?.image && !deleteImageRequested ? (
                <ClothingThumbnail
                  accessToken={accessToken}
                  item={editingItem}
                  onAuthExpired={onAuthExpired}
                />
              ) : (
                <div className={`closet-thumbnail-frame fallback ${form.category.toLowerCase()}`}>
                  <span
                    className={`closet-category-visual ${form.category.toLowerCase()}`}
                    aria-hidden="true"
                  >
                    {categoryVisualLabels[form.category]}
                  </span>
                  <ColorSwatch color={form.color} />
                </div>
              )}
            </div>
            <div className="closet-image-controls">
              <label className="field image-file-field" htmlFor="clothing-image-file">
                <span>{editingItem ? '이미지 교체' : '이미지 추가'}</span>
                <input
                  key={fileInputKey}
                  id="clothing-image-file"
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(event) =>
                    handleImageFileChange(event.target.files?.item(0) ?? null)
                  }
                />
              </label>
              <p className="muted closet-image-help">jpg, png, webp / 최대 5MB</p>
              {selectedImageFile ? (
                <div className="closet-image-selection" role="status">
                  <span>{selectedImageFile.name}</span>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={clearImageSelection}
                    disabled={submitting}
                  >
                    선택 해제
                  </button>
                </div>
              ) : null}
              {editingItem?.image || deleteImageRequested ? (
                <button
                  className="secondary-button danger-button"
                  type="button"
                  onClick={handleRequestImageDelete}
                  disabled={submitting || deleteImageRequested}
                >
                  {deleteImageRequested ? '삭제 예정' : '이미지 삭제'}
                </button>
              ) : null}
              {deleteImageRequested ? (
                <p className="muted closet-image-help" role="status">
                  저장하면 이 옷의 이미지가 삭제됩니다.
                </p>
              ) : null}
            </div>
          </section>

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
                  {preset.minTemperature}°C-{preset.maxTemperature}°C
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

          <section className="closet-style-tag-editor" aria-label="옷 스타일 태그">
            <div className="section-title-row">
              <div>
                <h3>스타일 태그</h3>
                <p className="muted closet-form-note">
                  저장 전 {form.styleTags.length}개 태그가 추천 개인화에 사용됩니다.
                </p>
              </div>
            </div>
            <div className="style-tag-suggestions" aria-label="추천 스타일 태그">
              {styleTagSuggestionGroups.map((group) => (
                <div className="style-tag-suggestion-group" key={group.situation}>
                  <span className="style-tag-group-label">
                    {recommendationSituationLabels[group.situation]}
                  </span>
                  <div className="style-tag-suggestion-chips">
                    {group.tags.map((tag) => {
                      const selected = hasStyleTag(form.styleTags, tag);

                      return (
                        <button
                          className={selected ? 'suggestion-chip active' : 'suggestion-chip'}
                          type="button"
                          key={`${group.situation}:${tag}`}
                          aria-pressed={selected}
                          onClick={() => handleToggleSuggestedTag(tag)}
                          disabled={submitting}
                        >
                          {tag}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
            <div className="inline-form tag-form">
              <label className="field">
                <span>태그</span>
                <input
                  value={tagInput}
                  maxLength={30}
                  onChange={(event) => setTagInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault();
                      handleAddTag();
                    }
                    if (event.key === ',') {
                      event.preventDefault();
                      if (tagInput.trim()) {
                        handleAddTag();
                      }
                    }
                  }}
                  placeholder="미니멀, 단정"
                />
              </label>
              <button
                className="secondary-button"
                type="button"
                onClick={handleAddTag}
                disabled={submitting}
              >
                추가
              </button>
            </div>
            <div className="tag-list" aria-label="옷 스타일 태그 목록">
              {form.styleTags.length > 0 ? (
                form.styleTags.map((tag) => (
                  <span className="tag-chip" key={tag}>
                    {formatStyleTagLabel(tag)}
                    <button
                      type="button"
                      aria-label={`${tag} 삭제`}
                      onClick={() => handleRemoveTag(tag)}
                      disabled={submitting}
                    >
                      x
                    </button>
                  </span>
                ))
              ) : (
                <span className="muted">저장된 스타일 태그가 없어요.</span>
              )}
            </div>
          </section>

          <div className="closet-form-actions">
            <button className="primary-button" type="submit" disabled={submitting}>
              {submitting ? '저장 중' : editingItem ? '수정 저장' : '등록하기'}
            </button>
            {editingItem ? (
              <span className="muted">보관 여부는 별도 보관 버튼으로만 변경합니다.</span>
            ) : null}
          </div>
        </form>
      </div>

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}
    </article>
  );
}
