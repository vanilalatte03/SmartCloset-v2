# MVP4를 반응형 실사용 UX로 정의

## 상태
승인됨

## 맥락
MVP-3에서는 인증 사용자 기준 baseline을 완료했다. 사용자는 회원가입과 로그인을 할 수 있고, 자신의 옷장, 위치, 선호도를 관리하며, 추천을 생성하고, 이력을 조회하고, 추천을 착용 완료로 표시할 수 있다.

남은 제품 리스크는 추천 점수 계산의 깊이가 아니다. 핵심 문제는 실제 사용자가 가치를 얻기 전에 내부 enum 값, 백엔드 실패 코드, 분리된 패널, 점수 중심 추천 결과를 이해해야 한다는 점이다.

따라서 MVP4의 제품 목표는 다음과 같다. 회원가입 또는 로그인 후 사용자가 2분 안에 첫 옷차림 추천을 성공해야 한다.

## 결정
MVP4는 반응형 웹 UX MVP다.

- 프론트엔드 플랫폼은 React + Vite + TypeScript를 유지한다.
- MVP4에서는 네이티브 모바일 앱을 만들지 않는다.
- PWA 설치, 푸시 알림, 앱스토어 출시 작업을 요구하지 않는다.
- 로그인 후 앱을 Today, Closet, Preferences, Location, History의 다섯 가지 제품 화면 중심으로 재구성한다.
- 데스크톱에서는 사이드바 내비게이션을, 모바일에서는 하단 탭 내비게이션을 사용한다.
- 로그인 후 기본 화면은 Today로 둔다.
- 기존 API 데이터를 사용해 첫 추천 준비 체크리스트를 추가한다.
- Today가 추천 생성 전에 사용자의 저장 위치 기준 날씨를 보여줄 수 있도록 보호 API `GET /api/weather/current`를 하나 추가한다.
- API enum 값은 한국어 UI 라벨, 색상 swatch, 소재 chip으로 표시한다.
- 기존 옷 API를 사용해 생성, 수정, archive 처리가 가능한 실용적인 옷장 관리 UX를 추가한다.
- UI에서는 원시 추천 비즈니스 실패 코드를 한국어 안내와 직접 행동 유도 버튼으로 대체한다.
- 추천 점수 세부 내역보다 추천 이유를 먼저 보여준다.

MVP4는 공개 API, DB schema, 추천 점수 계산, 새로운 weather provider를 추가하지 않는다. 유일한 백엔드 API 추가는 보호 API인 현재 날씨 요약 엔드포인트다.

P0 release cut은 Step 7에서 Docker Compose 공유와 첫 추천 성공 흐름을 기준으로 마감한다. Step 8-13은 선호도, 위치, 이력 화면 polish와 Today/Closet/Preferences/Location/History의 시각 우선순위 보강 tail이며 P0 공유 성공 여부를 막는 blocker가 아니다.

## 결과
- 프론트엔드 구현은 백엔드 schema migration 없이 진행할 수 있다.
- 기존 API 문서는 계속 유효하지만, 프론트엔드 문서는 MVP4가 API 데이터를 사용자에게 보이는 UI로 매핑하는 방식을 정의해야 한다.
- `GET /api/weather/current`는 기존 KMA/fallback `WeatherProvider` 경로를 재사용하며 추천 결과나 추천 이력을 생성하면 안 된다.
- 추천 실패 코드는 안정적인 API 계약으로 유지하면서 UI는 사용자 친화적으로 만든다.
- 외부 디자인 초안의 visual reference는 참고 자료일 뿐이다. 프로젝트 단일 기준은 PRD, ADR, 프론트엔드 문서다.
- MVP5에서는 이미지 업로드, PWA, 네이티브 앱, AI/GPT 추천, 외부 location provider를 다시 검토할 수 있다.

## 범위 제외
- 이미지 업로드
- 소셜 로그인
- 비밀번호 재설정
- 이메일 인증
- refresh token
- 외부 주소/지도 API
- 브라우저 위치 정보
- AI/GPT 추천
- styleTags 점수화 또는 추천 이유 반영
- 선호도 정규화 테이블
- Redis
- AWS 배포와 CD 자동화
