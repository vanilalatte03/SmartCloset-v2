# 단계 6: p0-sharing-verification

범위: Must-have / P0

## 읽어야 할 파일
- `README.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/API.md`
- `docs/COMMANDS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `phases/1-smartcloset-mvp/step5.md`

## 작업
P0 공유 기준을 충족하도록 Docker Compose 실행 환경, Swagger 확인 흐름, README 시나리오 검증을 완성한다.

## 변경 예상 파일
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `src/main/resources/application*.yml`
- seed data 관련 파일
- `README.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`

## 구현 메모
- Docker Compose로 Spring Boot 앱과 MySQL이 함께 실행되어야 한다.
- `.env.example`은 로컬 공유용 값만 포함하고 실제 비밀번호나 토큰을 넣지 않는다.
- Swagger UI는 `http://localhost:8080/swagger-ui/index.html`에서 접근 가능해야 한다.
- OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 접근 가능해야 한다.
- `userId=1` 기준 목록 조회, 옷 등록, 추천 생성, 착용 완료, 재추천 시나리오를 문서와 맞춘다.
- Health check는 구현 방식을 확정한 경우에만 README에 확정 경로로 반영한다.

## 검증 절차
```bash
./gradlew test
./gradlew build
docker compose up --build
curl -s http://localhost:8080/v3/api-docs
```

## 인수 기준
- Docker Compose로 앱과 MySQL이 실행된다.
- Swagger에서 P0 API를 호출할 수 있다.
- README와 DEMO_SCENARIO의 P0 흐름이 재현된다.
- 추천 결과에 top, bottom, outer, score, reasons가 포함된다.
- `docker compose down -v`로 DB 초기화가 가능하다.

## 금지사항
- AWS 배포를 추가하지 마라. 이유: 1차 MVP 공유 방식은 Docker Compose로 고정되어 있다.
- P1 Demo UI 구현을 이 단계에 섞지 마라. 이유: P0 공유 기준을 먼저 안정화해야 한다.
