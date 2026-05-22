import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import { getLocations, updateUserLocation } from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import type {
  ErrorResponse,
  LocationOptionResponse,
  UserLocationResponse,
} from '../../types/api';

type LocationPanelProps = {
  accessToken: string;
  location: UserLocationResponse | null;
  loading: boolean;
  onAuthExpired: () => void;
  onLocationChange: (location: UserLocationResponse) => void;
};

export function LocationPanel({
  accessToken,
  location,
  loading,
  onAuthExpired,
  onLocationChange,
}: LocationPanelProps) {
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [options, setOptions] = useState<LocationOptionResponse[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(true);
  const [savingCode, setSavingCode] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const loadLocations = useCallback(async () => {
    setOptionsLoading(true);
    setError(null);

    try {
      const nextOptions = await getLocations(accessToken, submittedKeyword);
      setOptions(nextOptions);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setOptions([]);
      setError(toErrorResponse(caught, 'Unable to load the location catalog.'));
    } finally {
      setOptionsLoading(false);
    }
  }, [accessToken, onAuthExpired, submittedKeyword]);

  useEffect(() => {
    void loadLocations();
  }, [loadLocations]);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmittedKeyword(keyword.trim());
    setStatus(null);
  };

  const handleSelect = async (option: LocationOptionResponse) => {
    setSavingCode(option.code);
    setError(null);
    setStatus(null);

    try {
      const updatedLocation = await updateUserLocation(accessToken, option.code);
      onLocationChange(updatedLocation);
      setStatus(`${updatedLocation.name} saved as the current location.`);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to save the selected location.'));
    } finally {
      setSavingCode(null);
    }
  };

  return (
    <article className="panel">
      <h2>Location</h2>
      <section className="panel-section" aria-label="Current location">
        {loading ? (
          <p className="muted">Loading current user location.</p>
        ) : location ? (
          <dl className="metric-list compact">
            <div>
              <dt>Name</dt>
              <dd>{location.name}</dd>
            </div>
            <div>
              <dt>Code</dt>
              <dd>{location.code}</dd>
            </div>
            <div>
              <dt>KMA grid</dt>
              <dd>
                nx={location.nx}, ny={location.ny}
              </dd>
            </div>
          </dl>
        ) : (
          <p className="muted">No location data loaded.</p>
        )}
      </section>

      <form className="inline-form" onSubmit={handleSearch}>
        <label className="field">
          <span>Keyword</span>
          <input
            type="search"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="SEOUL, BUSAN"
          />
        </label>
        <button className="secondary-button" type="submit" disabled={optionsLoading}>
          Search
        </button>
      </form>

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}

      <div className="option-list" aria-label="Location catalog">
        {optionsLoading ? (
          <p className="muted">Loading locations.</p>
        ) : options.length > 0 ? (
          options.map((option) => {
            const selected = location?.code === option.code;
            const saving = savingCode === option.code;

            return (
              <div className="option-row" key={option.code}>
                <div>
                  <strong>{option.name}</strong>
                  <span>
                    {option.code} - nx={option.nx}, ny={option.ny}
                  </span>
                </div>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => void handleSelect(option)}
                  disabled={selected || savingCode !== null}
                >
                  {selected ? 'Selected' : saving ? 'Saving' : 'Select'}
                </button>
              </div>
            );
          })
        ) : (
          <p className="muted">No locations match this keyword.</p>
        )}
      </div>
    </article>
  );
}
