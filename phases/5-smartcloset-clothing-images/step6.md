# 단계 6: compose-docs-qa

## 읽어야 할 파일

- `.agents/skills/smartcloset-backend/SKILL.md`
- `README.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `.env.example`
- `docker-compose.yml`
- `Dockerfile`
- `frontend/package.json`

## 작업

MVP5 최종 공유 기준을 맞추고 검증한다.

- Docker Compose app service에 이미지 storage volume을 연결한다.
- `.env.example`에 이미지 storage env를 추가한다.
- `.gitignore`에 로컬 uploads directory가 커밋되지 않게 한다.
- README, DEMO_SCENARIO, SHARING_GUIDE, COMMANDS가 실제 구현과 일치하는지 동기화한다.
- Docker Compose smoke와 브라우저 수동 QA를 수행한다.
- QA 결과가 필요하면 `docs/qa/` 아래에 MVP5 확인 기록을 남긴다.

## 인수 기준

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 회원가입, 옷 등록, 이미지 업로드, 교체, 삭제, 추천 생성, 추천 이력 썸네일을 수동 확인한다.
3. app 재시작 후 이미지가 유지되는지 확인한다.
4. 성공하면 phase index의 Step 6과 phases index를 completed로 갱신한다.

## 금지사항

- 실제 API key, JWT secret, token을 문서나 `.env.example`에 넣지 마라. 이유: 민감정보 커밋 금지.
- AWS/S3/CDN 배포를 추가하지 마라. 이유: MVP5 공유 방식은 Docker Compose 기준이다.
- 수동 QA 없이 최종 공유 완료로 표시하지 마라. 이유: 이미지 업로드는 브라우저 동작 확인이 필요하다.
