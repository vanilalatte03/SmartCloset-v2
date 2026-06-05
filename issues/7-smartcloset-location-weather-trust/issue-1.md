# Issue 1: 7-smartcloset-location-weather-trust step 1 자동 리뷰 실패 1

## 발생 위치
- Phase: 7-smartcloset-location-weather-trust
- Step: 1 `kma-location-catalog`
- PR: https://github.com/vanilalatte03/smart-closet/pull/94

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
- src/main/resources/kma-location-catalog.csv:1 only adds a 32-row data set: the 9 legacy representative cities plus a small Goyang/Ilsan subset. Step 1 requires converting the KMA short-range forecast grid latitude/longitude data into an application resource/generated source data and replacing representative-city search with KMA administrative catalog search. With this sample, most 읍/면/동 searches return no candidates, so the Step 1 P0 catalog contract is not met.
- src/main/java/com/smartcloset/location/domain/LocationCatalog.java:37 and src/main/java/com/smartcloset/location/domain/LocationCatalog.java:70 make numeric keywords match every row. `compact()` removes digits, so a keyword like `4128551000` becomes an empty compact keyword, and `String.contains("")` is true for all compacted name/fullName/region fields. Since the API contract says keyword searches include code, numeric KMA code searches should not return the full catalog.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
