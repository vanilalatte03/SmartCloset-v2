# MVP 5 Changelog

## 2026-05-26

- 5차 MVP 문맥을 MVP6 추천 피드백/개인화 문서 작성 준비를 위해 archive로 정리했다.
- 5차 MVP 전체 문서 복사본을 남기지 않고, 과거 맥락 확인용 최소 archive 요약으로 정리했다.

## MVP 5 종료 시점 주요 변경

- 옷 이미지 업로드/교체/조회/삭제 보호 API를 추가했다.
- 이미지 파일 검증과 UUID 기반 로컬 파일 저장을 추가했다.
- Docker Compose image volume을 추가했다.
- `clothing_items` image metadata 컬럼을 추가했다.
- 기본 옷 프리셋 5개와 번들 상품컷 이미지를 추가했다.
- `ClothingResponse`와 추천 outfit item DTO에 nullable image metadata를 추가했다.
- Closet view에 이미지 업로드, 교체, 삭제 UX를 추가했다.
- Today 추천 결과와 History 추천 이력에 썸네일을 표시했다.
- 이미지 없는 옷 fallback visual을 유지했다.
- 이미지 metadata가 추천 점수와 추천 이유에 영향을 주지 않도록 유지했다.
