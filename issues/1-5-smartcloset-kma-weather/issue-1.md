# Issue 1: 1-5-smartcloset-kma-weather step 2 자동 리뷰 실패 1

## 발생 위치
- Phase: 1-5-smartcloset-kma-weather
- Step: 2 `kma-http-client`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/11

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
- src/main/java/com/smartcloset/weather/infrastructure/kma/KmaVilageForecastClient.java:49 builds the KMA URI with queryParam("serviceKey", properties.serviceKey()).build().toUri() without a defined/tested encoding policy. data.go.kr service keys commonly contain reserved characters such as '+', and this construction can send decoded keys incorrectly or double-encode already encoded keys, causing valid KMA_SERVICE_KEY values to fail real getVilageFcst authentication and fall back/500 instead of exercising the 1.5 KMA success path. Add explicit serviceKey encoding handling and tests with realistic decoded/encoded keys.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
