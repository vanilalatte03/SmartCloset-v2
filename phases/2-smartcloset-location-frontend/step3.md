# 단계 3: frontend-scaffold-and-compose

범위: Must-have / 2차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/COMMANDS.md`
- `docs/SHARING_GUIDE.md`
- `docs/adr/007-mvp2-user-location-and-react-frontend.md`
- `docker-compose.yml`
- `.env.example`
- `.gitignore`

이전 단계에서 만들어진 백엔드 위치 API와 추천 API 계약을 확인한 뒤 작업하라.

## 작업
`frontend/` 아래 React+Vite+TypeScript SPA 기반을 만들고 Docker Compose `frontend` 서비스를 같은 step에서 추가한다. 이 단계는 앱 shell, TypeScript strict build, API client 골격, Compose 실행 구성을 만든다. 위치/옷장/추천의 실제 화면 흐름 완성은 다음 step에서 한다.

## 변경 예상 파일
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/tsconfig.json`
- `frontend/tsconfig.app.json`
- `frontend/tsconfig.node.json`
- `frontend/vite.config.ts`
- `frontend/index.html`
- `frontend/src/**`
- `docker-compose.yml`
- `.env.example`
- `.gitignore`
- `docs/COMMANDS.md`
- 필요 시 `README.md`, `docs/SHARING_GUIDE.md`

## 구현 메모
- React, Vite, TypeScript를 사용한다.
- TypeScript `strict`를 켠다.
- 권장 scripts:

```json
{
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "tsc -b && vite build",
    "preview": "vite preview --host 0.0.0.0"
  }
}
```

- 프론트 환경변수는 `VITE_API_BASE_URL`을 사용하며 로컬 기본값은 `http://localhost:8080`이다.
- Docker Compose `frontend` 서비스는 브라우저에서 접근 가능한 API base URL을 사용해야 한다.
- `frontend` 서비스와 `frontend/` 스캐폴드를 반드시 같은 변경에 포함한다.
- `docs/COMMANDS.md`는 `frontend/` 생성 후 `frontend-build`를 현재 필수 검증으로 올릴 수 있다.
- 이 단계의 앱 화면은 비어 있지 않은 shell과 API 연결 상태 표시 정도면 충분하다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
cd frontend && npm run build
docker compose config
```

## 인수 기준
- `frontend/`가 Vite React TypeScript 앱으로 생성된다.
- `npm run build`가 TypeScript type check와 Vite build를 함께 수행한다.
- `ErrorResponse.details` 타입은 `Array<{ field: string; message: string }>`이다.
- 컴포넌트에서 직접 `fetch`를 호출하지 않도록 API client 파일이 준비된다.
- Docker Compose에 `frontend` 서비스가 추가되고 `5173` 포트로 접근 가능하다.
- `docker compose config`가 통과해 Compose 서비스 구성이 유효하다.
- 기존 `mysql`, `app` Compose 실행 흐름이 깨지지 않는다.
- README 또는 공유 문서를 수정했다면 현재 실행 가능한 상태와 설명이 일치한다.

## 금지사항
- 프론트 스캐폴드만 만들고 Compose `frontend` 서비스를 빼지 마라. 이유: 문서와 공유 실행 경로가 다시 어긋난다.
- Compose `frontend` 서비스만 만들고 `frontend/`를 빼지 마라. 이유: 실행할 앱이 없어 공유 검증이 실패한다.
- 대형 상태 관리 라이브러리를 추가하지 마라. 이유: 2차는 React state와 작은 API client로 충분하다.
- 프론트에서 추천 점수 계산이나 KMA 매핑을 재구현하지 마라. 이유: 도메인 규칙은 백엔드 책임이다.
- today 추천 GET 경로를 프론트 API client에 추가하지 마라. 이유: 금지된 API 계약이다.
