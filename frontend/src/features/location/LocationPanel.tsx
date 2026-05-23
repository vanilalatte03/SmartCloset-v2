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
      setError(toErrorResponse(caught, '위치 목록을 불러오지 못했습니다.'));
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
      setStatus(`${updatedLocation.name}을 현재 위치로 저장했습니다.`);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, '선택한 위치를 저장하지 못했습니다.'));
    } finally {
      setSavingCode(null);
    }
  };

  return (
    <article className="panel">
      <h2>위치</h2>
      <section className="panel-section" aria-label="현재 위치">
        {loading ? (
          <p className="muted">현재 위치를 불러오고 있어요.</p>
        ) : location ? (
          <dl className="metric-list compact">
            <div>
              <dt>위치</dt>
              <dd>{location.name}</dd>
            </div>
            <div>
              <dt>코드</dt>
              <dd>{location.code}</dd>
            </div>
            <div>
              <dt>KMA 격자</dt>
              <dd>
                nx={location.nx}, ny={location.ny}
              </dd>
            </div>
          </dl>
        ) : (
          <p className="muted">불러온 위치 정보가 없어요.</p>
        )}
      </section>

      <form className="inline-form" onSubmit={handleSearch}>
        <label className="field">
          <span>검색어</span>
          <input
            type="search"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="서울, 부산, SEOUL"
          />
        </label>
        <button className="secondary-button" type="submit" disabled={optionsLoading}>
          검색
        </button>
      </form>

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}

      <div className="option-list" aria-label="위치 목록">
        {optionsLoading ? (
          <p className="muted">위치 목록을 불러오고 있어요.</p>
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
                  {selected ? '선택됨' : saving ? '저장 중' : '선택'}
                </button>
              </div>
            );
          })
        ) : (
          <p className="muted">검색어와 일치하는 위치가 없어요.</p>
        )}
      </div>
    </article>
  );
}
