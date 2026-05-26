# MVP 5 Summary

## 구현된 기능

- 옷 1개당 이미지 1장 업로드/교체/조회/삭제
- 이미지 파일 검증: 5MB 이하 jpg/jpeg/png/webp
- 원본 파일명을 사용하지 않는 UUID 기반 파일 저장
- Docker Compose image volume
- `clothing_items` image metadata 컬럼
- 보호 이미지 API
- 기본 옷 프리셋 5개와 번들 상품컷 이미지
- `ClothingResponse.image`
- 추천 결과와 추천 이력 outfit item image metadata
- Closet 이미지 업로드/교체/삭제 UX
- 추천 결과와 History 썸네일 표시
- 이미지 없는 옷 fallback visual

## 유지된 기능

- Spring Boot 4.0.6, Java 21, MySQL
- Spring Security + JWT Bearer access token
- 공개 API `POST /api/auth/signup`, `POST /api/auth/login`
- 인증 사용자 기준 옷장/위치/선호도/추천 이력/착용 이력 격리
- KMA `getVilageFcst` JSON weather provider와 fallback weather
- React+Vite+TypeScript 프론트엔드
- `sessionStorage` token 저장
- 규칙 기반 추천 점수 100점 체계
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`

## 제외된 기능

- AI/GPT 추천
- AI 자동 태깅
- 다중 이미지 업로드
- 이미지 편집/크롭/압축 파이프라인
- EXIF 분석
- image moderation
- S3/CDN
- 이미지 기반 추천 점수 또는 추천 이유
- refresh token
- social login
- Redis
- AWS 배포와 CD 자동화
- styleTags 점수 반영

## 데모 시나리오 요약

- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- React 앱에서 회원가입 또는 로그인을 수행한다.
- Closet에서 기본 옷 프리셋과 썸네일을 확인한다.
- 새 옷을 등록하고 이미지를 업로드한다.
- 이미지를 교체하거나 삭제한다.
- 추천을 생성하고 추천 결과의 썸네일을 확인한다.
- 추천 이력에서 썸네일과 착용 상태를 확인한다.
