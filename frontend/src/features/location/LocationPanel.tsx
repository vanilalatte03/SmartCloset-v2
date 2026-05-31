import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import {
  getCurrentWeather,
  resolveLocation,
  searchLocations,
  updateUserLocation,
} from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import { WeatherBadge, WeatherLabel } from '../../components/DisplayTokens';
import type {
  ErrorResponse,
  LocationOptionResponse,
  LocationSource,
  UserLocationResponse,
  WeatherResponse,
} from '../../types/api';
import { locationSourceLabels } from '../../utils/displayMappings';
import { getRoParticle } from '../../utils/koreanParticles';

const locationCatalogApiPath = '/api/locations?keyword={keyword}';
const locationResolveApiPath = '/api/locations/resolve';
const locationWeatherApiPath = '/api/weather/current';

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
  const [weather, setWeather] = useState<WeatherResponse | null>(null);
  const [weatherLoading, setWeatherLoading] = useState(true);
  const [weatherError, setWeatherError] = useState<ErrorResponse | null>(null);
  const [savingCode, setSavingCode] = useState<string | null>(null);
  const [resolveOptions, setResolveOptions] = useState<LocationOptionResponse[]>([]);
  const [resolveLoading, setResolveLoading] = useState(false);
  const [resolveStatus, setResolveStatus] = useState<string | null>(null);
  const [resolveError, setResolveError] = useState<ErrorResponse | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const searchSummary = submittedKeyword
    ? `"${submittedKeyword}" 검색 결과`
    : '전체 대표 위치';

  const loadCurrentWeather = useCallback(async () => {
    setWeatherLoading(true);
    setWeatherError(null);

    try {
      const currentWeather = await getCurrentWeather(accessToken);
      setWeather(currentWeather);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setWeather(null);
      setWeatherError(toErrorResponse(caught, '현재 날씨를 불러오지 못했습니다.'));
    } finally {
      setWeatherLoading(false);
    }
  }, [accessToken, onAuthExpired]);

  const loadLocations = useCallback(async () => {
    setOptionsLoading(true);
    setError(null);

    try {
      const nextOptions = await searchLocations(accessToken, submittedKeyword);
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
    void loadCurrentWeather();
  }, [loadCurrentWeather, location?.code, location?.updatedAt]);

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

  const handleSelect = async (
    option: LocationOptionResponse,
    source: LocationSource = 'MANUAL_SEARCH'
  ) => {
    setSavingCode(option.code);
    setError(null);
    setStatus(null);

    try {
      const updatedLocation = await updateUserLocation(accessToken, {
        locationCode: option.code,
        source,
      });
      onLocationChange(updatedLocation);
      setStatus(
        `현재 위치를 ${updatedLocation.name}${getRoParticle(updatedLocation.name)} 저장했습니다.`
      );
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

  const handleResolveCurrentLocation = () => {
    setResolveError(null);
    setResolveStatus(null);
    setResolveOptions([]);

    if (!('geolocation' in navigator)) {
      setResolveError({
        code: 'GEOLOCATION_UNSUPPORTED',
        message: '이 브라우저에서는 현재 위치 찾기를 사용할 수 없습니다.',
        details: [],
      });
      return;
    }

    setResolveLoading(true);
    setResolveStatus('브라우저 위치 권한을 확인하고 있습니다.');

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;

        resolveLocation(accessToken, { latitude, longitude })
          .then((response) => {
            setResolveOptions(response.candidates);
            setResolveStatus(
              response.nearest
                ? `KMA 격자 nx=${response.grid.nx}, ny=${response.grid.ny} 기준 후보를 찾았습니다.`
                : `KMA 격자 nx=${response.grid.nx}, ny=${response.grid.ny} 기준 후보가 없습니다.`
            );
          })
          .catch((caught) => {
            if (isUnauthorizedError(caught)) {
              onAuthExpired();
              return;
            }
            setResolveError(toErrorResponse(caught, '현재 위치 후보를 찾지 못했습니다.'));
          })
          .finally(() => {
            setResolveLoading(false);
          });
      },
      (positionError) => {
        const denied = positionError.code === positionError.PERMISSION_DENIED;
        setResolveError({
          code: denied ? 'GEOLOCATION_DENIED' : 'GEOLOCATION_FAILED',
          message: denied
            ? '브라우저 위치 권한이 거부되었습니다. 수동 검색으로 위치를 선택해주세요.'
            : '브라우저 현재 위치를 가져오지 못했습니다. 수동 검색으로 위치를 선택해주세요.',
          details: [],
        });
        setResolveLoading(false);
      },
      {
        enableHighAccuracy: false,
        maximumAge: 5 * 60 * 1000,
        timeout: 10000,
      }
    );
  };

  return (
    <article className="location-panel">
      <section className="location-hero" aria-label="저장된 위치 요약">
        <div>
          <p className="eyebrow">현재 저장된 위치</p>
          <h2>{location ? location.fullName || location.name : '위치 확인 중'}</h2>
          <p>
            추천과 기록은 저장한 동네의 날씨 기준으로 계산됩니다.
          </p>
          {location ? (
            <div className="location-hero-tags" aria-label="위치 기준">
              <span>{locationSourceLabels[location.source]}</span>
              <span>추천에 반영</span>
            </div>
          ) : null}
        </div>
        {location ? (
          <dl className="location-hero-grid" aria-label="KMA 위치 기준">
            <div>
              <dt>catalog</dt>
              <dd>{location.code}</dd>
            </div>
            <div>
              <dt>KMA grid</dt>
              <dd>
                {location.nx}/{location.ny}
              </dd>
            </div>
          </dl>
        ) : null}
      </section>

      <div className="location-status-grid">
        <section className="panel location-current-card" aria-label="현재 위치">
          <div className="section-title-row location-current-heading">
            <div>
              <p className="eyebrow">현재 위치</p>
              <h2>{location ? location.name : '위치 확인 중'}</h2>
              <p className="muted location-panel-copy">
                저장된 행정구역과 KMA 격자를 기준으로 오늘 날씨와 추천에 사용할 위치를 정합니다.
              </p>
            </div>
            {location ? <span className="location-code-pill">{location.code}</span> : null}
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
                <dt>선택 방식</dt>
                <dd>{locationSourceLabels[location.source]}</dd>
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

        <section
          className="panel location-weather-card"
          aria-label="현재 날씨 요약"
          data-api-path={locationWeatherApiPath}
        >
          <div className="section-title-row">
            <div>
              <p className="eyebrow">현재 날씨 요약</p>
              <h3>{location ? `${location.name} 기준` : '저장 위치 기준'}</h3>
            </div>
            <button
              className="secondary-button"
              type="button"
              onClick={() => void loadCurrentWeather()}
              disabled={weatherLoading}
            >
              새로고침
            </button>
          </div>

          {weatherLoading ? <p className="muted">현재 날씨를 확인하고 있어요.</p> : null}
          {!weatherLoading && weather ? (
            <div className="location-weather-summary">
              <WeatherBadge weather={weather} />
              <dl className="metric-list compact location-weather-metrics">
                <div>
                  <dt>날씨</dt>
                  <dd>
                    <WeatherLabel weatherType={weather.weatherType} />
                  </dd>
                </div>
                <div>
                  <dt>강수</dt>
                  <dd>{weather.rainy ? '비 가능' : '비 없음'}</dd>
                </div>
                <div>
                  <dt>바람</dt>
                  <dd>{weather.windy ? '바람 강함' : '바람 잔잔'}</dd>
                </div>
              </dl>
            </div>
          ) : null}
          {!weatherLoading && weatherError ? (
            <div className="location-weather-fallback">
              <ApiErrorMessage error={weatherError} />
              <p className="muted">
                날씨 요약이 없어도 위치 검색과 선택은 계속 사용할 수 있어요.
              </p>
            </div>
          ) : null}
        </section>
      </div>

      <section
        className="panel location-search-card"
        aria-label="위치 catalog"
        data-api-path={locationCatalogApiPath}
      >
        <div className="section-title-row">
          <div>
            <h2>위치</h2>
            <p className="muted location-panel-copy">
              행정구역 catalog에서 직접 검색하거나 현재 위치 후보를 확인한 뒤 저장합니다.
            </p>
          </div>
          <span className="location-result-count">
            {optionsLoading ? '검색 중' : `${options.length}개`}
          </span>
        </div>

        <form className="inline-form location-search-form" onSubmit={handleSearch}>
          <label className="field">
            <span>검색어</span>
            <input
              type="search"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="예: 일산동, 역삼동, SEOUL"
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

        <section className="location-resolve-box" data-api-path={locationResolveApiPath}>
          <div className="section-title-row">
            <div>
              <p className="eyebrow">현재 위치 후보</p>
              <h3>브라우저 현재 위치로 찾기</h3>
              <p className="muted location-panel-copy">
                좌표는 후보 찾기에만 사용하며, 후보를 선택해야 내 위치로 저장됩니다.
              </p>
            </div>
            <button
              className="secondary-button"
              type="button"
              onClick={handleResolveCurrentLocation}
              disabled={resolveLoading || savingCode !== null}
            >
              {resolveLoading ? '확인 중' : '현재 위치로 찾기'}
            </button>
          </div>

          {resolveStatus ? (
            <p className="panel-success location-resolve-status" role="status">
              {resolveStatus}
            </p>
          ) : null}
          {resolveError ? <ApiErrorMessage error={resolveError} /> : null}

          <div className="location-coordinate-note" role="note">
            <strong>좌표는 저장하지 않아요</strong>
            <span>브라우저 좌표는 가까운 행정구역 후보를 찾는 데만 사용됩니다.</span>
          </div>

          {resolveOptions.length > 0 ? (
            <div className="location-option-list" aria-label="현재 위치 후보 목록">
              {resolveOptions.map((option) => {
                const selected = location?.code === option.code;
                const saving = savingCode === option.code;

                return (
                  <article
                    className={selected ? 'location-option-card selected' : 'location-option-card'}
                    key={`resolve-${option.code}`}
                  >
                    <div className="location-option-main">
                      <strong>{option.name}</strong>
                      <span>{option.fullName}</span>
                      <span>
                        KMA 격자 nx={option.nx}, ny={option.ny}
                      </span>
                    </div>
                    <button
                      className={selected ? 'primary-button' : 'secondary-button'}
                      type="button"
                      onClick={() => void handleSelect(option, 'BROWSER_GEOLOCATION')}
                      disabled={selected || savingCode !== null}
                    >
                      {selected ? '현재 위치' : saving ? '저장 중' : '후보 저장'}
                    </button>
                  </article>
                );
              })}
            </div>
          ) : null}
        </section>

        {error ? <ApiErrorMessage error={error} /> : null}
        {status ? (
          <p className="panel-success" role="status">
            {status}
          </p>
        ) : null}

        <div className="location-catalog-heading">
          <div>
            <p className="eyebrow">Catalog</p>
            <h3>{searchSummary}</h3>
          </div>
        </div>

        <div className="location-option-list" aria-label="위치 목록">
          {optionsLoading ? (
            <p className="muted">위치 목록을 불러오고 있어요.</p>
          ) : options.length > 0 ? (
            options.map((option) => {
              const selected = location?.code === option.code;
              const saving = savingCode === option.code;

              return (
                <article
                  className={selected ? 'location-option-card selected' : 'location-option-card'}
                  key={option.code}
                >
                  <div className="location-option-main">
                    <strong>{option.name}</strong>
                    <span>{option.fullName}</span>
                    <span>catalog code {option.code}</span>
                    <span>
                      KMA 격자 nx={option.nx}, ny={option.ny}
                    </span>
                  </div>
                  <button
                    className={selected ? 'primary-button' : 'secondary-button'}
                    type="button"
                    onClick={() => void handleSelect(option, 'MANUAL_SEARCH')}
                    disabled={selected || savingCode !== null}
                  >
                    {selected ? '현재 위치' : saving ? '저장 중' : '이 위치 선택'}
                  </button>
                </article>
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
