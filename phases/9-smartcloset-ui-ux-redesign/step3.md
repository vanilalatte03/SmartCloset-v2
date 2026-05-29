# 단계 3: closet-list-form-images

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/design/mvp9/README.md`
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/App.css`

## 작업

- Closet 목록과 등록/수정 UX를 `smartcloset-closet-list-mockup.png`, `smartcloset-closet-add-mockup.png` 방향으로 리디자인한다.
- 옷 이미지는 목록 card와 form 주변 preview에서 우선 표시하고, 없으면 기존 fallback visual을 사용한다.
- 색상은 swatch, 소재와 style tag는 chip/toggle 기반 control로 표시한다.
- 옷 이미지 보호 API blob fetch와 object URL cleanup 흐름을 유지한다.
- 1440px 데스크톱과 390px 모바일에서 Closet 목록, empty/loading/error, 등록/수정 form의 겹침/잘림을 점검하고 수정한다.

## 인수 기준

```bash
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Closet 체크리스트를 확인한다:
   - 옷 이미지 보호 API blob fetch와 object URL cleanup이 유지되는가?
   - 옷 등록/수정 JSON API가 multipart로 대체되지 않았는가?
   - 색상, 소재, style tag 입력이 모바일에서 parent를 넘지 않는가?
   - Closet 목록과 form이 1440px 데스크톱과 390px 모바일에서 겹침/잘림 없이 보이는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "Closet 목록, 등록/수정 form, 이미지 표시 UX를 MVP9 디자인 기준으로 리디자인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 옷 등록/수정 JSON API를 multipart로 대체하지 마라. 이유: 이미지 업로드는 별도 보호 API 계약이다.
- 다중 이미지, 이미지 편집, AI 자동 태깅 UI를 추가하지 마라. 이유: MVP9 범위가 아니다.
- 옷 이미지 API를 public `<img src>` 직접 참조로 바꾸지 마라. 이유: 보호 이미지에는 Authorization header가 필요하다.
- Preferences, Location, History 화면을 함께 리디자인하지 마라. 이유: 해당 화면은 이후 step 범위다.
