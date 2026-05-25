# PRD: SmartCloset MVP5 옷 이미지 업로드

## 문서 목적

이 문서는 SmartCloset MVP5의 확정 범위를 정의한다. MVP5는 MVP4에서 완성한 인증 사용자 기반 반응형 웹 UX 위에, 사용자가 직접 등록한 옷 이미지를 업로드하고 추천 결과에서 썸네일로 확인할 수 있게 만드는 단계다.

현재 구현 baseline은 MVP4 완료 상태다. 회원가입/로그인, JWT Bearer access token, 인증 사용자 기준 옷장/위치/선호도/추천 이력/착용 이력 분리, `GET /api/weather/current`, React `sessionStorage` 세션, Today/Closet/Preferences/Location/History 반응형 UX는 이미 구현되어 있다.

## MVP5 한 줄 정의

사용자가 옷 1개당 이미지 1장을 등록하고, 옷장과 추천 결과에서 실제 옷 썸네일을 확인할 수 있게 한다.

신규 가입자와 옷이 0개인 기존 계정은 기본 옷 프리셋 5개와 상품컷 이미지를 자동으로 받아 첫 추천까지 더 빠르게 도달할 수 있다.

## 목표

- 옷장을 텍스트와 enum 중심 목록에서 실제 옷 이미지 중심 경험으로 개선한다.
- 추천 결과에서 사용자가 어떤 옷을 입을지 더 빠르게 알아볼 수 있게 한다.
- Docker Compose 로컬 공유 환경에서 이미지 저장까지 재현 가능하게 한다.
- AI 자동 태깅 없이 기존 수동 입력 방식을 유지한다.

## 현재 Baseline

- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 그 외 API는 `Authorization: Bearer {accessToken}` header를 요구한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 옷 등록/수정 API는 JSON `ClothingRequest`를 사용한다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약 API는 `GET /api/weather/current`이며 보호 API다.
- 프론트 access token 저장 위치는 `sessionStorage`다.
- 추천 점수는 규칙 기반 100점 체계이며 `preferenceScore`는 선호 색상/소재만 반영한다.
- `styleTags`는 저장/조회/표시만 하며 추천 점수와 추천 이유에는 반영하지 않는다.
- 외부 Weather API는 기상청 단기예보 `getVilageFcst` JSON 연동만 사용한다.
- 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용한다.
- Docker Compose 공유 방식을 유지한다.

## 해결하려는 문제

- 옷 목록에서 이름, 색상, 소재만으로 실제 옷을 구분해야 한다.
- 추천 결과에 표시되는 옷 조합이 텍스트 중심이라 사용자가 실제 착장으로 연결하기 어렵다.
- 사용자가 이미 등록한 옷을 수정할 때 이미지 교체나 삭제 흐름이 없다.
- 공유 환경에서 이미지 저장 방식이 정의되어 있지 않아 후속 구현자가 파일 저장 경로, 접근 권한, 검증 기준을 임의로 정할 수 있다.
- 신규 사용자는 처음 로그인했을 때 옷장이 비어 있어 이미지 UX와 추천 흐름을 체험하기 전 수동 등록 부담이 크다.

## 핵심 사용자 시나리오

1. 사용자가 로그인 후 Closet view에서 옷을 등록한다.
2. 사용자가 등록한 옷에 이미지 1장을 업로드한다.
3. 옷 목록 카드에 업로드한 이미지 썸네일이 표시된다.
4. 사용자가 옷 이미지를 교체하거나 삭제한다.
5. 사용자가 추천을 생성하면 추천 결과의 상의/하의/아우터 카드에 썸네일이 표시된다.
6. 추천 이력에서도 추천 당시 포함된 옷의 현재 이미지 상태를 확인한다.

기본 프리셋 시나리오:

1. 신규 사용자가 회원가입한다.
2. 서버가 현재 사용자 소유 기본 옷 5개와 이미지 metadata를 생성한다.
3. 사용자는 별도 등록 없이 Closet view에서 기본 옷 썸네일을 확인한다.
4. 옷이 0개인 기존 계정은 다음 로그인 시 같은 프리셋을 한 번만 받는다.

## MVP5 우선순위

### P0: 이미지 저장과 보호 API

- 옷 1개당 이미지 1장만 허용한다.
- 기존 `POST /api/clothes`, `PUT /api/clothes/{clothingId}` JSON 계약은 유지한다.
- 별도 보호 API로 이미지 업로드, 조회, 삭제를 제공한다.
- 모든 이미지 API는 인증 사용자 소유 옷만 접근할 수 있다.
- 이미지 조회는 public static path가 아니라 보호 API로 제공한다.
- 로컬 파일 저장과 Docker Compose volume 저장 기준을 문서화한다.

### P0: 파일 검증

- 최대 파일 크기는 5MB다.
- 허용 확장자는 `.jpg`, `.jpeg`, `.png`, `.webp`다.
- 허용 MIME type은 `image/jpeg`, `image/png`, `image/webp`다.
- 원본 파일명은 저장 경로에 사용하지 않는다.
- 저장 파일명은 서버가 생성한 UUID 기반 이름을 사용한다.
- 잘못된 파일은 `400 INVALID_REQUEST`로 실패한다.

### P0: 썸네일 UX

- Closet 옷 목록과 수정 패널에서 이미지 미리보기를 제공한다.
- 추천 결과의 outfit slot 카드에 썸네일을 표시한다.
- 추천 이력의 outfit summary에 썸네일을 표시한다.
- 이미지가 없으면 기존 category glyph, 색상 swatch, 소재 chip으로 fallback한다.
- 모바일 375px에서 썸네일, 버튼, 텍스트가 겹치지 않아야 한다.

### P1: 사용성 보강

- 업로드 진행/성공/실패 상태를 한국어 문장으로 표시한다.
- 교체와 삭제 액션을 수정 흐름에서 명확히 분리한다.
- 이미지 삭제는 idempotent하게 처리한다.
- 인증 만료 시 이미지 blob fetch도 기존 인증 만료 흐름으로 연결한다.
- 신규/빈 계정에는 기본 옷 프리셋을 자동 제공해 첫 화면의 빈 상태를 줄인다.

## 포함 범위

- 이미지 메타데이터 컬럼 추가
- 로컬 파일 저장 service
- 이미지 검증
- 이미지 업로드/조회/삭제 보호 API
- 기본 옷 프리셋 5개와 번들 상품컷 이미지
- 옷/추천 DTO에 nullable 이미지 메타데이터 추가
- 프론트 API client에 multipart upload와 authenticated blob fetch 추가
- Closet, Recommendation, History 썸네일 표시
- Docker Compose volume 기반 공유 문서화
- MVP5 phase 문서와 데모 시나리오 작성

## 제외 범위

- AI 자동 태깅
- AI/GPT 추천
- 다중 이미지 업로드
- 이미지 편집, 크롭, 리사이즈, 압축 파이프라인
- EXIF 기반 위치/시간 분석
- S3, CDN, 외부 image hosting
- 관리자 이미지 관리 기능
- 이미지 moderation
- 이미지 기반 추천 점수 변경
- refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- 외부 주소/지도 검색 API
- Redis
- AWS 배포와 CD 자동화
- native app/PWA 출시

## API 변경 계획

MVP5는 새 공개 API를 추가하지 않는다. 이미지 API는 모두 보호 API다.

추가할 보호 API:

- `PUT /api/clothes/{clothingId}/image`
- `GET /api/clothes/{clothingId}/image`
- `DELETE /api/clothes/{clothingId}/image`

원칙:

- 기존 JSON 옷 등록/수정 API는 유지한다.
- 이미지 업로드는 `multipart/form-data`를 사용한다.
- multipart part name은 `image`로 고정한다.
- 성공 응답 중 JSON API는 기존 `{ "data": ... }` envelope를 유지한다.
- 이미지 bytes 조회 응답은 파일 stream과 `Content-Type` header를 반환한다.
- 현재 사용자 전용 DTO에 `userId`를 되살리지 않는다.
- 공개 `userId` query parameter를 추가하지 않는다.

## 데이터/ERD 변경 계획

`clothing_items`에 nullable 이미지 메타데이터 컬럼을 추가한다.

- `image_stored_filename`
- `image_content_type`
- `image_size_bytes`
- `image_uploaded_at`

별도 이미지 테이블은 만들지 않는다. 옷 1개당 이미지 1장 정책이므로 clothing item row에 메타데이터를 둔다.

파일 bytes는 DB가 아니라 로컬 파일 시스템 또는 Docker volume에 저장한다.

## 프론트엔드 변경 계획

Closet view:

- 옷 카드에 썸네일 영역을 추가한다.
- 이미지가 있으면 authenticated blob fetch로 object URL을 만들고 표시한다.
- 이미지가 없으면 기존 category visual을 표시한다.
- 등록 후 이미지 업로드를 이어서 수행할 수 있다.
- 수정 중 이미지 교체와 삭제를 수행할 수 있다.

Recommendation/History view:

- 추천 outfit item에 nullable image metadata를 반영한다.
- 썸네일이 있으면 표시하고, 없으면 swatch/chip 중심 fallback을 유지한다.

프론트 API:

- JSON API helper는 유지한다.
- multipart upload 함수는 `Content-Type`을 직접 지정하지 않고 `FormData`를 전달한다.
- 이미지 조회 함수는 `Authorization` header를 붙여 blob을 가져온다.
- object URL은 컴포넌트 unmount 또는 이미지 변경 시 revoke한다.

## 추천 규칙 변경 계획

추천 규칙은 변경하지 않는다.

- 이미지 존재 여부는 candidate filtering에 영향을 주지 않는다.
- 이미지 존재 여부는 score와 tie-break에 영향을 주지 않는다.
- 추천 이유에는 이미지 업로드 여부를 포함하지 않는다.
- `styleTags`는 계속 저장/조회/표시만 한다.

## 완료 기준

- 로그인한 사용자가 자신의 옷에 이미지 1장을 업로드할 수 있다.
- 신규 가입자와 옷이 0개인 기존 로그인 사용자는 기본 옷 5개와 이미지 metadata를 받는다.
- 옷 이미지 교체와 삭제가 가능하다.
- 다른 사용자의 옷 이미지에 접근하면 기존 소유권 정책대로 실패한다.
- 잘못된 파일 크기, 확장자, MIME type은 실패한다.
- 옷 목록에 썸네일이 표시된다.
- 추천 결과와 추천 이력에 썸네일이 표시된다.
- 이미지가 없는 옷도 기존 fallback UI로 자연스럽게 표시된다.
- Docker Compose 환경에서 app 재시작 후 업로드 이미지가 유지된다.
- AI 자동 태깅이나 이미지 기반 추천 점수 변경이 포함되지 않는다.

## 테스트/검증 기준

- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- Docker Compose 실행 후 이미지 업로드, 교체, 삭제, 추천 썸네일 표시 확인
- 모바일 375px에서 Closet, Today 추천 결과, History 화면 overflow 확인

## 결정 완료 사항

- 이미지 API 형태: 기존 JSON 옷 API 유지 + 별도 보호 이미지 API
- 이미지 접근: 보호 API에서 인증/소유권 확인 후 bytes 반환
- 파일 제한: 5MB, jpg/jpeg/png/webp
- 저장 방식: MVP5는 Docker Compose 로컬 볼륨 기반 파일 저장부터 시작
- AI 자동 태깅: MVP5 제외
