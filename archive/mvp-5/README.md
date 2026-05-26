# SmartCloset MVP 5 Archive

이 문서는 SmartCloset 5차 MVP를 이해하기 위한 최소 요약이다.

- 아카이브 정리 시점: 2026-05-26
- 완료 시점: 2026-05-25T21:01:54+0900
- 상태: 옷 이미지 업로드 완료 후 MVP6 추천 피드백/개인화 문서 전환
- 목표: 사용자가 옷 1개당 이미지 1장을 업로드, 교체, 삭제하고, 옷 목록과 추천 결과와 추천 이력에서 썸네일을 확인할 수 있게 한다.
- 최종 범위: 이미지 메타데이터 컬럼, 로컬 파일 저장, 파일 검증, 보호 이미지 API, 기본 옷 프리셋 이미지, 옷/추천 DTO image metadata, Closet 이미지 UX, 추천 결과/이력 썸네일, Docker Compose image volume

현재 구현 기준 문서는 루트 `README.md`와 `docs/` 아래 문서다. 이 archive는 과거 MVP 맥락 확인용이며 구현 source of truth가 아니다.

## 관련 링크

- 현재 PRD/MVP6 틀: ../../docs/PRD.md
- 현재 API 문서: ../../docs/API.md
- 현재 프론트 문서: ../../docs/FRONTEND.md
- 현재 추천 규칙: ../../docs/RECOMMENDATION_RULES.md
- ADR 인덱스: ../../docs/ADR.md
- MVP 5 ADR: ../../docs/adr/010-mvp5-clothing-images.md
- MVP 5 phase 기록: ../../phases/5-smartcloset-clothing-images/README.md
- MVP 5 QA 기록: ../../docs/qa/mvp5-clothing-images-qa.md
- MVP 5 요약: SUMMARY.md
- MVP 5 결정: DECISIONS.md
- MVP 5 변경 이력: CHANGELOG.md
