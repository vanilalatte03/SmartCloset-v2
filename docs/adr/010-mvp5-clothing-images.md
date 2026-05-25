# MVP5를 옷 이미지 업로드 MVP로 정의

## 상태
승인됨

## 맥락

MVP4에서는 인증 사용자 baseline 위에 Today, Closet, Preferences, Location, History 중심 반응형 웹 UX를 완성했다. 사용자는 로그인 후 자신의 위치와 날씨를 확인하고, 옷을 빠르게 등록하고, 추천 결과와 이유를 이해할 수 있다.

남은 제품 리스크는 옷과 추천 결과가 여전히 텍스트, enum 라벨, 색상 swatch 중심이라는 점이다. 실제 옷을 빠르게 알아보려면 옷별 이미지가 필요하다.

다만 이미지 업로드는 저장소, 파일 검증, 접근 권한, Docker Compose 공유 방식까지 함께 결정해야 한다. AI 자동 태깅이나 이미지 기반 추천 점수까지 같이 넣으면 MVP5 범위가 흔들린다.

## 결정

MVP5는 옷 이미지 업로드 MVP다.

- 옷 1개당 이미지 1장만 지원한다.
- 기존 `POST /api/clothes`, `PUT /api/clothes/{clothingId}` JSON API는 유지한다.
- 이미지 업로드와 교체는 별도 보호 API `PUT /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 조회는 보호 API `GET /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 삭제는 보호 API `DELETE /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 API는 모두 `Authorization: Bearer {accessToken}`을 요구한다.
- 다른 사용자 옷 이미지에는 접근할 수 없다.
- 파일 bytes는 DB가 아니라 로컬 파일 시스템 또는 Docker Compose volume에 저장한다.
- `clothing_items`에는 nullable 이미지 메타데이터 컬럼을 둔다.
- 옷 목록, 옷 상세, 추천 결과, 추천 이력 DTO에는 nullable image metadata를 포함한다.
- 프론트는 보호 이미지 API를 사용하므로 Authorization header로 blob을 가져와 object URL로 표시한다.

파일 검증 기준:

- 최대 크기 5MB
- 허용 확장자 `.jpg`, `.jpeg`, `.png`, `.webp`
- 허용 MIME type `image/jpeg`, `image/png`, `image/webp`
- 원본 파일명은 저장 경로에 사용하지 않는다.
- 서버가 생성한 UUID 기반 저장 파일명을 사용한다.

## 결과

- 기존 JSON 옷 API의 호환성을 유지하면서 이미지 기능을 추가할 수 있다.
- 이미지 접근은 인증/소유권 경계 안에 머문다.
- Docker Compose 공유 환경에서 app 재시작 후 이미지 유지 여부를 검증할 수 있다.
- 이미지가 없는 옷도 기존 category visual, 색상 swatch, 소재 chip으로 표시할 수 있다.
- 추천 도메인은 이미지 존재 여부와 무관하게 기존 규칙 기반 점수 체계를 유지한다.

## 범위 제외

- AI 자동 태깅
- AI/GPT 추천
- 다중 이미지 업로드
- 이미지 편집, 크롭, 리사이즈, 압축 파이프라인
- EXIF 분석
- 이미지 moderation
- S3, CDN, 외부 image hosting
- 이미지 기반 추천 점수 변경
- 이미지 기반 추천 이유 생성
- refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- Redis
- AWS 배포와 CD 자동화
