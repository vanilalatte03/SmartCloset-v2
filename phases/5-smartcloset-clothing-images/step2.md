# 단계 2: clothing-image-api

## 읽어야 할 파일

- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `src/main/java/com/smartcloset/clothing/application/ClothingService.java`
- `src/main/java/com/smartcloset/clothing/presentation/ClothingController.java`
- `src/main/java/com/smartcloset/common/exception/ErrorCode.java`
- `src/test/java/com/smartcloset/clothing/ClothingControllerTest.java`
- `src/test/java/com/smartcloset/security/SecurityBoundaryRegressionTest.java`

## 작업

옷 이미지 보호 API를 구현한다.

- `PUT /api/clothes/{clothingId}/image`
- `GET /api/clothes/{clothingId}/image`
- `DELETE /api/clothes/{clothingId}/image`
- `ClothingImageResponse`
- `ClothingResponse.image`

규칙:

- 모든 endpoint는 Bearer token이 필요하다.
- 현재 인증 사용자 소유 옷만 접근 가능하다.
- 다른 사용자 옷 또는 없는 옷은 `CLOTHING_NOT_FOUND`로 실패한다.
- 내 옷이지만 이미지가 없으면 조회에서 `CLOTHING_IMAGE_NOT_FOUND`로 실패한다.
- 삭제는 idempotent하다.
- 업로드 검증 실패는 `INVALID_REQUEST`와 details로 실패한다.
- 파일 크기 초과와 Spring multipart size 초과도 `400 INVALID_REQUEST`와 `details` 배열로 실패한다.

## 인수 기준

```bash
./gradlew test --tests com.smartcloset.clothing.ClothingControllerTest
./gradlew test --tests com.smartcloset.security.SecurityBoundaryRegressionTest
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 보호 API 인증 경계, 사용자 격리, `ClothingResponse.image` 테스트가 포함되어 있는지 확인한다.
3. 성공하면 phase index의 Step 2를 completed로 갱신한다.

## 금지사항

- 이미지 조회를 public static URL로 열지 마라. 이유: 인증 사용자 이미지가 노출될 수 있다.
- 삭제 실패를 이유로 이미 없는 이미지를 에러 처리하지 마라. 이유: 삭제 API는 idempotent해야 한다.
- AI 태깅을 추가하지 마라. 이유: MVP5 범위 밖이다.
