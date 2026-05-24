# 추천 이력 Fetch 성능 개선 기록

## 문서 목적
이 문서는 추천 이력 조회와 추천 생성 중 최근 추천 조회에서 발생할 수 있던 Hibernate collection fetch pagination 위험을 정리한 성능 개선 기록이다.

이 문서는 ADR이 아니며 공개 API, DB schema, 추천 점수 규칙을 변경하지 않는다. 관련 구현은 PR `#66`에서 반영했다.

## 문제
Hibernate WARN `HHH90003004`는 collection fetch join과 pagination이 함께 사용될 때 발생할 수 있다.

문제의 핵심은 모든 fetch join이 아니다. 위험한 조합은 `OneToMany` 컬렉션 fetch join과 `Pageable` 또는 TopN 제한을 함께 사용하는 경우다.

예를 들어 추천 결과 20개를 최신순으로 가져오면서 각 추천 결과의 item 컬렉션까지 한 번에 join fetch하면, DB row는 추천 결과 1개가 아니라 추천 결과 item 수만큼 늘어난다. 이 상태에서 SQL `LIMIT 20`을 적용하면 추천 결과 20개가 아니라 join row 20개가 잘릴 수 있다.

Hibernate는 부모 entity 개수 제한이 깨지는 것을 피하기 위해 SQL limit을 직접 적용하지 않고, 더 많은 row를 메모리로 읽은 뒤 Java 메모리에서 제한을 적용할 수 있다. 추천 이력이 많은 사용자에게는 응답 지연, 메모리 사용 증가, WARN 로그 반복으로 이어질 수 있다.

## Before
기존 구조의 위험 요소는 다음과 같았다.

- `RecommendationResult`가 `items` 컬렉션 association을 가지고 있었다.
- 추천 이력 조회에서 result와 `items`, `items.clothingItem`을 함께 fetch했다.
- 추천 이력 API의 limit과 추천 생성 중 최근 추천 TopN 조회가 컬렉션 fetch와 결합될 수 있었다.
- DB가 부모 추천 결과 개수 기준으로 안전하게 pagination하기 어려운 형태였다.

이 문제는 작은 로컬 DB에서는 잘 드러나지 않지만, 사용자별 추천 이력과 추천 item이 많아질수록 비용이 커지는 구조적 성능 위험이다.

## After
개선 후 조회 전략은 2단계다.

1. 먼저 추천 결과 id만 최신순으로 선조회한다.
2. 선조회한 id 목록이 비어 있으면 즉시 빈 응답을 반환한다.
3. 추천 결과와 추천 결과 item을 각각 `IN (...)` 조회로 가져온다.
4. `IN (...)` 조회 결과의 DB 반환 순서는 신뢰하지 않는다.
5. 최종 응답과 scoring snapshot은 항상 선조회한 ordered id list 기준으로 Java에서 재조립한다.
6. item 응답은 DB 반환 순서가 아니라 `OutfitSlot` 기준으로 TOP, BOTTOM, OUTER에 매핑한다.

`RecommendationResultItem -> ClothingItem` 같은 `ManyToOne` 단건 association fetch join은 유지할 수 있다. 실제 item 조회는 item별 `clothingItem` N+1을 막기 위해 `RecommendationResultItem` 기준 `IN (...)` 조회에서 `clothingItem`을 join fetch한다.

즉 금지해야 할 것은 fetch join 전체가 아니라, 부모 결과를 pagination하면서 `OneToMany` 컬렉션을 함께 fetch하는 패턴이다.

## 최근 이력 병합 기준
추천 생성 중 scoring용 추천 이력은 두 목록을 별도로 선조회한다.

- 우선순위 1: 최근 7일 추천 결과 id
- 우선순위 2: 최근 5개 추천 결과 id

병합은 insertion order를 보존하는 `LinkedHashSet`을 사용한다. 최근 7일 id를 먼저 넣고, 최근 5개 id를 보강 목록으로 뒤에 붙인다. 중복 id는 처음 들어온 위치를 유지한다.

scorer에 전달되는 snapshot 순서는 DB `IN (...)` 조회 결과 순서가 아니라 병합된 ordered id list와 일치해야 한다.

## 회귀 방지 기준
추천 이력과 최근 추천 조회에서는 다음 기준을 지킨다.

- `OneToMany` 컬렉션 fetch join과 `Pageable` 또는 TopN 제한을 함께 쓰지 않는다.
- `RecommendationResult.items` 같은 JPA 컬렉션에 의존해 응답이나 scoring snapshot을 조립하지 않는다.
- 먼저 id를 정렬된 목록으로 조회하고, 해당 id 목록을 기준으로 Java에서 result와 item을 재조립한다.
- `findAllByIdIn` 또는 `IN (...)` 조회 결과 순서를 신뢰하지 않는다.
- item별 `clothingItem` N+1은 `ManyToOne` 단건 join fetch로 해결한다.
- history 관련 테스트에는 `hibernate.query.fail_on_pagination_over_collection_fetch=true`를 적용해 collection fetch pagination 회귀를 테스트 단계에서 차단한다.

## 확인한 테스트
PR `#66`에서 다음 검증을 통과했다.

- `./gradlew test --tests com.smartcloset.recommendation.RecommendationControllerTest`
- `./gradlew test --tests com.smartcloset.recommendation.domain.RecommendationScorerTest`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`

문서나 코드에서 메서드명을 언급할 때는 현재 코드에서 read-only로 확인한 확실한 이름만 사용한다. 이름이 불확실하거나 변경 가능성이 크면 `추천 이력 조회 repository 메서드`, `item IN 조회`처럼 역할 중심 표현을 사용한다.
