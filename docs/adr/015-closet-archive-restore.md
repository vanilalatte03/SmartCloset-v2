# 옷장 보관함 복원 API와 UX 추가

## 상태

승인됨

## 맥락

기존 옷장 UX의 `보관` 동작은 삭제처럼 보일 수 있지만, 실제 도메인 모델에는 `clothing_items.archived` 플래그가 이미 존재한다. 사용자는 보관한 옷을 확인하고 다시 추천 후보로 꺼낼 수 있어야 한다.

MVP9는 프론트 UI/UX 리디자인 MVP였기 때문에 백엔드 API 변경을 원칙적으로 제외했지만, 보관함을 완성하려면 보관한 옷 목록 조회와 보관 해제 동작이 현재 사용자 소유 경계 안에서 필요하다.

## 결정

기존 `archived` 컬럼을 재사용하고 DB schema는 변경하지 않는다.

- `GET /api/clothes`는 보관하지 않은 현재 사용자 옷만 반환한다.
- `GET /api/clothes/archived`는 보관한 현재 사용자 옷만 반환한다.
- `PATCH /api/clothes/{clothingId}/archive`는 기존처럼 옷을 보관 처리한다.
- `PATCH /api/clothes/{clothingId}/unarchive`는 보관한 옷을 다시 활성 상태로 돌린다.
- archive와 unarchive는 멱등으로 처리한다.
- 다른 사용자 옷 또는 존재하지 않는 옷은 기존처럼 `CLOTHING_NOT_FOUND`로 실패한다.
- 현재 사용자 전용 response DTO에는 `userId`를 노출하지 않는다.

프론트 옷장 화면에는 `보관함` view를 추가하고, 보관한 옷은 `다시 꺼내기`로 활성 목록과 추천 후보에 복귀시킨다.

## 결과

- 사용자는 보관이 삭제가 아니라 숨김 상태임을 확인할 수 있다.
- 보관한 옷을 별도 목록에서 다시 꺼낼 수 있다.
- 기존 추천 후보 필터링은 보관하지 않은 옷 기준을 유지한다.
- DB schema, 추천 점수, 후보 tie-break, 이미지 저장 방식은 변경하지 않는다.

## 범위 제외

- DB schema 변경
- soft delete/account restore 정책 변경
- 추천 점수/필터/tie-break 변경
- 옷 이미지 저장소 변경
- AI 자동 태깅
