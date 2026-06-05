# MVP 9 Decisions

MVP 9의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정

- MVP9는 AWS 배포가 아니라 프론트 UI/UX 리디자인 MVP로 정의했다.
- 자세한 내용은 ../../docs/adr/014-mvp9-ui-ux-redesign.md 를 따른다.
- AWS 배포, S3, SES/SMTP, Secrets Manager, CD 자동화는 후속 MVP로 연기했다.
- MVP9는 백엔드 HTTP API, DTO, DB schema, 추천 점수/필터/tie-break를 변경하지 않았다.
- 데스크톱 primary navigation은 상단 탭으로 정했다.
- 모바일 primary navigation은 하단 탭으로 정했다.
- primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록`으로 고정했다.
- `계정 설정`은 primary nav가 아니라 우측 상단 profile pill/menu에서 진입하게 했다.
- 프론트 UX 기준은 `docs/FRONTEND.md`와 구현된 React 화면에 둔다.
- 옷장 보관함 복원은 ADR-015에 따라 기존 `archived` 컬럼을 재사용하고 DB schema와 추천 규칙을 변경하지 않았다.

## MVP10으로 넘긴 문제

- 사진 업로드 후 AI가 옷 등록 후보값을 제안하는 흐름
- 사용자가 confidence 낮은 후보를 확인/수정한 뒤 저장하는 UX
- Spring AI와 OpenAI 모델 호출을 비용 제한과 비활성 기본값으로 감싼 provider boundary
- 추천 생성은 계속 규칙 기반으로 유지하고 AI를 추천 점수/이유에 사용하지 않는 경계
