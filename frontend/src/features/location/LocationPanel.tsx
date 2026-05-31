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
import { WeatherBadge } from '../../components/DisplayTokens';
import type {
  ErrorResponse,
  LocationOptionResponse,
  LocationSource,
  UserLocationResponse,
  WeatherResponse,
} from '../../types/api';
import { locationSourceLabels } from '../../utils/displayMappings';

const locationCatalogApiPath = '/api/locations?keyword={keyword}';
const locationResolveApiPath = '/api/locations/resolve';
const locationWeatherApiPath = '/api/weather/current';
const locationOptionsPageSize = 3;
const locationNameCollator = new Intl.Collator('ko-KR', {
  numeric: true,
  sensitivity: 'base',
});

type LocationPanelProps = {
  accessToken: string;
  location: UserLocationResponse | null;
  loading: boolean;
  onAuthExpired: () => void;
  onLocationChange: (location: UserLocationResponse) => void;
};

type LocationDisplaySource = Pick<
  LocationOptionResponse,
  'name' | 'fullName' | 'region1' | 'region2' | 'region3'
>;

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
  const [locationPage, setLocationPage] = useState(1);
  const [optionsLoading, setOptionsLoading] = useState(true);
  const [weather, setWeather] = useState<WeatherResponse | null>(null);
  const [weatherLoading, setWeatherLoading] = useState(true);
  const [weatherError, setWeatherError] = useState<ErrorResponse | null>(null);
  const [savingCode, setSavingCode] = useState<string | null>(null);
  const [resolveOptions, setResolveOptions] = useState<LocationOptionResponse[]>([]);
  const [resolveLoading, setResolveLoading] = useState(false);
  const [resolveStatus, setResolveStatus] = useState<string | null>(null);
  const [resolveError, setResolveError] = useState<ErrorResponse | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const searchSummary = submittedKeyword
    ? `"${submittedKeyword}" 검색 결과`
    : '전체 대표 위치';
  const locationPageCount = Math.max(1, Math.ceil(options.length / locationOptionsPageSize));
  const activeLocationPage = Math.min(locationPage, locationPageCount);
  const locationPageStartIndex = (activeLocationPage - 1) * locationOptionsPageSize;
  const visibleLocationOptions = options.slice(
    locationPageStartIndex,
    locationPageStartIndex + locationOptionsPageSize
  );
  const locationPaginationLabel = `${activeLocationPage}/${locationPageCount}`;

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
      setOptions(sortLocationOptionsForKeyword(nextOptions, submittedKeyword));
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

  useEffect(() => {
    setLocationPage(1);
  }, [submittedKeyword]);

  useEffect(() => {
    setLocationPage((currentPage) => Math.min(currentPage, locationPageCount));
  }, [locationPageCount]);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmittedKeyword(keyword.trim());
    setLocationPage(1);
  };

  const handleShowAll = () => {
    setKeyword('');
    setSubmittedKeyword('');
    setLocationPage(1);
  };

  const handleSelect = async (
    option: LocationOptionResponse,
    source: LocationSource = 'MANUAL_SEARCH'
  ) => {
    setSavingCode(option.code);
    setError(null);

    try {
      const updatedLocation = await updateUserLocation(accessToken, {
        locationCode: option.code,
        source,
      });
      onLocationChange(updatedLocation);
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
                ? '가까운 동네 후보를 찾았습니다. 원하는 동네를 선택해 저장해주세요.'
                : '가까운 동네 후보를 찾지 못했습니다. 동네 검색으로 선택해주세요.'
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

  const locationDisplayName = location ? formatLocationFullName(location) : '위치 확인 중';
  const locationUpdatedLabel = location ? formatUpdatedAt(location.updatedAt) : null;

  return (
    <article className="location-panel">
      <section className="location-hero" aria-label="저장된 위치 요약">
        <div className="location-hero-copy">
          <p className="eyebrow">추천 기준 동네</p>
          <h2>{locationDisplayName}</h2>
          <p>오늘 추천과 기록은 저장된 동네의 날씨를 기준으로 계산됩니다.</p>
          {loading ? <p className="location-hero-loading">위치를 확인하고 있어요.</p> : null}
          {location ? (
            <div className="location-hero-tags" aria-label="위치 기준">
              <span>{locationSourceLabels[location.source]}</span>
              {locationUpdatedLabel ? <span>{locationUpdatedLabel} 업데이트</span> : null}
              <span>추천에 반영 중</span>
            </div>
          ) : null}
        </div>

        <div
          className="location-weather-glance"
          aria-label="현재 날씨 요약"
          data-api-path={locationWeatherApiPath}
        >
          <div className="location-weather-heading">
            <div>
              <p className="eyebrow">오늘 날씨</p>
              <h3>{location ? `${location.name} 기준` : '저장 위치 기준'}</h3>
            </div>
            <button
              className="secondary-button location-weather-refresh"
              type="button"
              onClick={() => void loadCurrentWeather()}
              disabled={weatherLoading}
            >
              새로고침
            </button>
          </div>

          {weatherLoading ? <p>현재 날씨를 확인하고 있어요.</p> : null}
          {!weatherLoading && weather ? (
            <div className="location-weather-summary">
              <WeatherBadge weather={weather} />
              <p>겉옷과 비 대비 여부를 고를 때 이 날씨를 함께 봅니다.</p>
            </div>
          ) : null}
          {!weatherLoading && weatherError ? (
            <div className="location-weather-fallback">
              <ApiErrorMessage error={weatherError} />
              <p>날씨 요약이 없어도 위치 검색과 선택은 계속 사용할 수 있어요.</p>
            </div>
          ) : null}
        </div>
      </section>

      {error ? <ApiErrorMessage error={error} /> : null}
      <div className="location-action-grid">
        <section
          className="panel location-search-card"
          aria-label="동네 검색"
          data-api-path={locationCatalogApiPath}
        >
          <div className="section-title-row">
            <div>
              <p className="eyebrow">동네 검색</p>
              <h2>저장할 동네 선택</h2>
              <p className="muted location-panel-copy">
                동네 이름으로 검색하고 추천 기준으로 사용할 위치를 저장합니다.
              </p>
            </div>
          </div>

          <form className="inline-form location-search-form" onSubmit={handleSearch}>
            <label className="field">
              <span>검색어</span>
              <input
                type="search"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="예: 일산동, 역삼동, 서초구"
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

          <div className="location-catalog-heading">
            <div>
              <p className="eyebrow">검색 결과</p>
              <h3>{searchSummary}</h3>
            </div>
            {!optionsLoading && options.length > 0 ? (
              <div className="location-pagination" aria-label="위치 목록 페이지 이동">
                <span className="location-page-range" aria-live="polite">
                  {locationPaginationLabel}
                </span>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => setLocationPage((currentPage) => Math.max(1, currentPage - 1))}
                  disabled={activeLocationPage === 1}
                >
                  이전
                </button>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() =>
                    setLocationPage((currentPage) =>
                      Math.min(locationPageCount, currentPage + 1)
                    )
                  }
                  disabled={activeLocationPage === locationPageCount}
                >
                  다음
                </button>
              </div>
            ) : null}
          </div>

          <div className="location-option-list" aria-label="위치 목록">
            {optionsLoading ? (
              <p className="muted">위치 목록을 불러오고 있어요.</p>
            ) : options.length > 0 ? (
              visibleLocationOptions.map((option) => {
                const selected = location?.code === option.code;
                const saving = savingCode === option.code;

                return (
                  <article
                    className={selected ? 'location-option-card selected' : 'location-option-card'}
                    key={option.code}
                  >
                    <div className="location-option-main">
                      <strong>{option.name}</strong>
                      <span>{formatLocationFullName(option)}</span>
                      <span>
                        {selected
                          ? '지금 추천 기준으로 쓰는 동네입니다.'
                          : '선택하면 추천과 날씨 기준이 이 동네로 바뀝니다.'}
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
                검색어와 일치하는 위치가 없어요. 도시명이나 동네명으로 다시 검색해주세요.
              </p>
            )}
          </div>
        </section>

        <section
          className="panel location-resolve-card"
          aria-label="현재 위치 후보"
          data-api-path={locationResolveApiPath}
        >
          <div className="section-title-row">
            <div>
              <p className="eyebrow">현재 위치 후보</p>
              <h2>가까운 동네 찾기</h2>
              <p className="muted location-panel-copy">
                브라우저 위치로 가까운 행정구역 후보를 찾고, 선택한 동네만 저장합니다.
              </p>
            </div>
            <button
              className="primary-button"
              type="button"
              onClick={handleResolveCurrentLocation}
              disabled={resolveLoading || savingCode !== null}
            >
              {resolveLoading ? '확인 중' : '현재 위치로 찾기'}
            </button>
          </div>

          <div className="location-coordinate-note" role="note">
            <strong>좌표는 저장하지 않아요</strong>
            <span>브라우저 좌표는 후보를 찾을 때만 쓰고, 계정에는 선택한 동네만 남습니다.</span>
          </div>

          {resolveStatus ? (
            <p className="panel-success location-resolve-status" role="status">
              {resolveStatus}
            </p>
          ) : null}
          {resolveError ? <ApiErrorMessage error={resolveError} /> : null}

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
                      <span>{formatLocationFullName(option)}</span>
                      <span>
                        {selected
                          ? '지금 추천 기준으로 쓰는 동네입니다.'
                          : '현재 위치에서 가까운 후보입니다.'}
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
          ) : (
            <p className="muted location-resolve-empty">
              버튼을 누르면 가까운 동네 후보가 여기에 표시됩니다.
            </p>
          )}
        </section>
      </div>
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

function formatLocationFullName(location: LocationDisplaySource): string {
  const parts = [
    location.region1,
    formatCityDistrict(location.region2),
    location.region3,
  ].filter((part): part is string => Boolean(part));

  if (parts.length > 0) {
    return parts.join(' ');
  }

  return addCityDistrictSpacing(location.fullName || location.name);
}

function formatCityDistrict(value: string | null): string | null {
  if (!value) {
    return null;
  }

  return addCityDistrictSpacing(value);
}

function addCityDistrictSpacing(value: string): string {
  const trimmed = value.trim().replace(/\s+/g, ' ');
  const cityDistrictMatch = trimmed.match(/^(.+시)(.+[구군])$/);
  if (!cityDistrictMatch) {
    return trimmed;
  }

  return `${cityDistrictMatch[1]} ${cityDistrictMatch[2]}`;
}

function sortLocationOptionsForKeyword(
  options: LocationOptionResponse[],
  keyword: string
): LocationOptionResponse[] {
  const normalizedKeyword = normalizeLocationSearchText(keyword);
  if (!normalizedKeyword) {
    return options;
  }

  return options
    .map((option, index) => ({
      option,
      index,
      rank: getLocationSearchRank(option, normalizedKeyword),
    }))
    .sort((left, right) => {
      if (left.rank !== right.rank) {
        return left.rank - right.rank;
      }

      const nameCompare = locationNameCollator.compare(left.option.name, right.option.name);
      if (nameCompare !== 0) {
        return nameCompare;
      }

      const fullNameCompare = locationNameCollator.compare(
        left.option.fullName,
        right.option.fullName
      );
      if (fullNameCompare !== 0) {
        return fullNameCompare;
      }

      return left.index - right.index;
    })
    .map(({ option }) => option);
}

function getLocationSearchRank(option: LocationOptionResponse, keyword: string): number {
  const isNeighborhoodSearch = keyword.endsWith('동') && !keyword.endsWith('동구');
  const neighborhoodName = option.region3
    ? normalizeLocationSearchText(option.region3)
    : isNeighborhoodSearch
      ? ''
      : normalizeLocationSearchText(option.name);
  const neighborhoodRank = rankLocationText(neighborhoodName, keyword);
  if (neighborhoodRank < 99) {
    return neighborhoodRank;
  }

  const nameRank = rankLocationText(normalizeLocationSearchText(option.name), keyword);
  if (nameRank < 99) {
    return 10 + nameRank;
  }

  const region2Rank = rankLocationText(normalizeLocationSearchText(option.region2), keyword);
  if (region2Rank < 99) {
    return 20 + region2Rank;
  }

  const fullNameRank = rankLocationText(normalizeLocationSearchText(option.fullName), keyword);
  if (fullNameRank < 99) {
    return 30 + fullNameRank;
  }

  return 99;
}

function rankLocationText(value: string, keyword: string): number {
  if (!value) {
    return 99;
  }

  if (value === keyword) {
    return 0;
  }

  const compactValue = removeLocationSearchDigits(value);
  const compactKeyword = removeLocationSearchDigits(keyword);
  if (compactKeyword && compactValue === compactKeyword) {
    return 1;
  }

  if (value.startsWith(keyword)) {
    return 2;
  }

  if (compactKeyword && compactValue.startsWith(compactKeyword)) {
    return 3;
  }

  if (value.includes(keyword)) {
    return 4;
  }

  if (compactKeyword && compactValue.includes(compactKeyword)) {
    return 5;
  }

  return 99;
}

function normalizeLocationSearchText(value: string | null): string {
  return (value ?? '').trim().replace(/\s+/g, '').toLocaleLowerCase('ko-KR');
}

function removeLocationSearchDigits(value: string): string {
  return value.replace(/\d+/g, '');
}
