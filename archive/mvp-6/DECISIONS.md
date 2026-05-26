# MVP 6 Decisions

MVP 6의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정

- MVP6는 추천 피드백/개인화 MVP로 정의했다.
- 자세한 내용은 ../../docs/adr/011-mvp6-feedback-personalization.md 를 따른다.
- 추천 생성은 계속 `POST /api/recommendations`를 사용했다.
- 추천 생성 request body는 선택이며 `situation`을 받을 수 있게 했다.
- body가 없거나 `situation`이 누락되면 `CASUAL`을 사용했다.
- 추천 상황 enum은 `WORK`, `CASUAL`, `WORKOUT`, `DATE`, `FORMAL`로 정했다.
- 추천 피드백은 추천 결과별 최신 상태 snapshot으로 저장했다.
- 별도 feedback event log table은 만들지 않았다.
- PUT feedback은 전체 교체로 정했다.
- 누락 필드는 `null`로 간주하고 양쪽 null은 clear로 처리했다.
- 옷별 styleTags는 `clothing_items.style_tags_json` JSON array string으로 저장했다.
- 총점 100점과 기존 score response field는 유지했다.
- `preferenceScore` 10점 내부만 색상, 소재, styleTags, 최근 피드백으로 확장했다.
- 최근 피드백 반영 window는 14일로 정했다.
- 이미지 metadata는 추천 점수와 추천 이유에 반영하지 않았다.

## MVP7로 넘긴 문제

- 대표 도시 중심 위치 catalog의 실사용 한계
- 추천에 사용된 위치와 weather source를 확인하기 어려운 문제
- KMA 사용 여부와 fallback 여부를 사용자에게 표시하는 문제
- 오전/오후/저녁 예보 기준 추천 선택
