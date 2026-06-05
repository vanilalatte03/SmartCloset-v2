import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ClipboardEvent, DragEvent, FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import {
  analyzeClothingImage,
  archiveClothing,
  createClothing,
  deleteClothingImage,
  getArchivedClothes,
  getClothingImageBlob,
  getClothes,
  unarchiveClothing,
  updateClothing,
  uploadClothingImage,
} from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import { ColorSwatch, MaterialChip } from '../../components/DisplayTokens';
import type {
  ClothingAnalysisField,
  ClothingAnalysisResponse,
  ClothingCategory,
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
  getDisplayStyleTagEntries,
  getDisplayStyleTags,
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
import { getEulParticle } from '../../utils/koreanParticles';

type CategoryFilter = 'ALL' | ClothingCategory | 'HAS_IMAGE' | 'HAS_TAG';
type ClosetView = 'active' | 'archived';

type TemperaturePreset = {
  id: string;
  label: string;
  minTemperature: number;
  maxTemperature: number;
  rainSuitable: boolean;
};

const analysisFields: ClothingAnalysisField[] = [
  'name',
  'category',
  'color',
  'material',
  'minTemperature',
  'maxTemperature',
  'rainSuitable',
  'styleTags',
];

const analysisFieldLabels: Record<ClothingAnalysisField, string> = {
  name: '옷 이름',
  category: '카테고리',
  color: '색상',
  material: '소재',
  minTemperature: '최저 기온',
  maxTemperature: '최고 기온',
  rainSuitable: '비 적합성',
  styleTags: '스타일 태그',
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
  { value: 'HAS_IMAGE', label: '이미지 있음' },
  { value: 'HAS_TAG', label: '태그 있음' },
];

const maxImageSizeBytes = 5 * 1024 * 1024;
const allowedImageTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);
const allowedImageExtensions = new Set(['jpg', 'jpeg', 'png', 'webp']);
const imageExtensionByType: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
};

const temperaturePresets: TemperaturePreset[] = [
  {
    id: 'deep-winter',
    label: '추운 날',
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
    label: '더운 날',
    minTemperature: 17,
    maxTemperature: 35,
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

function sortClothesById(first: ClothingResponse, second: ClothingResponse): number {
  return first.id - second.id;
}

function matchesPreset(form: ClothingRequest, preset: TemperaturePreset): boolean {
  return (
    form.minTemperature === preset.minTemperature &&
    form.maxTemperature === preset.maxTemperature &&
    form.rainSuitable === preset.rainSuitable
  );
}

function getFileExtension(fileName: string): string {
  if (!fileName.includes('.')) {
    return '';
  }
  return fileName.split('.').pop()?.toLowerCase() ?? '';
}

function ensureNamedImageFile(file: File): File {
  const currentExtension = getFileExtension(file.name);
  if (allowedImageExtensions.has(currentExtension)) {
    return file;
  }
  if (currentExtension) {
    return file;
  }

  const extension = imageExtensionByType[file.type];
  if (!extension) {
    return file;
  }

  // 클립보드 이미지처럼 filename이 없는 File도 서버 확장자 검증을 통과할 수 있게 이름을 붙인다.
  return new File([file], `smartcloset-image.${extension}`, {
    type: file.type,
    lastModified: file.lastModified,
  });
}

function getFirstFile(files: FileList): File | null {
  for (let index = 0; index < files.length; index += 1) {
    const file = files.item(index);
    if (file) {
      return file;
    }
  }
  return null;
}

function getFirstClipboardImageFile(dataTransfer: DataTransfer): File | null {
  for (let index = 0; index < dataTransfer.items.length; index += 1) {
    const item = dataTransfer.items[index];
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      return item.getAsFile();
    }
  }

  return getFirstFile(dataTransfer.files);
}

function hasFileDrag(dataTransfer: DataTransfer): boolean {
  for (let index = 0; index < dataTransfer.types.length; index += 1) {
    if (dataTransfer.types[index] === 'Files') {
      return true;
    }
  }
  return false;
}

function validateImageFile(file: File): ErrorResponse | null {
  if (file.size <= 0) {
    return imageValidationError('비어 있는 이미지는 업로드할 수 없습니다.');
  }

  if (file.size > maxImageSizeBytes) {
    return imageValidationError('이미지는 5MB 이하만 업로드할 수 있습니다.');
  }

  const extension = getFileExtension(file.name);
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

function getFileFingerprint(file: File): string {
  return [file.name, file.size, file.lastModified, file.type].join(':');
}

function isAnalysisField(value: string): value is ClothingAnalysisField {
  return analysisFields.includes(value as ClothingAnalysisField);
}

function getReviewRequiredFields(response: ClothingAnalysisResponse): ClothingAnalysisField[] {
  const fields = new Set<ClothingAnalysisField>();

  response.reviewRequiredFields.forEach((field) => {
    if (isAnalysisField(field)) {
      fields.add(field);
    }
  });

  analysisFields.forEach((field) => {
    const confidence = response.fieldConfidence[field];
    if (typeof confidence === 'number' && confidence < response.lowConfidenceThreshold) {
      fields.add(field);
    }
  });

  return Array.from(fields);
}

function scrollToTopOnMobile() {
  if (
    typeof window === 'undefined' ||
    !window.matchMedia('(max-width: 920px)').matches
  ) {
    return;
  }

  window.requestAnimationFrame(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });
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

  useEffect(() => {
    let active = true;
    let nextObjectUrl: string | null = null;

    setObjectUrl(null);
    setFailed(false);

    if (!item.image) {
      return undefined;
    }

    // 보호 이미지는 Authorization header가 필요하므로 blob으로 받은 뒤 object URL을 만든다.
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
        // object URL은 브라우저 메모리를 점유하므로 thumbnail 변경/언마운트 시 반드시 해제한다.
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

  return <ClothingImageFallback category={item.category} color={item.color} label={`${item.name} 이미지 없음`} />;
}

function ClothingImageFallback({
  category,
  color,
  label,
}: {
  category: ClothingCategory;
  color: ClothingResponse['color'];
  label: string;
}) {
  const colorMetadata = clothingColorMetadata[color];

  return (
    <div
      className={`closet-thumbnail-frame fallback ${category.toLowerCase()}`}
      role="img"
      aria-label={label}
    >
      <span
        className="closet-placeholder-color-dot"
        style={{
          backgroundColor: colorMetadata.swatch,
          borderColor: colorMetadata.borderColor,
        }}
        aria-hidden="true"
      />
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
  const [archivedClothes, setArchivedClothes] = useState<ClothingResponse[]>([]);
  const [form, setForm] = useState<ClothingRequest>(defaultForm);
  const [tagInput, setTagInput] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>('ALL');
  const [closetView, setClosetView] = useState<ClosetView>('active');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [archivingId, setArchivingId] = useState<number | null>(null);
  const [restoringId, setRestoringId] = useState<number | null>(null);
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [imageDropActive, setImageDropActive] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [deleteImageRequested, setDeleteImageRequested] = useState(false);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [analysisLoading, setAnalysisLoading] = useState(false);
  const [analysisMessage, setAnalysisMessage] = useState<string | null>(null);
  const [analysisCache, setAnalysisCache] = useState<Record<string, ClothingAnalysisResponse>>({});
  const [reviewRequiredFields, setReviewRequiredFields] = useState<ClothingAnalysisField[]>([]);
  const [pendingReviewSubmit, setPendingReviewSubmit] = useState<ClothingRequest | null>(null);
  const [mobileEditorOpen, setMobileEditorOpen] = useState(false);
  const [archivedLoaded, setArchivedLoaded] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const selectedImageFingerprintRef = useRef<string | null>(null);
  const updateSelectedImageFile = useCallback((file: File | null) => {
    selectedImageFingerprintRef.current = file ? getFileFingerprint(file) : null;
    setSelectedImageFile(file);
  }, []);

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

  const loadArchivedClothes = useCallback(async (preserveError = false) => {
    setLoading(true);
    if (!preserveError) {
      setError(null);
    }

    try {
      const nextArchivedClothes = await getArchivedClothes(accessToken);
      setArchivedClothes(nextArchivedClothes);
      setArchivedLoaded(true);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setArchivedClothes([]);
      setArchivedLoaded(false);
      setError(toErrorResponse(caught, '보관함을 불러오지 못했습니다.'));
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
    setClosetView('active');
    setCategoryFilter(initialCategory);
    setForm({
      ...defaultForm,
      category: initialCategory,
    });
    setMobileEditorOpen(true);
    scrollToTopOnMobile();
  }, [initialCategory]);

  const activeClothes = useMemo(
    () => clothes.filter((item) => !item.archived),
    [clothes]
  );
  const visibleClothes = closetView === 'archived' ? archivedClothes : activeClothes;
  const activeCategoryCounts = useMemo(
    () => getActiveCategoryCounts(activeClothes),
    [activeClothes]
  );
  const filteredClothes = useMemo(() => {
    if (categoryFilter === 'ALL') {
      return visibleClothes;
    }
    if (categoryFilter === 'HAS_IMAGE') {
      return visibleClothes.filter((item) => item.image !== null);
    }
    if (categoryFilter === 'HAS_TAG') {
      return visibleClothes.filter((item) => item.styleTags.length > 0);
    }

    return visibleClothes.filter((item) => item.category === categoryFilter);
  }, [categoryFilter, visibleClothes]);
  const editingItem = editingId
    ? activeClothes.find((item) => item.id === editingId) ?? null
    : null;

  const isReviewRequired = (field: ClothingAnalysisField) =>
    reviewRequiredFields.includes(field);

  const clearAnalysisState = () => {
    setAnalysisLoading(false);
    setAnalysisMessage(null);
    setReviewRequiredFields([]);
    setPendingReviewSubmit(null);
  };

  const markFieldReviewed = (field: ClothingAnalysisField) => {
    setReviewRequiredFields((current) => current.filter((candidate) => candidate !== field));
  };

  const updateFormField = <Field extends keyof ClothingRequest>(
    field: Field,
    value: ClothingRequest[Field]
  ) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
    if (isAnalysisField(field)) {
      markFieldReviewed(field);
    }
  };

  const renderReviewMarker = (field: ClothingAnalysisField) =>
    isReviewRequired(field) ? (
      <>
        <span className="analysis-review-badge">확인 필요</span>
        <button
          className="analysis-confirm-button"
          type="button"
          onClick={() => markFieldReviewed(field)}
          disabled={submitting}
        >
          확인
        </button>
      </>
    ) : null;

  const resetForm = () => {
    setForm(defaultForm);
    setTagInput('');
    setEditingId(null);
    updateSelectedImageFile(null);
    setPreviewUrl(null);
    setDeleteImageRequested(false);
    setFileInputKey((current) => current + 1);
    clearAnalysisState();
  };

  const openCreateForm = () => {
    setError(null);
    setStatus(null);
    resetForm();
    setClosetView('active');
    setMobileEditorOpen(true);
    scrollToTopOnMobile();
  };

  const closeMobileEditor = () => {
    resetForm();
    setMobileEditorOpen(false);
    scrollToTopOnMobile();
  };

  const handleToggleArchiveView = () => {
    setError(null);
    setStatus(null);

    if (closetView === 'archived') {
      setClosetView('active');
      scrollToTopOnMobile();
      return;
    }

    resetForm();
    setClosetView('archived');
    setMobileEditorOpen(false);
    scrollToTopOnMobile();
    if (!archivedLoaded) {
      // 보관함은 처음 열 때만 별도 API를 호출해 기본 옷장 화면의 초기 로드를 가볍게 유지한다.
      void loadArchivedClothes();
    }
  };

  const handleRefresh = () => {
    if (closetView === 'archived') {
      void loadArchivedClothes();
      return;
    }

    void loadClothes();
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
    if (analysisLoading) {
      setFileInputKey((current) => current + 1);
      return;
    }

    setError(null);
    setStatus(null);
    setAnalysisMessage(null);
    setReviewRequiredFields([]);
    setPendingReviewSubmit(null);

    if (!file) {
      updateSelectedImageFile(null);
      return;
    }

    const imageFile = ensureNamedImageFile(file);
    const imageError = validateImageFile(imageFile);
    if (imageError) {
      updateSelectedImageFile(null);
      setFileInputKey((current) => current + 1);
      setError(imageError);
      return;
    }

    setDeleteImageRequested(false);
    updateSelectedImageFile(imageFile);
  };

  const handleImageDragEnter = (event: DragEvent<HTMLElement>) => {
    if (!hasFileDrag(event.dataTransfer)) {
      return;
    }
    event.preventDefault();
    if (analysisLoading) {
      event.dataTransfer.dropEffect = 'none';
      setImageDropActive(false);
      return;
    }
    setImageDropActive(true);
  };

  const handleImageDragOver = (event: DragEvent<HTMLElement>) => {
    if (!hasFileDrag(event.dataTransfer)) {
      return;
    }
    event.preventDefault();
    if (analysisLoading) {
      event.dataTransfer.dropEffect = 'none';
      setImageDropActive(false);
      return;
    }
    event.dataTransfer.dropEffect = 'copy';
    setImageDropActive(true);
  };

  const handleImageDragLeave = (event: DragEvent<HTMLElement>) => {
    if (event.currentTarget.contains(event.relatedTarget as Node | null)) {
      return;
    }
    setImageDropActive(false);
  };

  const handleImageDrop = (event: DragEvent<HTMLElement>) => {
    if (!hasFileDrag(event.dataTransfer)) {
      return;
    }
    event.preventDefault();
    setImageDropActive(false);
    if (analysisLoading) {
      return;
    }
    handleImageFileChange(getFirstFile(event.dataTransfer.files));
  };

  const handleImagePaste = (event: ClipboardEvent<HTMLElement>) => {
    const imageFile = getFirstClipboardImageFile(event.clipboardData);
    if (!imageFile) {
      return;
    }
    event.preventDefault();
    if (analysisLoading) {
      return;
    }
    handleImageFileChange(imageFile);
  };

  const handleRequestImageDelete = () => {
    if (analysisLoading) {
      return;
    }

    setError(null);
    setStatus(null);
    updateSelectedImageFile(null);
    setDeleteImageRequested(true);
    setFileInputKey((current) => current + 1);
    setAnalysisMessage(null);
    setReviewRequiredFields([]);
    setPendingReviewSubmit(null);
  };

  const clearImageSelection = () => {
    if (analysisLoading) {
      return;
    }

    updateSelectedImageFile(null);
    setDeleteImageRequested(false);
    setFileInputKey((current) => current + 1);
    setAnalysisMessage(null);
    setReviewRequiredFields([]);
    setPendingReviewSubmit(null);
  };

  const applyAnalysisResponse = (response: ClothingAnalysisResponse, fromCache: boolean) => {
    if (!response.analyzable || !response.suggestion) {
      setReviewRequiredFields([]);
      setAnalysisMessage('옷으로 보기 어려운 이미지입니다. 직접 입력해 저장할 수 있습니다.');
      return;
    }

    const suggestion = response.suggestion;
    setForm({
      name: suggestion.name,
      category: suggestion.category,
      color: suggestion.color,
      material: suggestion.material,
      minTemperature: suggestion.minTemperature,
      maxTemperature: suggestion.maxTemperature,
      rainSuitable: suggestion.rainSuitable,
      styleTags: normalizeStyleTags(suggestion.styleTags),
    });

    const nextReviewRequiredFields = getReviewRequiredFields(response);
    setReviewRequiredFields(nextReviewRequiredFields);
    setAnalysisMessage(
      nextReviewRequiredFields.length > 0
        ? `${fromCache ? '이전 분석 결과를 다시 적용했습니다.' : 'AI 후보를 적용했습니다.'} 확인 필요 항목 ${nextReviewRequiredFields.length}개를 확인해주세요.`
        : `${fromCache ? '이전 분석 결과를 다시 적용했습니다.' : 'AI 후보를 적용했습니다.'} 바로 저장할 수 있습니다.`
    );
  };

  const handleAnalyzeImage = async () => {
    setError(null);
    setStatus(null);
    setAnalysisMessage(null);
    setPendingReviewSubmit(null);

    if (!selectedImageFile) {
      setAnalysisMessage('이미지를 선택한 뒤 AI 후보 체크를 실행해주세요.');
      return;
    }

    const fingerprint = getFileFingerprint(selectedImageFile);
    const cachedResponse = analysisCache[fingerprint];
    if (cachedResponse) {
      applyAnalysisResponse(cachedResponse, true);
      return;
    }

    setAnalysisLoading(true);
    try {
      const response = await analyzeClothingImage(accessToken, selectedImageFile);
      if (selectedImageFingerprintRef.current !== fingerprint) {
        return;
      }
      setAnalysisCache((current) => ({
        ...current,
        [fingerprint]: response,
      }));
      applyAnalysisResponse(response, false);
    } catch (caught) {
      if (selectedImageFingerprintRef.current !== fingerprint) {
        return;
      }
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setReviewRequiredFields([]);
      setAnalysisMessage('AI 후보 체크를 사용할 수 없습니다. 직접 입력해 저장할 수 있습니다.');
      setError(toErrorResponse(caught, 'AI 후보 체크에 실패했습니다.'));
    } finally {
      setAnalysisLoading(false);
    }
  };

  const persistClothing = async (requestBody: ClothingRequest) => {
    setSubmitting(true);
    try {
      if (editingId !== null) {
        const updated = await updateClothing(accessToken, editingId, requestBody);
        let imageUpdated = updated;
        try {
          // 옷 기본 정보 저장과 이미지 변경은 별도 API라, 이미지 실패 시에도 텍스트 수정 결과를 보존한다.
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
            // 새 옷은 먼저 id를 만든 뒤 그 id로 이미지 API를 호출해야 한다.
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
      setMobileEditorOpen(false);
      scrollToTopOnMobile();
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

  const buildValidatedRequestBody = (): ClothingRequest | null => {
    const trimmedName = form.name.trim();
    if (!trimmedName) {
      setError(validationError('옷 이름을 입력해주세요.'));
      return null;
    }
    if (form.minTemperature > form.maxTemperature) {
      setError(validationError('최저 기온은 최고 기온보다 낮거나 같아야 합니다.'));
      return null;
    }

    return {
      ...form,
      name: trimmedName,
      styleTags: normalizeStyleTags(form.styleTags),
    };
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setStatus(null);
    setPendingReviewSubmit(null);

    const requestBody = buildValidatedRequestBody();
    if (!requestBody) {
      return;
    }

    if (reviewRequiredFields.length > 0) {
      setPendingReviewSubmit(requestBody);
      return;
    }

    await persistClothing(requestBody);
  };

  const handleConfirmReviewSubmit = async () => {
    if (!pendingReviewSubmit) {
      return;
    }
    const requestBody = pendingReviewSubmit;
    setPendingReviewSubmit(null);
    setReviewRequiredFields([]);
    await persistClothing(requestBody);
  };

  const handleEdit = (item: ClothingResponse) => {
    setError(null);
    setStatus(null);
    clearAnalysisState();
    setEditingId(item.id);
    setTagInput('');
    setForm(toClothingRequest(item));
    updateSelectedImageFile(null);
    setDeleteImageRequested(false);
    setFileInputKey((current) => current + 1);
    setMobileEditorOpen(true);
    scrollToTopOnMobile();
  };

  const handleArchive = async (item: ClothingResponse) => {
    setError(null);
    setStatus(null);
    setArchivingId(item.id);

    try {
      await archiveClothing(accessToken, item.id);
      // UI에서는 즉시 목록을 옮기고, 이후 loadClothes로 서버 상태를 다시 맞춘다.
      setClothes((current) => current.filter((candidate) => candidate.id !== item.id));
      if (archivedLoaded) {
        setArchivedClothes((current) =>
          [...current.filter((candidate) => candidate.id !== item.id), { ...item, archived: true }]
            .sort(sortClothesById)
        );
      }
      if (editingId === item.id) {
        resetForm();
        setMobileEditorOpen(false);
        scrollToTopOnMobile();
      }
      setStatus(`${item.name}${getEulParticle(item.name)} 보관했습니다.`);
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

  const handleRestore = async (item: ClothingResponse) => {
    setError(null);
    setStatus(null);
    setRestoringId(item.id);

    try {
      await unarchiveClothing(accessToken, item.id);
      const restoredItem = { ...item, archived: false };
      setArchivedClothes((current) => current.filter((candidate) => candidate.id !== item.id));
      setClothes((current) =>
        [...current.filter((candidate) => candidate.id !== item.id), restoredItem].sort(sortClothesById)
      );
      setStatus(`${item.name}${getEulParticle(item.name)} 다시 꺼냈습니다.`);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, '옷을 다시 꺼내지 못했습니다.'));
    } finally {
      setRestoringId(null);
    }
  };

  const applyPreset = (preset: TemperaturePreset) => {
    setForm((current) => ({
      ...current,
      minTemperature: preset.minTemperature,
      maxTemperature: preset.maxTemperature,
      rainSuitable: preset.rainSuitable,
    }));
    setReviewRequiredFields((current) =>
      current.filter(
        (field) =>
          field !== 'minTemperature' && field !== 'maxTemperature' && field !== 'rainSuitable'
      )
    );
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
    markFieldReviewed('styleTags');
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
    markFieldReviewed('styleTags');
    setTagInput('');
  };

  const handleRemoveDisplayedTags = (sourceTags: string[]) => {
    setError(null);
    setStatus(null);
    setForm((current) => ({
      ...current,
      styleTags: sourceTags.reduce(
        (remainingTags, tag) => removeStyleTag(remainingTags, tag),
        current.styleTags
      ),
    }));
    markFieldReviewed('styleTags');
  };

  const formDisplayStyleTags = getDisplayStyleTags(form.styleTags);
  const formDisplayStyleTagEntries = getDisplayStyleTagEntries(form.styleTags);

  return (
    <article
      className={
        mobileEditorOpen ? 'panel closet-panel mobile-editor-open' : 'panel closet-panel'
      }
    >
      <div className="closet-panel-header closet-wardrobe-hero">
        <div className="closet-hero-copy">
          <p className="eyebrow">옷장 상태</p>
          <h2>추천에 쓸 수 있는 옷 {activeClothes.length}개</h2>
          <p className="closet-panel-copy">
            카테고리와 온도 범위를 정리해 오늘 추천 후보를 더 빠르게 준비합니다.
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
        <section
          className="closet-card-section"
          aria-label={closetView === 'archived' ? '보관한 옷 카드' : '활성 옷 카드'}
        >
          <div className="section-title-row">
            <div>
              <p className="eyebrow">
                {closetView === 'archived' ? '보관함' : '내 옷 리스트'}
              </p>
              <h3>{closetView === 'archived' ? '보관한 옷' : '최근 추가한 옷'}</h3>
              <p className="muted closet-form-note">
                {closetView === 'archived'
                  ? '다시 꺼낸 옷은 추천 후보로 돌아옵니다.'
                  : '보관하지 않은 옷만 추천 후보가 됩니다.'}
              </p>
            </div>
            <div className="closet-list-actions">
              <button
                className="primary-button closet-mobile-add-button"
                type="button"
                onClick={openCreateForm}
                disabled={submitting || archivingId !== null || restoringId !== null}
              >
                옷 추가
              </button>
              <button
                className={
                  closetView === 'archived' ? 'secondary-button active' : 'secondary-button'
                }
                type="button"
                aria-pressed={closetView === 'archived'}
                onClick={handleToggleArchiveView}
                disabled={loading || submitting || archivingId !== null || restoringId !== null}
              >
                보관함
              </button>
              <button
                className="secondary-button"
                type="button"
                onClick={handleRefresh}
                disabled={loading || submitting || archivingId !== null || restoringId !== null}
              >
                새로고침
              </button>
            </div>
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
            <p className="muted">
              {closetView === 'archived'
                ? '보관함을 확인하고 있어요.'
                : '활성 옷을 확인하고 있어요.'}
            </p>
          ) : visibleClothes.length > 0 ? (
            filteredClothes.length > 0 ? (
              <div className="closet-card-grid">
                {filteredClothes.map((item) => {
                  const displayStyleTags = getDisplayStyleTags(item.styleTags);

                  return (
                    <article className="closet-card" key={item.id}>
                      <ClothingThumbnail
                        accessToken={accessToken}
                        item={item}
                        onAuthExpired={onAuthExpired}
                      />
                      <div className="closet-card-body">
                        <strong className="closet-item-name">{item.name}</strong>
                        <span className="closet-item-detail">
                          {clothingCategoryLabels[item.category]} · {clothingMaterialLabels[item.material]} ·{' '}
                          {item.minTemperature}°C-{item.maxTemperature}°C
                        </span>
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
                          {displayStyleTags.length > 0 ? (
                            displayStyleTags.map((tag) => (
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
                        {closetView === 'archived' ? (
                          <button
                            className="secondary-button"
                            type="button"
                            onClick={() => void handleRestore(item)}
                            disabled={submitting || archivingId !== null || restoringId !== null}
                          >
                            {restoringId === item.id ? '꺼내는 중' : '다시 꺼내기'}
                          </button>
                        ) : (
                          <>
                            <button
                              className="secondary-button"
                              type="button"
                              onClick={() => handleEdit(item)}
                              disabled={submitting || archivingId !== null || restoringId !== null}
                            >
                              수정
                            </button>
                            <button
                              className="secondary-button danger-button"
                              type="button"
                              onClick={() => void handleArchive(item)}
                              disabled={submitting || archivingId !== null || restoringId !== null}
                            >
                              {archivingId === item.id ? '보관 중' : '보관'}
                            </button>
                          </>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            ) : (
              <p className="muted">
                {closetView === 'archived'
                  ? '선택한 카테고리에 보관한 옷이 없어요.'
                  : '선택한 카테고리에 활성 옷이 없어요.'}
              </p>
            )
          ) : (
            <p className="muted">
              {closetView === 'archived'
                ? '보관한 옷이 없어요.'
                : '첫 추천을 위해 상의, 하의, 아우터를 하나씩 등록해주세요.'}
            </p>
          )}
        </section>

        <form className="panel-form closet-form closet-quick-panel" onSubmit={handleSubmit}>
          <div className="section-title-row closet-form-heading">
            <div>
              <p className="eyebrow">상세 정보</p>
              <h3>{editingItem ? '옷 정보 수정' : '새 옷 등록'}</h3>
              <p className="muted closet-form-note">
                {editingItem
                  ? '선택한 옷 정보를 전체 수정합니다.'
                  : '추천 준비에 필요한 옷 정보를 한 번에 등록합니다.'}
              </p>
            </div>
            <div className="closet-form-heading-actions">
              <button
                className="secondary-button closet-mobile-editor-close"
                type="button"
                onClick={closeMobileEditor}
                disabled={submitting}
              >
                {editingItem ? '수정 취소' : '목록으로'}
              </button>
              {editingItem ? (
                <button
                  className="secondary-button closet-desktop-edit-cancel"
                  type="button"
                  onClick={resetForm}
                  disabled={submitting}
                >
                  수정 취소
                </button>
              ) : null}
            </div>
          </div>

          <div className="closet-form-overview">
            <section
              className={imageDropActive ? 'closet-image-editor drop-active' : 'closet-image-editor'}
              aria-label="옷 이미지 관리"
              onDragEnter={handleImageDragEnter}
              onDragLeave={handleImageDragLeave}
              onDragOver={handleImageDragOver}
              onDrop={handleImageDrop}
              onPaste={handleImagePaste}
              tabIndex={0}
            >
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
                  <ClothingImageFallback
                    category={form.category}
                    color={form.color}
                    label="선택한 옷 이미지 없음"
                  />
                )}
              </div>
              <div className="closet-image-controls">
                <p className="closet-image-drop-hint">
                  {imageDropActive
                    ? '여기에 놓으면 이미지가 추가됩니다.'
                    : '이미지를 드래그하거나 붙여넣어 추가할 수 있습니다.'}
                </p>
                <label className="field image-file-field" htmlFor="clothing-image-file">
                  <span>{editingItem ? '이미지 교체' : '이미지 추가'}</span>
                  <input
                    key={fileInputKey}
                    id="clothing-image-file"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    disabled={analysisLoading || submitting}
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
                      disabled={analysisLoading || submitting}
                    >
                      선택 해제
                    </button>
                  </div>
                ) : null}
                <div className="analysis-assist-row">
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => void handleAnalyzeImage()}
                    disabled={!selectedImageFile || analysisLoading || submitting}
                  >
                    {analysisLoading ? '확인 중' : 'AI 후보 체크'}
                  </button>
                  {analysisMessage ? (
                    <p className="analysis-assist-message" role="status">
                      {analysisMessage}
                    </p>
                  ) : (
                    <p className="muted closet-image-help">
                      선택한 이미지가 있을 때만 수동으로 실행됩니다.
                    </p>
                  )}
                </div>
                {editingItem?.image || deleteImageRequested ? (
                  <button
                    className="secondary-button danger-button"
                    type="button"
                    onClick={handleRequestImageDelete}
                    disabled={analysisLoading || submitting || deleteImageRequested}
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

            <aside className="closet-live-preview" aria-label="등록 미리보기">
              <span className="eyebrow">등록 미리보기</span>
              <strong>{form.name.trim() || '이름을 입력하세요'}</strong>
              <span>
                {clothingCategoryLabels[form.category]} · {clothingMaterialLabels[form.material]} ·{' '}
                {form.minTemperature}°C-{form.maxTemperature}°C
                {form.rainSuitable ? ' · 비 오는 날 가능' : ''}
              </span>
              <div className="token-row closet-token-row">
                <ColorSwatch color={form.color} />
                <MaterialChip material={form.material} />
              </div>
              <div className="tag-list closet-card-tags">
                {formDisplayStyleTags.length > 0 ? (
                  formDisplayStyleTags.slice(0, 5).map((tag) => (
                    <span className="tag-chip readonly" key={tag}>
                      {tag}
                    </span>
                  ))
                ) : (
                  <span className="muted">태그를 추가하면 추천 개인화에 반영됩니다.</span>
                )}
              </div>
            </aside>
          </div>

          <div className={isReviewRequired('name') ? 'field wide analysis-field review-required' : 'field wide analysis-field'}>
            <div className="field-label-row">
              <label htmlFor="clothing-name">옷 이름</label>
              {renderReviewMarker('name')}
            </div>
            <input
              id="clothing-name"
              value={form.name}
              maxLength={50}
              onChange={(event) => updateFormField('name', event.target.value)}
              placeholder="그레이 후드"
            />
          </div>

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
            <section className={isReviewRequired('category') ? 'closet-control-card analysis-field review-required' : 'closet-control-card analysis-field'}>
              <div className="field-label-row">
                <span className="label">카테고리</span>
                {renderReviewMarker('category')}
              </div>
              <div className="closet-option-grid compact" role="group" aria-label="카테고리">
                {clothingCategoryOptions.map((option) => (
                  <button
                    className={
                      form.category === option ? 'closet-category-button active' : 'closet-category-button'
                    }
                    type="button"
                    key={option}
                    aria-pressed={form.category === option}
                    onClick={() => updateFormField('category', option)}
                  >
                    {clothingCategoryLabels[option]}
                  </button>
                ))}
              </div>
            </section>
            <section className={isReviewRequired('color') ? 'closet-control-card color-control-card analysis-field review-required' : 'closet-control-card color-control-card analysis-field'}>
              <div className="field-label-row">
                <span className="label">색상</span>
                {renderReviewMarker('color')}
              </div>
              <div className="closet-option-grid color-grid" role="group" aria-label="색상">
                {clothingColorOptions.map((option) => (
                  <button
                    className={form.color === option ? 'closet-color-button active' : 'closet-color-button'}
                    type="button"
                    key={option}
                    aria-pressed={form.color === option}
                    onClick={() => updateFormField('color', option)}
                  >
                    <ColorSwatch color={option} />
                  </button>
                ))}
              </div>
            </section>
            <section className={isReviewRequired('material') ? 'closet-control-card material-control-card analysis-field review-required' : 'closet-control-card material-control-card analysis-field'}>
              <div className="field-label-row">
                <span className="label">소재</span>
                {renderReviewMarker('material')}
              </div>
              <div className="closet-option-grid material-grid" role="group" aria-label="소재">
                {clothingMaterialOptions.map((option) => (
                  <button
                    className={
                      form.material === option ? 'closet-material-button active' : 'closet-material-button'
                    }
                    type="button"
                    key={option}
                    aria-pressed={form.material === option}
                    onClick={() => updateFormField('material', option)}
                  >
                    <MaterialChip material={option} />
                  </button>
                ))}
              </div>
            </section>
            <div className={isReviewRequired('minTemperature') ? 'field analysis-field review-required' : 'field analysis-field'}>
              <div className="field-label-row">
                <label htmlFor="clothing-min-temperature">최저 기온</label>
                {renderReviewMarker('minTemperature')}
              </div>
              <input
                id="clothing-min-temperature"
                type="number"
                value={form.minTemperature}
                onChange={(event) =>
                  updateFormField('minTemperature', Number(event.target.value))
                }
              />
            </div>
            <div className={isReviewRequired('maxTemperature') ? 'field analysis-field review-required' : 'field analysis-field'}>
              <div className="field-label-row">
                <label htmlFor="clothing-max-temperature">최고 기온</label>
                {renderReviewMarker('maxTemperature')}
              </div>
              <input
                id="clothing-max-temperature"
                type="number"
                value={form.maxTemperature}
                onChange={(event) =>
                  updateFormField('maxTemperature', Number(event.target.value))
                }
              />
            </div>
            <div className={isReviewRequired('rainSuitable') ? 'checkbox-field analysis-field review-required' : 'checkbox-field analysis-field'}>
              <input
                id="clothing-rain-suitable"
                type="checkbox"
                checked={form.rainSuitable}
                onChange={(event) =>
                  updateFormField('rainSuitable', event.target.checked)
                }
              />
              <label htmlFor="clothing-rain-suitable">비 오는 날 입기 좋음</label>
              {renderReviewMarker('rainSuitable')}
            </div>
          </div>

          <section
            className={isReviewRequired('styleTags') ? 'closet-style-tag-editor analysis-field review-required' : 'closet-style-tag-editor analysis-field'}
            aria-label="옷 스타일 태그"
          >
            <div className="section-title-row">
              <div>
                <h3>스타일 태그</h3>
                <p className="muted closet-form-note">
                  저장 전 {formDisplayStyleTags.length}개 태그가 추천 개인화에 사용됩니다.
                </p>
              </div>
              <div className="field-label-row">{renderReviewMarker('styleTags')}</div>
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
              {formDisplayStyleTagEntries.length > 0 ? (
                formDisplayStyleTagEntries.map((entry) => (
                  <span className="tag-chip" key={entry.label}>
                    {entry.label}
                    <button
                      type="button"
                      aria-label={`${entry.label} 삭제`}
                      onClick={() => handleRemoveDisplayedTags(entry.sourceTags)}
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
      {pendingReviewSubmit ? (
        <div
          className="analysis-confirm-modal-backdrop"
          role="presentation"
          onClick={() => setPendingReviewSubmit(null)}
        >
          <section
            className="analysis-confirm-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="analysis-confirm-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="analysis-confirm-modal-heading">
              <p className="eyebrow">저장 전 확인</p>
              <h2 id="analysis-confirm-title">아직 확인 필요 항목이 있습니다</h2>
            </div>
            <p className="account-delete-copy">
              {reviewRequiredFields
                .map((field) => analysisFieldLabels[field])
                .join(', ')}
              을(를) 확인하지 않았습니다. 현재 값으로 저장할 수 있습니다.
            </p>
            <div className="analysis-review-list" aria-label="확인 필요 항목">
              {reviewRequiredFields.map((field) => (
                <span className="analysis-review-badge" key={field}>
                  {analysisFieldLabels[field]}
                </span>
              ))}
            </div>
            <div className="account-delete-modal-actions">
              <button
                className="secondary-button"
                type="button"
                onClick={() => setPendingReviewSubmit(null)}
                disabled={submitting}
              >
                더 확인하기
              </button>
              <button
                className="primary-button"
                type="button"
                onClick={() => void handleConfirmReviewSubmit()}
                disabled={submitting}
              >
                현재 값으로 저장
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </article>
  );
}
