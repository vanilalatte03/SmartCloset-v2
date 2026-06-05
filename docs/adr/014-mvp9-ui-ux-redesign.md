# MVP9를 프론트 UI/UX 리디자인 MVP로 정의

## 상태

승인됨

## 맥락

MVP8에서는 refresh session, 이메일 인증, 비밀번호 재설정, Google login, 계정 삭제까지 계정 안정성 기능을 완료했다. 원래 다음 단계는 AWS 배포였지만, 현재 서비스는 기능은 갖췄어도 사용자가 처음 접했을 때 완성된 제품처럼 느끼기에는 화면 밀도, 시각 위계, 모바일 완성도가 부족하다.

MVP9 기획과 구현 단계에서는 Auth, 추천, 옷장, 취향, 위치, 기록, 계정 설정 화면의 데스크톱/모바일 리디자인 시안을 참고했다. MVP9 반영 완료 후 별도 시안 asset은 저장소에서 제거하고, 현재 프론트 UX 기준은 `docs/FRONTEND.md`와 구현된 React 화면에 둔다.

## 결정

MVP9는 AWS 배포가 아니라 프론트 UI/UX 리디자인 MVP다.

- AWS 배포, S3, SES/SMTP, Secrets Manager, CD 자동화는 후속 MVP로 연기한다.
- MVP9는 백엔드 HTTP API, DB schema, 추천 점수/필터/tie-break를 변경하지 않는다.
- MVP8 계정 안정성 기능은 현재 baseline으로 유지한다.
- 프론트 UX 기준은 `docs/FRONTEND.md`와 구현된 React 화면에 둔다.
- 데스크톱 primary navigation은 상단 탭으로 둔다.
- 모바일 primary navigation은 하단 탭으로 둔다.
- primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록`으로 고정한다.
- `계정 설정`은 주 navigation tab이 아니라 우측 상단 profile pill/menu에서 진입한다.
- 추천 화면은 날씨, 상황, 예보 시간대, 옷장 준비 상태, 최근 이력을 한 화면에서 스캔할 수 있게 한다.
- 옷장과 추천 결과는 실제 옷 이미지 중심으로 읽히게 한다.
- 입력 UI는 swatch, chip, segmented control, toggle, slider/input 같은 익숙한 control을 우선한다.
- 카드 radius는 8px 이하로 유지하고, 카드 안에 카드가 중첩되는 느낌을 피한다.
- 모바일 390px 기준으로 텍스트와 CTA가 겹치거나 잘리지 않아야 한다.

## 결과

- 사용자는 로그인 후 더 완성도 높은 제품 화면을 경험한다.
- 구현자는 API/DB/추천 규칙 변경 없이 프론트 화면 구조와 스타일에 집중할 수 있다.
- AWS 배포는 화면 완성도 개선 이후 더 안정적인 제품 상태에서 다룬다.

## 범위 제외

- AWS 배포 구현
- S3 storage 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- CD 자동화
- Redis
- 백엔드 API/DTO 변경
- DB schema 변경
- 추천 점수/필터/tie-break 변경
- AI/GPT 추천
- AI 자동 태깅
- native mobile app 또는 PWA 배포
