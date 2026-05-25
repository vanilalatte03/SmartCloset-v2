# 단계 1: image-storage-foundation

## 읽어야 할 파일

- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/ARCHITECTURE.md`
- `src/main/java/com/smartcloset/clothing/domain/ClothingItem.java`
- `src/main/java/com/smartcloset/clothing/repository/ClothingItemRepository.java`
- `src/main/resources/application.yml`
- `src/test/resources/application-test.yml`

## 작업

이미지 API endpoint를 노출하기 전에 저장 foundation을 만든다.

- `ClothingItem`에 nullable image metadata field와 mutation method를 추가한다.
- `application.yml`에 image storage dir와 max size property를 추가한다.
- `spring.servlet.multipart.max-file-size`와 `spring.servlet.multipart.max-request-size`를 `CLOTHING_IMAGE_MAX_SIZE_BYTES`보다 작지 않게 설정한다.
- 로컬 파일 저장 adapter를 `clothing.infrastructure.file` 아래에 추가한다.
- 파일명은 UUID 기반으로 생성한다.
- 원본 파일명은 저장 경로에 사용하지 않는다.
- 파일 validator는 5MB, jpg/jpeg/png/webp, MIME type, signature 검증을 담당한다.
- 테스트에서 임시 디렉터리를 사용해 파일 저장/삭제가 격리되게 한다.

## 인수 기준

```bash
./gradlew test --tests com.smartcloset.domain.EntityBehaviorTest
./gradlew test --tests com.smartcloset.clothing.*
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. `ClothingItem`의 기존 create/update/archive 동작이 유지되는지 확인한다.
3. 성공하면 phase index의 Step 1을 completed로 갱신한다.

## 금지사항

- Controller endpoint를 추가하지 마라. 이유: API 노출은 Step 2 범위다.
- 기존 옷 등록/수정 API를 multipart로 바꾸지 마라. 이유: JSON API 호환성을 유지해야 한다.
- 추천 도메인 클래스를 수정하지 마라. 이유: 이미지 metadata는 추천 계산 입력이 아니다.
