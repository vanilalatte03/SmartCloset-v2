# 단계 2: persistence-and-seed

범위: Must-have / P0

## 읽어야 할 파일
- `docs/ERD.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/DEMO_SCENARIO.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `phases/1-smartcloset-mvp/step1.md`

## 작업
ERD 기준 JPA Entity, enum, repository, auditing, seed user/data를 구현한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/common/domain/BaseTimeEntity.java`
- `src/main/java/com/smartcloset/user/**`
- `src/main/java/com/smartcloset/clothing/domain/**`
- `src/main/java/com/smartcloset/recommendation/domain/**`
- `src/main/java/com/smartcloset/recommendation/repository/**`
- `src/main/resources/application.yml`
- `src/main/resources/data.sql` 또는 seed initializer
- `src/test/java/com/smartcloset/**`

## 구현 메모
- 모든 JPA Entity는 `BaseTimeEntity`를 상속한다.
- enum은 `VARCHAR(30)`으로 저장한다.
- `recommendation_results.reasons_json`은 DB JSON 컬럼, Entity는 `String reasonsJson`으로 둔다.
- `WearHistory`는 `recommendation_result_id`를 참조하고 개별 clothing item id를 중복 저장하지 않는다.
- seed user는 `id=1`, `name=demo-user` 기준으로 준비한다.
- seed data는 `temperature=12`에서 TOP/BOTTOM/OUTER 추천이 가능하도록 구성한다.
- Entity 변경은 `updateDetails`, `archive`, `markWorn`처럼 의도가 드러나는 메서드로 제한한다.
- 이 단계는 persistence/entity/seed 준비에 한정하고 API controller, application use case, 추천 점수 계산 구현은 이후 step에서 다룬다.

## 검증 절차
```bash
./gradlew test
./gradlew build
```

## 인수 기준
- ERD의 필수 Entity와 enum이 구현되어 있다.
- JPA auditing이 동작한다.
- seed user와 seed clothes가 로딩된다.
- Entity에 `@Data` 또는 무분별한 setter가 없다.

## 금지사항
- 추천 점수 계산을 Repository에 넣지 마라. 이유: 추천 계산은 도메인/애플리케이션 계층 책임이다.
- 인증 사용자 모델을 만들지 마라. 이유: 1차 MVP는 seed/test user만 사용한다.
- P0 API controller를 구현하지 마라. 이유: API 계층은 Step 3과 Step 5에서 나누어 구현한다.
