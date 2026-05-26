# Issue 2: 7-smartcloset-location-weather-trust step 3 자동 리뷰 실패 2

## 발생 위치
- Phase: 7-smartcloset-location-weather-trust
- Step: 3 `weather-source-snapshot`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/97

## 재현 명령
```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 핵심 에러
## 자체 리뷰

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 로컬 검증 | 통과 | docs/COMMANDS.md 기준 명령 |
| diff 검사 | 통과 | git diff --check |
| 금지 범위 | 통과 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 발견사항
- `src/main/java/com/smartcloset/weather/domain/WeatherSource.java:43` and `src/main/java/com/smartcloset/weather/infrastructure/kma/KmaVilageForecastWeatherProvider.java:133`: fallback source metadata always sets `forecastDate` and `forecastTime` to null. Step 3 explicitly requires KMA success and fallback to fill `provider`, `kmaUsed`, `fallbackUsed`, `baseDate`, `baseTime`, `forecastDate`, and `forecastTime`, and MVP7 requires users to see the actual forecast 기준 시각. The current tests also lock in the wrong contract by expecting null fallback forecast fields in `CurrentWeatherControllerTest`. Compute and return the selected fallback forecast date/time for the requested `ForecastPeriod` instead of null.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
