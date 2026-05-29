# 단계 5: history

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/design/mvp9/README.md`
- `frontend/src/features/history/HistoryPanel.tsx`
- `frontend/src/App.css`

## 작업

- History 화면을 `smartcloset-history-mockup.png` 방향으로 리디자인한다.
- 추천 이력은 최신순 scan이 쉬운 list/card 구조로 정리하고, 착용/피드백 상태를 명확히 표시한다.
- 이력의 위치/날씨 source snapshot 표시를 유지한다.
- 옷 이미지는 이력 item에서 우선 표시하고, 없으면 기존 fallback visual을 사용한다.
- 1440px 데스크톱과 390px 모바일에서 History list, empty/loading/error, item detail의 겹침/잘림을 점검하고 수정한다.

## 인수 기준

```bash
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. History 체크리스트를 확인한다:
   - 추천 이력 API와 `limit` 계약을 변경하지 않았는가?
   - 이력의 위치/날씨 source snapshot 표시가 유지되는가?
   - 착용/피드백 상태가 기존 데이터 흐름과 맞게 표시되는가?
   - History 화면이 1440px 데스크톱과 390px 모바일에서 겹침/잘림 없이 보이는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "History 화면을 MVP9 디자인 기준으로 리디자인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 추천 이력 API나 추천 feedback DTO를 변경하지 마라. 이유: MVP9는 백엔드 계약을 변경하지 않는다.
- 위치/날씨 source snapshot을 숨기지 마라. 이유: MVP7 이후 이력 설명 계약이다.
- 이미지 metadata를 추천 점수나 추천 이유에 사용하지 마라. 이유: 이미지 존재 여부는 추천 품질 신호가 아니다.
- Account settings 또는 Preferences/Location 화면을 함께 리디자인하지 마라. 이유: 해당 화면은 별도 step 범위다.
