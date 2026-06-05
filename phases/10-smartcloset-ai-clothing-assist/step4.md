# 단계 4: closet-form-ai-assist

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/adr/016-mvp10-ai-clothing-registration-assist.md`
- `frontend/src/types/api.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/App.css`

## 작업

- `ClothingAnalysisResponse` 타입과 `analyzeClothingImage(accessToken, file)` API 함수를 추가한다.
- 옷 등록/수정 form에서 이미지 선택 후 preview와 `AI 후보 체크` 버튼을 제공한다.
- 분석 기능이 비활성/실패 상태면 기존 수동 입력 form은 계속 사용할 수 있어야 한다.
- 분석 성공 시 후보값을 form에 채운다.
- confidence가 `lowConfidenceThreshold`보다 낮은 필드는 흐리게 표시하고 `확인 필요` 상태를 표시한다.
- 사용자가 해당 필드를 수정하거나 확인하면 normal 상태로 전환한다.
- 확인 필요 필드가 남은 상태에서 저장을 누르면 한 번 더 확인하는 modal 또는 bottom sheet를 띄운다.
- 저장은 기존 순서를 유지한다:
  - `POST /api/clothes` 또는 `PUT /api/clothes/{clothingId}` JSON 저장
  - 선택 이미지가 있으면 기존 `PUT /api/clothes/{clothingId}/image` 업로드
- 같은 파일은 프론트 파일 fingerprint 기준으로 마지막 분석 결과를 재사용할 수 있다.

## 인수 기준

```bash
git diff --check
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Frontend 체크리스트를 확인한다:
   - 분석 API 호출은 사용자가 버튼을 눌렀을 때만 일어나는가?
   - low-confidence 필드는 흐림/확인 필요 상태로 보이는가?
   - 분석 실패 후에도 수동 저장 flow가 가능하며 입력값이 불필요하게 사라지지 않는가?
   - 기존 이미지 업로드 실패와 옷 정보 저장 실패 안내가 분리되는가?
   - 1440px 데스크톱과 390px 모바일에서 form, preview, 확인 필요 chip이 겹치지 않는가?
3. 결과에 따라 `phases/10-smartcloset-ai-clothing-assist/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- 이미지 선택만으로 자동 분석하지 마라. 이유: 비용은 사용자 수동 호출로 제한한다.
- 분석 결과를 바로 저장하지 마라. 이유: 사용자가 확인/수정한 값만 저장한다.
- 큰 state-management library를 추가하지 마라. 이유: 현재 앱은 React state와 작은 hook으로 충분하다.
- 추천 화면이나 추천 API를 AI 분석 결과에 맞춰 바꾸지 마라. 이유: MVP10 AI는 옷 등록 form 전용이다.
