# Phase: SmartCloset 5차 Clothing Images MVP

## 목표

MVP4 인증 사용자 반응형 UX 위에 옷 이미지 업로드와 썸네일 표시를 추가한다. 사용자는 옷 1개당 이미지 1장을 업로드, 교체, 삭제할 수 있고, 옷 목록과 추천 결과와 추천 이력에서 썸네일을 확인할 수 있어야 한다.

## 작업 범위

- Must-have / MVP5 P0: 이미지 메타데이터 컬럼, 로컬 파일 저장, 파일 검증, 보호 이미지 API, 옷/추천 DTO image metadata, Closet 업로드/교체/삭제 UX, 추천 결과/이력 썸네일, Docker Compose volume 공유 검증
- Should-have / MVP5 P1: 업로드 실패 문구 polish, 이미지 fetch fallback polish, 모바일 썸네일 레이아웃 polish
- MVP5 제외: AI 자동 태깅, 다중 이미지, 이미지 편집/크롭/압축 파이프라인, S3/CDN, 이미지 기반 추천 점수, 이미지 moderation

## Steps

| Step | Name | Range |
| ---: | --- | --- |
| 0 | mvp5-scope-docs | Must-have / MVP5 P0 |
| 1 | image-storage-foundation | Must-have / MVP5 P0 |
| 2 | clothing-image-api | Must-have / MVP5 P0 |
| 3 | image-response-dtos | Must-have / MVP5 P0 |
| 4 | closet-image-ux | Must-have / MVP5 P0 |
| 5 | recommendation-thumbnail-ux | Must-have / MVP5 P0 |
| 6 | compose-docs-qa | Must-have / MVP5 P0 |

## 단계 진행 원칙

- Step 0은 문서 전환과 phase 정의만 다룬다.
- Step 1은 DB/entity/storage/validation foundation만 다룬다. Controller endpoint를 노출하지 않는다.
- Step 2는 이미지 업로드/조회/삭제 API, `ClothingImageResponse`, `ClothingResponse.image`, backend tests에 집중한다. 프론트 UI를 구현하지 않는다.
- Step 3은 추천 DTO의 nullable image metadata를 반영한다. 추천 점수 규칙은 바꾸지 않는다.
- Step 4는 Closet view 이미지 관리 UX만 다룬다. 추천 화면 썸네일은 Step 5에서 한다.
- Step 5는 Today 추천 결과와 History 추천 이력의 썸네일 표시만 다룬다.
- Step 6은 Docker Compose volume, `.env.example`, 문서 동기화, 최종 QA를 수행한다.

## 완료 기준

- 기존 JSON 옷 등록/수정 API가 유지된다.
- 이미지 API는 모두 보호 API다.
- 현재 사용자 소유 옷 이미지만 접근 가능하다.
- 5MB jpg/jpeg/png/webp 검증이 동작한다.
- 이미지 업로드, 교체, 삭제가 가능하다.
- 옷 목록과 추천 결과와 추천 이력에 썸네일이 표시된다.
- 이미지가 없는 옷도 fallback UI로 표시된다.
- app 재시작 후 Docker Compose volume 이미지가 유지된다.
- 이미지 존재 여부가 추천 점수와 추천 이유를 바꾸지 않는다.

## 검증 명령

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config
```

최종 step에서는 아래를 추가로 실행한다.

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 실행 예시

```bash
python3 scripts/execute.py 5-smartcloset-clothing-images --next-step-only
python3 scripts/execute.py 5-smartcloset-clothing-images
python3 scripts/autopilot.py 5-smartcloset-clothing-images --base main --max-review-fixes 2 --unsafe
```
