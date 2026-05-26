# MVP 5 Decisions

MVP 5의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정

- MVP5는 옷 이미지 업로드 MVP로 정의했다.
- 자세한 내용은 ../../docs/adr/010-mvp5-clothing-images.md 를 따른다.
- 기존 `POST /api/clothes`, `PUT /api/clothes/{clothingId}` JSON API를 multipart로 바꾸지 않았다.
- 이미지 업로드/교체는 `PUT /api/clothes/{clothingId}/image`로 분리했다.
- 이미지 조회는 `GET /api/clothes/{clothingId}/image` 보호 API로 처리했다.
- 이미지 삭제는 `DELETE /api/clothes/{clothingId}/image`로 처리했다.
- 옷 1개당 이미지는 1장만 허용했다.
- 파일 bytes는 DB가 아니라 로컬 파일 시스템 또는 Docker Compose volume에 저장했다.
- DB에는 `clothing_items` image metadata만 저장했다.
- 기본 옷 프리셋 5개와 번들 상품컷 이미지를 추가했다.
- 추천 점수와 추천 이유에는 이미지 metadata를 반영하지 않았다.
- styleTags는 MVP5까지 저장/조회/표시만 하고 점수에는 반영하지 않았다.
- AI 자동 태깅, 다중 이미지, S3/CDN, 이미지 기반 추천 점수는 MVP5 범위에서 제외했다.
