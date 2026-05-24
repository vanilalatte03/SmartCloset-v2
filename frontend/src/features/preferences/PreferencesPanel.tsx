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
  clothingMaterialOptions,
  formatStyleTagLabel,
  styleTagLabels,
} from '../../utils/displayMappings';

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
  const [tagInput, setTagInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const loadPreferences = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const loaded = await getUserPreferences(accessToken);
      setPreferences(loaded);
      onPreferencesConfirmed();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setPreferences(emptyPreferences);
      setError(toErrorResponse(caught, '선호도를 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  }, [accessToken, onAuthExpired, onPreferencesConfirmed]);

  useEffect(() => {
    void loadPreferences();
  }, [loadPreferences]);

  const handleAddTag = () => {
    const nextTag = tagInput.trim();
    setError(null);
    setStatus(null);

    if (!nextTag) {
      setError(validationError('스타일 태그를 입력해주세요.'));
      return;
    }
    if (nextTag.length > 30) {
      setError(validationError('스타일 태그는 30자 이하로 입력해주세요.'));
      return;
    }
    if (preferences.styleTags.includes(nextTag)) {
      setTagInput('');
      return;
    }

    setPreferences({
      ...preferences,
      styleTags: [...preferences.styleTags, nextTag],
    });
    setTagInput('');
  };

  const handleToggleColor = (color: ClothingColor) => {
    setError(null);
    setStatus(null);
    setPreferences({
      ...preferences,
      preferredColors: toggleValue(preferences.preferredColors, color),
    });
  };

  const handleToggleMaterial = (material: ClothingMaterial) => {
    setError(null);
    setStatus(null);
    setPreferences({
      ...preferences,
      preferredMaterials: toggleValue(preferences.preferredMaterials, material),
    });
  };

  const handleRemoveTag = (tag: string) => {
    setError(null);
    setStatus(null);
    setPreferences({
      ...preferences,
      styleTags: preferences.styleTags.filter((candidate) => candidate !== tag),
    });
  };

  const normalizeStyleTags = (tags: string[]) =>
    Array.from(new Set(tags.map((tag) => tag.trim()).filter(Boolean)));

  const handleSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setStatus(null);

    try {
      const requestBody: UserPreferencesResponse = {
        preferredColors: preferences.preferredColors,
        preferredMaterials: preferences.preferredMaterials,
        styleTags: normalizeStyleTags(preferences.styleTags),
      };
      const saved = await updateUserPreferences(accessToken, requestBody);
      setPreferences(saved);
      onPreferencesConfirmed();
      setStatus('선호도를 저장했습니다. 오늘 체크리스트에도 반영됩니다.');
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
      <div className="section-title-row">
        <div>
          <h2>선호도</h2>
          <p className="muted preference-guidance">
            색상과 소재는 취향 선택으로 저장하고, 스타일 태그는 화면에서 참고하기 위한
            표시용 문구로만 보관합니다.
          </p>
        </div>
        <button
          className="secondary-button"
          type="button"
          onClick={() => void loadPreferences()}
          disabled={loading || saving}
        >
          새로고침
        </button>
      </div>

      {loading ? (
        <p className="muted">선호도를 불러오고 있어요.</p>
      ) : (
        <form className="panel-form compact-form preferences-form" onSubmit={handleSave}>
          <dl className="metric-list preference-summary" aria-label="저장할 선호도 요약">
            <div>
              <dt>색상</dt>
              <dd>{preferences.preferredColors.length}개 선택</dd>
            </div>
            <div>
              <dt>소재</dt>
              <dd>{preferences.preferredMaterials.length}개 선택</dd>
            </div>
            <div>
              <dt>태그</dt>
              <dd>{preferences.styleTags.length}개 저장 예정</dd>
            </div>
          </dl>

          <div className="preferences-layout">
            <div className="preference-choice-stack">
              <section className="preference-card color-preference-card" aria-label="선호 색상">
                <div className="section-title-row">
                  <div>
                    <h3>선호 색상</h3>
                    <p className="muted preference-helper">
                      원형 swatch를 눌러 여러 색상을 함께 선택하세요.
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
                    <h3>선호 소재</h3>
                    <p className="muted preference-helper">
                      자주 손이 가는 소재를 chip으로 골라두세요.
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
                    <h3>{styleTagLabels.title}</h3>
                    <p className="muted preference-helper">
                      룩을 기억하기 위한 별도 표시 영역입니다.
                    </p>
                  </div>
                  <span className="preference-count-pill">
                    {preferences.styleTags.length}개 태그
                  </span>
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
                      }}
                      placeholder={styleTagLabels.placeholder}
                    />
                  </label>
                  <button className="secondary-button" type="button" onClick={handleAddTag}>
                    {styleTagLabels.addCta}
                  </button>
                </div>
                <div className="tag-list" aria-label="저장된 스타일 태그">
                  {preferences.styleTags.length > 0 ? (
                    preferences.styleTags.map((tag) => (
                      <span className="tag-chip" key={tag}>
                        {formatStyleTagLabel(tag)}
                        <button
                          type="button"
                          aria-label={`${tag} 삭제`}
                          onClick={() => handleRemoveTag(tag)}
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

              <div className="preferences-save-bar">
                <span className="muted">저장하면 Today 체크리스트에 확인 상태가 반영됩니다.</span>
                <button className="primary-button" type="submit" disabled={saving}>
                  {saving ? '저장 중' : '선호도 저장'}
                </button>
              </div>
            </div>
          </div>
        </form>
      )}

      {error ? <PreferenceErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}
    </article>
  );
}
