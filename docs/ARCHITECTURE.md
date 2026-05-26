# 아키텍처: SmartCloset MVP5

## 전체 아키텍처 개요

SmartCloset MVP5는 Spring Boot 4.0.6 백엔드와 React+Vite+TypeScript 프론트엔드 앱으로 구성한다. MVP5의 변경 지점은 옷 이미지 저장, 검증, 보호 API, 썸네일 표시다.

기존 인증, 추천, KMA weather, 위치 catalog, 선호도, 추천 이력 구조는 유지한다. 신규/빈 계정 기본 옷 프리셋은 인증 흐름 안에서 현재 사용자 소유 옷과 이미지 metadata를 생성하는 application service로 처리한다.

```text
Controller -> Application Service -> Domain Service -> Repository / Provider
```

이미지 파일 bytes는 DB가 아니라 로컬 파일 시스템 또는 Docker Compose volume에 저장한다. DB에는 `clothing_items`의 nullable 이미지 메타데이터만 저장한다.

기본 옷 프리셋 이미지는 `src/main/resources/default-clothing-presets/`에 번들하고, 사용자에게 부여할 때마다 UUID 기반 파일명으로 storage volume에 복사한다. 여러 사용자가 같은 resource 파일명을 공유하지 않으므로 한 사용자의 이미지 삭제나 교체가 다른 사용자에게 영향을 주지 않는다.

## 권장 패키지 구조

```text
com.smartcloset
├── auth
├── common
├── security
├── user
├── location
├── weather
├── recommendation
└── clothing
    ├── domain
    ├── repository
    ├── application
    ├── presentation
    ├── dto
    └── infrastructure
        └── file
```

MVP5에서 `clothing.infrastructure.file`은 파일 시스템 저장, 파일명 생성, 파일 읽기/삭제 같은 persistence adapter 역할을 맡는다.

프론트엔드:

```text
frontend/src
├── api
├── components
├── features
│   ├── auth
│   ├── clothes
│   ├── location
│   ├── preferences
│   ├── recommendation
│   └── history
├── types
└── main.tsx
```

## 인증 경계

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

보호 API:

- 모든 `/api/**` endpoint 중 auth 2종을 제외한 endpoint
- MVP5 이미지 API `PUT/GET/DELETE /api/clothes/{clothingId}/image`

이미지 조회도 보호 API다. public static file serving으로 사용자 이미지를 노출하지 않는다.

## 기본 옷 프리셋 생성 흐름

```text
AuthService.signup/login
  -> DefaultClothingPresetSeeder.seedIfEmpty(user)
      -> ClothingItemRepository.countByUserId(user.id)
      -> classpath preset image read
      -> ClothingImageStorage.store(bytes, "jpg")
      -> ClothingItem.updateImageMetadata(...)
      -> ClothingItemRepository.flush()
```

- 회원가입은 사용자 저장 후 기본 위치와 빈 선호도를 유지한 채 기본 옷 5개를 생성한다.
- 로그인은 비밀번호 검증 후 현재 사용자 옷 row가 0개일 때만 같은 프리셋을 backfill한다.
- 옷 row가 하나라도 있으면 archived 상태와 관계없이 자동 프리셋을 추가하지 않는다.
- DB 반영 실패가 발생하면 이번 시도에서 storage에 복사한 프리셋 이미지 파일을 삭제해 orphan file을 줄인다.
- 프리셋 이미지 metadata도 일반 옷 이미지와 같은 보호 image URL로 노출한다.

## 이미지 업로드 흐름

```text
ClothingImageController
  -> ClothingImageService
      -> UserRepository
      -> ClothingItemRepository
      -> ClothingImageValidator
      -> ClothingImageStorage
      -> ClothingItem.updateImageMetadata(...)
```

1. Controller가 인증 principal에서 현재 사용자 id를 얻는다.
2. Service가 현재 사용자 존재 여부를 확인한다.
3. Service가 `clothingId`와 현재 사용자 id로 옷 소유권을 확인한다.
4. Validator가 파일 크기, 확장자, MIME type, signature를 검증한다.
5. Storage가 UUID 기반 파일명으로 새 파일을 저장한다.
6. Entity 이미지 메타데이터를 갱신한다.
7. 기존 이미지가 있으면 새 파일 저장과 메타데이터 갱신 성공 후 기존 파일을 삭제한다.
8. `ClothingResponse`를 반환한다.

실패 시 기존 이미지 상태를 유지하는 것이 원칙이다. 새 파일 저장 후 DB 갱신이 실패하면 새 파일을 삭제해 orphan file을 최소화한다.

## 이미지 조회 흐름

```text
GET /api/clothes/{clothingId}/image
  -> 인증 principal
  -> 소유권 확인
  -> 이미지 메타데이터 존재 확인
  -> storage read
  -> Content-Type + bytes 반환
```

실패 정책:

- 토큰 없음 또는 잘못된 토큰: `401`
- 다른 사용자 옷 또는 존재하지 않는 옷: `404 CLOTHING_NOT_FOUND`
- 내 옷이지만 이미지 없음: `404 CLOTHING_IMAGE_NOT_FOUND`
- 메타데이터는 있지만 파일이 없으면 `CLOTHING_IMAGE_NOT_FOUND`로 실패한다.

## 이미지 삭제 흐름

```text
DELETE /api/clothes/{clothingId}/image
  -> 인증 principal
  -> 소유권 확인
  -> 기존 이미지 파일 삭제 시도
  -> image_* metadata null 처리
  -> ClothingResponse 반환
```

삭제는 idempotent하다. 이미지가 이미 없어도 성공 응답을 반환한다.

## Storage 정책

기본 property:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: ${CLOTHING_IMAGE_MAX_SIZE_BYTES:5242880}
      max-request-size: ${CLOTHING_IMAGE_MAX_SIZE_BYTES:5242880}

smartcloset:
  clothing:
    image:
      storage-dir: ${CLOTHING_IMAGE_STORAGE_DIR:./uploads/clothing-images}
      max-size-bytes: ${CLOTHING_IMAGE_MAX_SIZE_BYTES:5242880}
```

Docker Compose에서는 app service가 `clothing-image-data` volume을 app container 내부 storage path인 `/data/smartcloset/clothing-images`에 mount한다.

저장 파일명:

```text
{uuid}.{extension}
```

원본 파일명은 저장 경로에 사용하지 않는다.

## DTO 구조

`ClothingResponse`와 `OutfitItemResponse`는 nullable image metadata를 포함한다.

```json
{
  "image": {
    "url": "/api/clothes/1/image",
    "contentType": "image/jpeg",
    "sizeBytes": 123456,
    "uploadedAt": "2026-05-25T10:00:00"
  }
}
```

이미지가 없으면 `image`는 `null`이다.

추천 결과는 `RecommendationResultItem -> ClothingItem` fetch 경로에서 image metadata를 함께 DTO로 매핑한다. 추천 점수 snapshot에는 이미지 관련 값을 저장하지 않는다.

## 트랜잭션 경계

- 옷 이미지 업로드/교체: write transaction + 파일 저장 보상 처리
- 옷 이미지 조회: readOnly transaction + 파일 read
- 옷 이미지 삭제: write transaction + 파일 삭제
- 옷 등록/수정/보관: 기존 transaction 유지
- 추천 생성/이력 조회: 기존 transaction 유지, DTO 매핑에 image metadata만 추가

파일 I/O와 DB transaction은 완전히 원자적일 수 없다. 구현은 실패 보상 처리와 테스트로 불일치 가능성을 줄인다.

## 프론트 이미지 구조

일반 `<img src="/api/clothes/1/image">`는 Authorization header를 붙일 수 없으므로 사용하지 않는다.

권장 흐름:

```text
image metadata url
  -> fetch(url, { headers: { Authorization: Bearer token } })
  -> Blob
  -> URL.createObjectURL(blob)
  -> img src={objectUrl}
  -> cleanup URL.revokeObjectURL(objectUrl)
```

컴포넌트는 이미지가 없거나 blob fetch에 실패하면 기존 category glyph/swatch/chip fallback을 표시한다.

## 추천 도메인 영향

이미지는 추천 도메인 입력이 아니다.

- `WeatherSuitabilityFilter`는 이미지 메타데이터를 사용하지 않는다.
- `OutfitCandidateGenerator`는 이미지 메타데이터를 사용하지 않는다.
- `RecommendationScorer`는 이미지 메타데이터를 사용하지 않는다.
- `RecommendationReasonGenerator`는 이미지 메타데이터를 사용하지 않는다.

## 금지 사항

- 이미지 API를 공개 API로 만들지 않는다. 이유: 사용자 소유 이미지가 인증 없이 노출될 수 있다.
- 기존 옷 등록/수정 JSON API를 multipart로 대체하지 않는다. 이유: 기존 프론트/테스트/API 계약을 불필요하게 흔든다.
- 원본 파일명을 저장 경로로 사용하지 않는다. 이유: path traversal과 파일명 충돌 위험이 있다.
- 이미지 업로드 여부를 추천 점수에 반영하지 않는다. 이유: MVP5는 시각적 식별 개선이 목표다.
- AI 자동 태깅을 추가하지 않는다. 이유: MVP5 범위를 넘고 추천 규칙 검증을 흐린다.
