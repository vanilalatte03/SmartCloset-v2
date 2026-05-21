# MVP 1.5 Changelog

## 2026-05-21
- 1.5차 MVP 문서를 2차 위치/프론트 기준으로 전환했다.
- 1.5차 MVP 전체 문서 복사본을 남기지 않고, 과거 맥락 확인용 최소 archive 요약으로 정리했다.

## MVP 1.5 종료 시점 주요 변경
- KMA `getVilageFcst` JSON weather provider를 추가했다.
- KMA base date/time 계산과 forecast group 선택 규칙을 정리했다.
- `TMP`, `SKY`, `PTY`, `PCP`, `WSD` category 매핑을 구현 기준으로 확정했다.
- fallback/strict KMA mode를 문서화했다.
- 추천 생성 API 계약을 유지했다.
- Docker Compose는 서비스키 없이도 fallback 데모가 가능하도록 유지했다.
- 사용자별 위치 저장과 정식 프론트엔드 앱은 2차 MVP 범위로 넘겼다.
