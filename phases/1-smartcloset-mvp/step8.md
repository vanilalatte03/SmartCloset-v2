# 단계 8: demo-ui-p1

범위: Should-have / P1

## 읽어야 할 파일
- `docs/ARCHITECTURE.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `README.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `phases/1-smartcloset-mvp/step6.md`

## 작업
Spring Boot static resource 기반 최소 Demo UI를 추가해 P0 API 흐름을 브라우저에서 확인할 수 있게 한다.

## 변경 예상 파일
- `src/main/resources/static/demo/index.html`
- `src/main/resources/static/demo/app.js`
- `src/main/resources/static/demo/style.css`
- 필요 시 `README.md`, `docs/DEMO_SCENARIO.md`

## 구현 메모
- Demo UI는 제품용 프론트가 아니라 API 흐름 공유용 단일 페이지다.
- 기능은 옷 등록, 옷 목록 조회, 추천 생성, 착용 완료 처리로 제한한다.
- API는 기존 P0 endpoint만 사용한다.
- 기본 `userId`는 `1`로 두되, 필요하면 입력값으로 바꿀 수 있게 한다.
- React/Next/Vue 등 정식 프론트엔드 앱을 도입하지 않는다.
- UI 스타일 개선은 P2이므로 최소한의 사용성과 오류 표시만 구현한다.

## 검증 절차
```bash
./gradlew test
./gradlew build
docker compose up --build
```

수동 확인:
- `http://localhost:8080/demo/index.html` 접속
- 옷 목록 조회
- 옷 등록
- 추천 생성
- 착용 완료 처리

## 인수 기준
- Demo UI에서 P0 핵심 흐름을 수행할 수 있다.
- Demo UI는 Spring Boot static resource로 제공된다.
- Swagger 기반 P0 데모 흐름은 계속 동작한다.

## 금지사항
- 정식 프론트엔드 앱을 만들지 마라. 이유: React/Next/Vue 기술 결정은 후속 MVP 범위다.
- P0 API 계약을 Demo UI 편의에 맞춰 바꾸지 마라. 이유: API 문서가 계약의 기준이다.
