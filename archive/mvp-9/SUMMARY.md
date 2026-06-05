# MVP 9 Summary

## 구현된 기능

- 데스크톱 상단 탭과 모바일 하단 탭 기반 app shell navigation
- `추천`, `옷장`, `내 취향`, `위치`, `기록` 5개 primary navigation 고정
- profile pill/menu 기반 계정 설정 진입
- Auth 화면 visual/form layout 리디자인
- 추천 dashboard와 추천 결과 표시 개선
- 옷장 목록, 등록/수정 form, 이미지 표시 UX 개선
- 보관함 조회와 다시 꺼내기 UX
- Preferences swatch/chip/toggle 입력 UX
- Location 동네 검색과 현재 위치 후보 찾기 UX 정리
- History timeline/list와 추천 이력 표시 개선
- 계정 설정 화면과 계정 삭제 팝업 UX 개선
- 데스크톱 1440px, 모바일 390px 수동 QA 기록

## 유지된 기능

- Spring Boot 4.0.6, Java 21, MySQL
- Spring Security + JWT Bearer access token
- DB-backed refresh session과 HttpOnly refresh cookie
- 이메일 인증, 비밀번호 재설정, Google OAuth, 계정 삭제
- 인증 사용자 기준 옷장/위치/선호도/추천 이력/착용 이력 격리
- KMA `getVilageFcst` JSON weather provider와 fallback weather
- 규칙 기반 추천 점수 100점 체계
- MVP5 이미지 업로드/교체/조회/삭제와 보호 이미지 blob fetch
- MVP6 styleTags, 추천 피드백, 최근 피드백 기반 `preferenceScore`
- MVP7 위치/날씨 source snapshot과 `forecastPeriod`
- Docker Compose 공유 흐름

## 제외된 기능

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
- 다중 이미지 업로드
- 이미지 편집/cropping/resizing pipeline

## 데모 시나리오 요약

- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- 회원가입, 이메일 인증, 로그인, refresh cookie 세션 복구를 확인한다.
- 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면을 desktop/mobile에서 확인한다.
- 옷 등록/수정, 이미지 업로드, 보관/복원, 추천 생성, 착용/피드백 저장을 확인한다.
- 계정 삭제 후 사용자 데이터와 이미지 파일이 삭제되는지 확인한다.
