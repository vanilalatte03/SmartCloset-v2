# Frontend: SmartCloset MVP5 Clothing Images

## 목표

MVP5 프론트엔드는 MVP4 반응형 웹앱 위에 옷 이미지 업로드, 교체, 삭제, 썸네일 표시를 추가한다.

사용자는 Closet view에서 옷별 이미지를 관리하고, Today 추천 결과와 History 추천 이력에서 실제 옷 썸네일을 확인할 수 있어야 한다.

## 기술 기준

- React
- Vite
- TypeScript strict
- 기존 CSS 체계 우선
- 큰 상태 관리 라이브러리 추가 금지
- API 요청/응답 DTO는 `src/types/api.ts`에 명시
- API 함수는 `src/api/smartClosetApi.ts`에서 정의
- access token 저장 위치는 `sessionStorage`

## 인증 상태 기준

이미지 API도 보호 API다. 모든 이미지 업로드, 삭제, blob 조회 요청에는 `Authorization: Bearer {accessToken}` header가 필요하다.

`401`은 기존 보호 API와 동일하게 인증 만료로 처리한다.

## API Client 기준

기존 JSON API helper는 유지한다.

MVP5에서 추가할 함수:

- `uploadClothingImage(accessToken, clothingId, file)`
- `deleteClothingImage(accessToken, clothingId)`
- `getClothingImageBlob(accessToken, imageUrl)`

multipart upload:

- `FormData`를 사용한다.
- part name은 `image`다.
- 브라우저가 boundary를 설정하도록 `Content-Type` header를 직접 지정하지 않는다.

blob fetch:

- `image.url`은 `/api/clothes/{id}/image` 형태다.
- fetch 시 API base URL과 합친다.
- Authorization header를 붙인다.
- 성공하면 `Blob`을 `URL.createObjectURL`로 변환한다.
- 컴포넌트 unmount 또는 이미지 변경 시 `URL.revokeObjectURL`을 호출한다.

## 타입 기준

```ts
export type ClothingImageResponse = {
  url: string;
  contentType: 'image/jpeg' | 'image/png' | 'image/webp';
  sizeBytes: number;
  uploadedAt: string;
};

export type ClothingResponse = ClothingRequest & {
  id: number;
  archived: boolean;
  image: ClothingImageResponse | null;
  createdAt: string;
  updatedAt: string;
};

export type OutfitItemResponse = {
  id: number;
  name: string;
  category: ClothingCategory;
  color: ClothingColor;
  material: ClothingMaterial;
  image: ClothingImageResponse | null;
};
```

## Closet View

MVP5 필수 구성:

- 옷 카드 썸네일 영역
- 이미지 없음 fallback visual
- 옷 등록 후 이미지 추가 흐름
- 옷 수정 중 이미지 교체
- 옷 수정 중 이미지 삭제
- 업로드 진행/성공/실패 상태
- 파일 검증 실패 메시지 표시

옷 카드:

- 이미지가 있으면 카드 상단 또는 좌측에 썸네일을 표시한다.
- 이미지가 없으면 기존 category glyph와 색상 swatch를 유지한다.
- name, category label, color swatch, material chip은 계속 표시한다.
- 수정/보관 액션은 모바일에서도 hover 없이 접근 가능해야 한다.

등록 흐름:

- 기본 옷 정보는 기존 JSON `POST /api/clothes`로 먼저 저장한다.
- 사용자가 파일을 선택한 경우 생성된 clothing id로 `PUT /api/clothes/{id}/image`를 이어서 호출한다.
- 이미지 업로드가 실패해도 옷 생성 자체를 되돌리지 않는다. 화면에는 "옷은 등록됐지만 이미지 저장에 실패했습니다."처럼 분리해서 안내한다.

수정 흐름:

- 기본 옷 정보 수정은 기존 JSON `PUT /api/clothes/{id}`를 사용한다.
- 이미지 교체는 별도 `PUT /api/clothes/{id}/image`를 사용한다.
- 이미지 삭제는 `DELETE /api/clothes/{id}/image`를 사용한다.
- 수정 취소 시 선택한 로컬 파일 미리보기와 임시 object URL을 정리한다.

파일 입력 UX:

- 허용 파일 설명: `jpg, png, webp / 최대 5MB`
- 파일 선택 후 로컬 preview를 보여준다.
- 프론트에서도 5MB와 MIME type을 사전 검사하되, 최종 검증은 서버 응답을 신뢰한다.

## Today Recommendation View

추천 결과 outfit slot 카드에 썸네일을 표시한다.

우선순위:

1. 이미지가 있으면 썸네일
2. 이미지가 없으면 category glyph
3. color swatch와 material chip은 항상 보조 정보로 표시

이미지 fetch 실패:

- 추천 결과 전체를 실패 처리하지 않는다.
- 해당 item만 fallback visual을 표시한다.
- 인증 만료로 판정되는 경우 기존 auth expired 흐름으로 연결한다.

## History View

추천 이력의 outfit summary에 썸네일을 표시한다.

주의:

- 추천 이력은 현재 `ClothingItem` 참조를 통해 최신 이미지 상태를 보여준다.
- 과거 추천 당시 이미지 snapshot을 별도로 저장하지 않는다.
- 이미지가 삭제된 옷은 fallback visual로 보인다.

## 공통 Thumbnail Component

권장 공통 컴포넌트:

```tsx
type ClothingThumbnailProps = {
  accessToken: string;
  image: ClothingImageResponse | null;
  fallbackLabel: string;
  className?: string;
};
```

역할:

- image가 없으면 fallback을 표시한다.
- image가 있으면 authenticated blob fetch를 수행한다.
- loading 상태는 레이아웃 크기를 바꾸지 않는다.
- 실패하면 fallback을 표시한다.
- object URL cleanup을 책임진다.

## 반응형 기준

- 모바일 375px에서 썸네일, 옷 이름, 수정/삭제/보관 버튼이 겹치지 않아야 한다.
- 썸네일 영역은 고정 aspect-ratio를 사용한다.
- 카드 hover에 의존하지 않는다.
- 이미지가 늦게 로드되어도 카드 높이가 크게 튀지 않도록 placeholder 영역을 유지한다.

## 접근성 기준

- 업로드 input은 label과 연결한다.
- 썸네일 `alt`는 옷 이름 기반으로 제공한다.
- 이미지 삭제 버튼은 파일 선택 input과 구분되는 명확한 텍스트를 가진다.
- 업로드 실패와 성공 문구는 status 영역으로 표시한다.

## 제외 범위

- 이미지 크롭 UI
- 이미지 편집 UI
- 다중 이미지 carousel
- drag and drop 전용 UX
- 카메라 직접 촬영
- EXIF 표시
- AI 자동 태깅
- 이미지 기반 추천 이유
- S3/CDN 직접 업로드
