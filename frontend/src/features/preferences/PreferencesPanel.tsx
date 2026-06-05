import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import { getUserPreferences, updateUserPreferences } from '../../api/smartClosetApi';
import { ColorSwatch, MaterialChip } from '../../components/DisplayTokens';
import type {
  ClothingColor,
  ClothingMaterial,
  ErrorResponse,
  UserPreferencesResponse,
} from '../../types/api';
import {
  clothingColorOptions,
  clothingColorMetadata,
  clothingMaterialOptions,
  clothingMaterialLabels,
  getDisplayStyleTagEntries,
  getDisplayStyleTags,
  recommendationSituationLabels,
  styleTagLabels,
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

const emptyPreferences: UserPreferencesResponse = {
  preferredColors: [],
  preferredMaterials: [],
  styleTags: [],
};

function validationError(message: string): ErrorResponse {
  return {
    code: 'INVALID_REQUEST',
    message,
    details: [],
  };
}

function PreferenceErrorMessage({ error }: { error: ErrorResponse }) {
  return (
    <div className="panel-error" role="status">
      <strong>선호도 작업을 완료하지 못했습니다.</strong>
      <span>{error.message}</span>
      {error.details.length > 0 ? (
        <ul className="error-details">
          {error.details.map((detail) => (
            <li key={`${detail.field}:${detail.message}`}>
              {detail.field}: {detail.message}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function toggleValue<T extends string>(values: T[], value: T): T[] {
  return values.includes(value)
    ? values.filter((candidate) => candidate !== value)
    : [...values, value];
}

function areArraysEqual(left: string[], right: string[]): boolean {
  return left.length === right.length && left.every((value, index) => value === right[index]);
}

function getSaveStatusLabel(saved: boolean): string {
  return saved ? '저장 완료' : '저장 예정';
}

type PreferencesPanelProps = {
  accessToken: string;
  onAuthExpired: () => void;
  onPreferencesConfirmed: () => void;
};

export function PreferencesPanel({
  accessToken,
  onAuthExpired,
  onPreferencesConfirmed,
}: PreferencesPanelProps) {
  const [preferences, setPreferences] = useState<UserPreferencesResponse>(emptyPreferences);
  const [savedPreferences, setSavedPreferences] =
    useState<UserPreferencesResponse>(emptyPreferences);
  const [tagInput, setTagInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const selectedColorLabel =
    preferences.preferredColors[0]
      ? clothingColorMetadata[preferences.preferredColors[0]].label
      : '색상';
  const selectedMaterialLabel =
    preferences.preferredMaterials[0]
      ? clothingMaterialLabels[preferences.preferredMaterials[0]]
      : '소재';
  const displayPreferenceStyleTags = getDisplayStyleTags(preferences.styleTags);
  const displayPreferenceStyleTagEntries = getDisplayStyleTagEntries(preferences.styleTags);
  const selectedTagLabel =
    displayPreferenceStyleTags[0] ?? '태그';
  // savedPreferences는 마지막 서버 저장값이라, 현재 편집값과 비교해 "저장 예정" 상태를 표시한다.
  const colorSummaryStatusLabel = getSaveStatusLabel(
    areArraysEqual(preferences.preferredColors, savedPreferences.preferredColors)
  );
  const materialSummaryStatusLabel = getSaveStatusLabel(
    areArraysEqual(preferences.preferredMaterials, savedPreferences.preferredMaterials)
  );
  const tagSummaryStatusLabel = getSaveStatusLabel(
    areArraysEqual(
      normalizeStyleTags(preferences.styleTags),
      normalizeStyleTags(savedPreferences.styleTags)
    )
  );
  const renderPreferencesSaveBar = (className = '', showDescription = true) => (
    <div
      className={className ? `preferences-save-bar ${className}` : 'preferences-save-bar'}
    >
      {showDescription ? (
        <span className="muted">저장하면 오늘 체크리스트에 확인 상태가 반영됩니다.</span>
      ) : null}
      <button className="primary-button" type="submit" disabled={saving}>
        {saving ? '저장 중' : '선호도 저장'}
      </button>
    </div>
  );

  const loadPreferences = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const loaded = await getUserPreferences(accessToken);
      setPreferences(loaded);
      setSavedPreferences(loaded);
      onPreferencesConfirmed();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setPreferences(emptyPreferences);
      setSavedPreferences(emptyPreferences);
      setError(toErrorResponse(caught, '선호도를 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  }, [accessToken, onAuthExpired, onPreferencesConfirmed]);

  useEffect(() => {
    void loadPreferences();
  }, [loadPreferences]);

  const addStyleTagsToPreferences = (nextTags: string[], reportEmpty: boolean): boolean => {
    const normalizedNextTags = normalizeStyleTags(nextTags);
    setError(null);

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

    setPreferences((current) => ({
      ...current,
      styleTags: mergeStyleTags(current.styleTags, normalizedNextTags),
    }));
    return true;
  };

  const handleAddTag = () => {
    if (addStyleTagsToPreferences(parseStyleTagInput(tagInput), true)) {
      setTagInput('');
    }
  };

  const handleToggleSuggestedTag = (tag: string) => {
    setError(null);
    setPreferences((current) => ({
      ...current,
      styleTags: hasStyleTag(current.styleTags, tag)
        ? removeStyleTag(current.styleTags, tag)
        : mergeStyleTags(current.styleTags, [tag]),
    }));
    setTagInput('');
  };

  const handleToggleColor = (color: ClothingColor) => {
    setError(null);
    setPreferences({
      ...preferences,
      preferredColors: toggleValue(preferences.preferredColors, color),
    });
  };

  const handleToggleMaterial = (material: ClothingMaterial) => {
    setError(null);
    setPreferences({
      ...preferences,
      preferredMaterials: toggleValue(preferences.preferredMaterials, material),
    });
  };

  const handleRemoveDisplayedTags = (sourceTags: string[]) => {
    setError(null);
    setPreferences({
      ...preferences,
      styleTags: sourceTags.reduce(
        (remainingTags, tag) => removeStyleTag(remainingTags, tag),
        preferences.styleTags
      ),
    });
  };

  const handleSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);

    try {
      const requestBody: UserPreferencesResponse = {
        preferredColors: preferences.preferredColors,
        preferredMaterials: preferences.preferredMaterials,
        styleTags: normalizeStyleTags(preferences.styleTags),
      };
      const saved = await updateUserPreferences(accessToken, requestBody);
      setPreferences(saved);
      setSavedPreferences(saved);
      onPreferencesConfirmed();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, '선호도를 저장하지 못했습니다.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <article className="panel preferences-panel">
      {loading ? (
        <p className="muted">선호도를 불러오고 있어요.</p>
      ) : (
        <form className="panel-form compact-form preferences-form" onSubmit={handleSave}>
          <section className="preferences-hero" aria-label="저장할 취향 요약">
            <div>
              <p className="eyebrow">취향 프로필</p>
              <h3>
                {selectedColorLabel}, {selectedMaterialLabel}, {selectedTagLabel} 취향을
                선호해요
              </h3>
              <p>
                저장한 색상, 소재, 분위기와 잘 맞는 옷을 추천에서 더 우선해 보여줘요.
              </p>
            </div>
          </section>

          <section className="preference-summary-card" aria-label="선호도 저장 및 요약">
            <dl className="metric-list preference-summary" aria-label="저장할 선호도 요약">
              <div>
                <dt>색상</dt>
                <dd>
                  {preferences.preferredColors.length}개 {colorSummaryStatusLabel}
                </dd>
              </div>
              <div>
                <dt>소재</dt>
                <dd>
                  {preferences.preferredMaterials.length}개 {materialSummaryStatusLabel}
                </dd>
              </div>
              <div>
                <dt>태그</dt>
                <dd>
                  {preferences.styleTags.length}개 {tagSummaryStatusLabel}
                </dd>
              </div>
            </dl>
            {renderPreferencesSaveBar('preferences-summary-save', false)}
          </section>

          <div className="preferences-layout">
            <div className="preference-choice-stack">
              <section className="preference-card color-preference-card" aria-label="선호 색상">
                <div className="section-title-row">
                  <div>
                    <p className="eyebrow">색상</p>
                    <h3>선호 색상</h3>
                    <p className="muted preference-helper">
                      원하는 색상 버튼을 눌러 여러 색상을 함께 선택하세요.
                    </p>
                  </div>
                  <span className="preference-count-pill">
                    {preferences.preferredColors.length}개 선택
                  </span>
                </div>
                <div className="preference-option-grid color-preference-grid">
                  {clothingColorOptions.map((color) => {
                    const selected = preferences.preferredColors.includes(color);

                    return (
                      <button
                        className={
                          selected
                            ? 'preference-option-button color-preference-button active'
                            : 'preference-option-button color-preference-button'
                        }
                        type="button"
                        key={color}
                        aria-pressed={selected}
                        onClick={() => handleToggleColor(color)}
                      >
                        <ColorSwatch color={color} size="large" />
                      </button>
                    );
                  })}
                </div>
              </section>

              <section
                className="preference-card material-preference-card"
                aria-label="선호 소재"
              >
                <div className="section-title-row">
                  <div>
                    <p className="eyebrow">소재</p>
                    <h3>선호 소재</h3>
                    <p className="muted preference-helper">
                      손이 자주 가는 소재를 골라주세요.
                    </p>
                  </div>
                  <span className="preference-count-pill">
                    {preferences.preferredMaterials.length}개 선택
                  </span>
                </div>
                <div className="preference-option-grid material-preference-grid">
                  {clothingMaterialOptions.map((material) => {
                    const selected = preferences.preferredMaterials.includes(material);

                    return (
                      <button
                        className={
                          selected
                            ? 'preference-option-button material-preference-button active'
                            : 'preference-option-button material-preference-button'
                        }
                        type="button"
                        key={material}
                        aria-pressed={selected}
                        onClick={() => handleToggleMaterial(material)}
                      >
                        <MaterialChip material={material} />
                      </button>
                    );
                  })}
                </div>
              </section>
            </div>

            <div className="preference-side-stack">
              <section className="preference-card style-tag-card" aria-label="스타일 태그">
                <div className="section-title-row">
                  <div>
                    <p className="eyebrow">태그</p>
                    <h3>{styleTagLabels.title}</h3>
                    <p className="muted preference-helper">
                      추천 취향으로 함께 저장됩니다.
                    </p>
                  </div>
                  <span className="preference-count-pill">
                    {displayPreferenceStyleTags.length}개 태그
                  </span>
                </div>
                <div className="style-tag-suggestions" aria-label="추천 스타일 태그">
                  {styleTagSuggestionGroups.map((group) => (
                    <div className="style-tag-suggestion-group" key={group.situation}>
                      <span className="style-tag-group-label">
                        {recommendationSituationLabels[group.situation]}
                      </span>
                      <div className="style-tag-suggestion-chips">
                        {group.tags.map((tag) => {
                          const selected = hasStyleTag(preferences.styleTags, tag);

                          return (
                            <button
                              className={selected ? 'suggestion-chip active' : 'suggestion-chip'}
                              type="button"
                              key={`${group.situation}:${tag}`}
                              aria-pressed={selected}
                              onClick={() => handleToggleSuggestedTag(tag)}
                              disabled={saving}
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
                    <span>{styleTagLabels.inputLabel}</span>
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
                      placeholder={styleTagLabels.placeholder}
                    />
                  </label>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={handleAddTag}
                    disabled={saving}
                  >
                    {styleTagLabels.addCta}
                  </button>
                </div>
                <div className="tag-list" aria-label="저장된 스타일 태그">
                  {displayPreferenceStyleTagEntries.length > 0 ? (
                    displayPreferenceStyleTagEntries.map((entry) => (
                      <span className="tag-chip" key={entry.label}>
                        {entry.label}
                        <button
                          type="button"
                          aria-label={`${entry.label} 삭제`}
                          onClick={() => handleRemoveDisplayedTags(entry.sourceTags)}
                        >
                          x
                        </button>
                      </span>
                    ))
                  ) : (
                    <span className="muted">{styleTagLabels.empty}</span>
                  )}
                </div>
              </section>

              {renderPreferencesSaveBar('preferences-mobile-only')}
            </div>
          </div>
        </form>
      )}

      {error ? <PreferenceErrorMessage error={error} /> : null}
    </article>
  );
}
