import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import { getUserPreferences, updateUserPreferences } from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import { ColorSwatch, MaterialChip } from '../../components/DisplayTokens';
import type {
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

function toggleValue<T extends string>(values: T[], value: T): T[] {
  return values.includes(value)
    ? values.filter((candidate) => candidate !== value)
    : [...values, value];
}

type PreferencesPanelProps = {
  accessToken: string;
  onAuthExpired: () => void;
};

export function PreferencesPanel({ accessToken, onAuthExpired }: PreferencesPanelProps) {
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
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setPreferences(emptyPreferences);
      setError(toErrorResponse(caught, 'Unable to load preferences.'));
    } finally {
      setLoading(false);
    }
  }, [accessToken, onAuthExpired]);

  useEffect(() => {
    void loadPreferences();
  }, [loadPreferences]);

  const handleAddTag = () => {
    const nextTag = tagInput.trim();
    setError(null);
    setStatus(null);

    if (!nextTag) {
      setError(validationError('Style tag must not be blank.'));
      return;
    }
    if (nextTag.length > 30) {
      setError(validationError('Style tag must be 30 characters or fewer.'));
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

  const handleSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setStatus(null);

    try {
      const saved = await updateUserPreferences(accessToken, preferences);
      setPreferences(saved);
      setStatus('Preferences saved.');
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to save preferences.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <article className="panel">
      <div className="section-title-row">
        <h2>Preferences</h2>
        <button
          className="secondary-button"
          type="button"
          onClick={() => void loadPreferences()}
          disabled={loading || saving}
        >
          Refresh
        </button>
      </div>

      {loading ? (
        <p className="muted">Loading preferences.</p>
      ) : (
        <form className="panel-form compact-form" onSubmit={handleSave}>
          <section className="panel-section" aria-label="Preferred colors">
            <h3>Preferred colors</h3>
            <div className="checkbox-grid">
              {clothingColorOptions.map((color) => (
                <label className="checkbox-field" key={color}>
                  <input
                    type="checkbox"
                    checked={preferences.preferredColors.includes(color)}
                    onChange={() =>
                      setPreferences({
                        ...preferences,
                        preferredColors: toggleValue(preferences.preferredColors, color),
                      })
                    }
                  />
                  <ColorSwatch color={color} />
                </label>
              ))}
            </div>
          </section>

          <section className="panel-section" aria-label="Preferred materials">
            <h3>Preferred materials</h3>
            <div className="checkbox-grid">
              {clothingMaterialOptions.map((material) => (
                <label className="checkbox-field" key={material}>
                  <input
                    type="checkbox"
                    checked={preferences.preferredMaterials.includes(material)}
                    onChange={() =>
                      setPreferences({
                        ...preferences,
                        preferredMaterials: toggleValue(
                          preferences.preferredMaterials,
                          material
                        ),
                      })
                    }
                  />
                  <MaterialChip material={material} />
                </label>
              ))}
            </div>
          </section>

          <section className="panel-section" aria-label="Style tags">
            <h3>{styleTagLabels.title}</h3>
            <div className="inline-form tag-form">
              <label className="field">
                <span>{styleTagLabels.inputLabel}</span>
                <input
                  value={tagInput}
                  maxLength={30}
                  onChange={(event) => setTagInput(event.target.value)}
                  placeholder={styleTagLabels.placeholder}
                />
              </label>
              <button className="secondary-button" type="button" onClick={handleAddTag}>
                {styleTagLabels.addCta}
              </button>
            </div>
            <div className="tag-list" aria-label="Saved style tags">
              {preferences.styleTags.length > 0 ? (
                preferences.styleTags.map((tag) => (
                  <span className="tag-chip" key={tag}>
                    {formatStyleTagLabel(tag)}
                    <button
                      type="button"
                      aria-label={`Remove ${tag}`}
                      onClick={() =>
                        setPreferences({
                          ...preferences,
                          styleTags: preferences.styleTags.filter((candidate) => candidate !== tag),
                        })
                      }
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

          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? 'Saving' : 'Save preferences'}
          </button>
        </form>
      )}

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}
    </article>
  );
}
