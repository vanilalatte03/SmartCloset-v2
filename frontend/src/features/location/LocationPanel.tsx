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

const locationCatalogApiPath = '/api/locations?keyword={keyword}';

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

  const searchSummary = submittedKeyword
    ? `"${submittedKeyword}" 검색 결과`
    : '전체 대표 위치';

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

  const handleShowAll = () => {
    setKeyword('');
    setSubmittedKeyword('');
    setStatus(null);
  };

  const handleSelect = async (option: LocationOptionResponse) => {
    setSavingCode(option.code);
    setError(null);
    setStatus(null);

    try {
      const updatedLocation = await updateUserLocation(accessToken, option.code);
      onLocationChange(updatedLocation);
      setStatus(`현재 위치를 ${updatedLocation.name}(으)로 저장했습니다.`);
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
    <article className="panel location-panel">
      <div className="section-title-row">
        <div>
          <h2>위치</h2>
          <p className="muted location-panel-copy">
            내장 대표 격자 catalog에서 현재 위치를 선택하면 오늘 날씨 요약도 새로
            확인됩니다.
          </p>
        </div>
      </div>

      <section className="panel-section location-current-card" aria-label="현재 위치">
        <div className="section-title-row">
          <div>
            <p className="eyebrow">현재 위치</p>
            <h3>{location ? location.name : '위치 확인 중'}</h3>
          </div>
        </div>
        {loading ? (
          <p className="muted">현재 위치를 불러오고 있어요.</p>
        ) : location ? (
          <dl className="metric-list compact location-current-metrics">
            <div>
              <dt>catalog code</dt>
              <dd>{location.code}</dd>
            </div>
            <div>
              <dt>KMA 격자</dt>
              <dd>
                nx={location.nx}, ny={location.ny}
              </dd>
            </div>
            <div>
              <dt>갱신</dt>
              <dd>{formatUpdatedAt(location.updatedAt)}</dd>
            </div>
          </dl>
        ) : (
          <p className="muted">불러온 위치 정보가 없어요. catalog에서 위치를 선택해주세요.</p>
        )}
      </section>

      <form className="inline-form location-search-form" onSubmit={handleSearch}>
        <label className="field">
          <span>검색어</span>
          <input
            type="search"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="서울특별시, 부산광역시, SEOUL"
          />
        </label>
        <div className="location-search-actions">
          <button className="secondary-button" type="submit" disabled={optionsLoading}>
            검색
          </button>
          <button
            className="secondary-button"
            type="button"
            onClick={handleShowAll}
            disabled={optionsLoading || (!submittedKeyword && !keyword.trim())}
          >
            전체 보기
          </button>
        </div>
      </form>

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}

      <section
        className="panel-section location-catalog-section"
        aria-label="위치 catalog"
        data-api-path={locationCatalogApiPath}
      >
        <div className="section-title-row">
          <div>
            <p className="eyebrow">Catalog</p>
            <h3>{searchSummary}</h3>
          </div>
          <span className="location-result-count">
            {optionsLoading ? '검색 중' : `${options.length}개`}
          </span>
        </div>

        <div className="option-list location-option-list" aria-label="위치 목록">
        {optionsLoading ? (
          <p className="muted">위치 목록을 불러오고 있어요.</p>
        ) : options.length > 0 ? (
          options.map((option) => {
            const selected = location?.code === option.code;
            const saving = savingCode === option.code;

            return (
              <div
                className={
                  selected
                    ? 'option-row location-option-row selected'
                    : 'option-row location-option-row'
                }
                key={option.code}
              >
                <div>
                  <strong>{option.name}</strong>
                  <span>{option.code}</span>
                  <span>
                    KMA 격자 nx={option.nx}, ny={option.ny}
                  </span>
                </div>
                <button
                  className={selected ? 'primary-button' : 'secondary-button'}
                  type="button"
                  onClick={() => void handleSelect(option)}
                  disabled={selected || savingCode !== null}
                >
                  {selected ? '현재 위치' : saving ? '저장 중' : '이 위치 선택'}
                </button>
              </div>
            );
          })
        ) : (
          <p className="muted">
            검색어와 일치하는 위치가 없어요. 도시명이나 code로 다시 검색해주세요.
          </p>
        )}
        </div>
      </section>
    </article>
  );
}

function formatUpdatedAt(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}
