# 단계 3: clothing-p0-api

범위: Must-have / P0

## 읽어야 할 파일
- `docs/API.md`
- `docs/ERD.md`
- `docs/ARCHITECTURE.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `phases/1-smartcloset-mvp/step2.md`

## 작업
P0 Clothing 등록과 목록 조회 API를 구현한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/clothing/presentation/**`
- `src/main/java/com/smartcloset/clothing/application/**`
- `src/main/java/com/smartcloset/clothing/dto/**`
- `src/main/java/com/smartcloset/clothing/repository/**`
- `src/test/java/com/smartcloset/clothing/**`

## 구현 메모
- `POST /api/clothes?userId={userId}`는 옷을 등록하고 `201 Created`를 반환한다.
- `GET /api/clothes?userId={userId}`는 `archived=false` 옷을 `id` 오름차순으로 반환한다.
- 등록 request field는 `name`, `category`, `color`, `material`, `minTemperature`, `maxTemperature`, `rainSuitable`이다.
- `archived`는 등록 시 서버에서 `false`로 설정한다.
- Validation은 `name` blank 불가/최대 50자, enum 검증, `minTemperature <= maxTemperature`, boolean 필수 기준을 따른다.
- Controller는 HTTP와 validation만 담당하고 비즈니스 규칙은 Service/Entity에 둔다.

## 검증 절차
```bash
./gradlew test
./gradlew build
```

## 인수 기준
- 옷 등록 API가 API 문서 응답 형태를 따른다.
- 옷 목록 조회가 `userId`와 `archived=false` 기준으로 동작한다.
- 다른 사용자 데이터가 섞이지 않는다.
- validation 실패는 공통 실패 응답으로 반환된다.

## 금지사항
- pagination을 추가하지 마라. 이유: P0 API는 pagination 없음으로 확정되어 있다.
- 추천 관련 로직을 Clothing API에 섞지 마라. 이유: 추천은 별도 도메인 경계다.
