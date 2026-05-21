# PRD: SmartCloset 1.5차 MVP

## 한 줄 정의
SmartCloset 1.5차 MVP는 1차 규칙 기반 코디 추천 백엔드에 기상청 단기예보 조회서비스 JSON 연동을 추가해, 실제 날씨 기반으로 설명 가능한 옷 조합을 추천하는 Spring Boot 4.0.6 백엔드 서비스다.

## 배경
1차 MVP는 `StaticWeatherProvider`의 고정 테스트 날씨로 추천 도메인, API 계약, Docker Compose 공유 흐름을 검증했다.

1.5차 MVP는 추천 알고리즘 자체를 키우기보다 날씨 입력을 실제 API로 바꿔 제품 사용감을 높인다. 추천 생성 API, 사용자 모델, 옷 관리 API, 점수 체계는 1차 MVP 계약을 유지한다.

공식 날씨 API 기준은 공공데이터포털의 기상청_단기예보 조회서비스다.

- 데이터: 기상청_단기예보 조회서비스
- URL: https://www.data.go.kr/data/15084084/openapi.do
- 상세 기능: `getVilageFcst`
- 데이터 포맷: JSON
- 참고문서: `기상청41_단기예보 조회서비스_오픈API활용가이드_2510.zip`

## 해결하려는 문제
사용자는 날씨에 맞는 옷을 고르는 데 매일 반복적인 판단 비용을 쓴다. 1차 MVP는 이 판단을 규칙 기반으로 설명 가능하게 만들었지만, 고정 날씨만 사용해 실제 사용 환경과 차이가 있었다.

1.5차 MVP는 기상청 단기예보 JSON 응답을 내부 `WeatherCondition`으로 변환해 추천에 사용한다. 외부 API가 실패해도 fallback 날씨로 데모와 공유 흐름을 유지한다.

## 핵심 사용자 시나리오
1. 사용자는 seed user 또는 테스트용 `userId`로 서비스를 사용한다.
2. 사용자는 옷을 등록하고 목록을 조회한다.
3. 서비스는 `WeatherProvider`를 통해 현재 추천에 사용할 `WeatherCondition`을 조회한다.
4. 1.5차 기본 구현은 기상청 `getVilageFcst` JSON 응답을 사용한다.
5. 서비스키 미설정, 외부 API 실패, `NODATA`, 필수 category 누락 시 `StaticWeatherProvider` 값으로 fallback한다.
6. 서비스는 날씨상 입기 어려운 옷을 먼저 제외한다.
7. 남은 옷으로 TOP/BOTTOM 또는 TOP/BOTTOM/OUTER 조합을 만든다.
8. 각 조합은 날씨 적합도, 색상 조합, 최근 착용 이력, 최근 추천 이력, 다양성 보정으로 점수화된다.
9. 사용자는 생성된 추천 결과에서 날씨 snapshot, 총점, 세부 점수, 추천 이유를 확인한다.
10. 사용자가 추천 결과를 착용 완료 처리하면 이후 추천에 이력이 반영된다.
11. 공유 대상자는 Docker Compose 실행 후 Swagger 또는 Demo UI로 같은 흐름을 확인한다.

## 1.5차 MVP 우선순위

### P0: 실제 날씨 연동 문서/백엔드 기준
- 기상청 단기예보 조회서비스 `getVilageFcst` JSON 연동
- `KmaVilageForecastWeatherProvider`를 통한 `WeatherCondition` 생성
- `StaticWeatherProvider` fallback 유지
- 환경변수 기반 기본 위치 설정
- API key를 민감정보로 관리
- 추천 생성 API 계약 유지
- 추천 응답의 weather snapshot 유지
- Docker Compose에서 API key 없이도 fallback 데모 가능
- README, API, 데모, 공유 문서 동기화

### P1: 연동 신뢰도 강화
- KMA 응답 매핑 테스트
- 외부 API 실패/fallback 테스트
- base date/time 계산 테스트
- 서비스키 설정 시 Docker Compose 기반 수동 검증 시나리오

### P2: 1.5차 이후 후보
- 사용자별 위치 저장
- 위치 검색/선택 API
- 실시간 관측/초단기예보 혼합
- 날씨 source, nx, ny snapshot DB 저장
- 최근 성공 날씨 캐싱
- 체감온도, 습도, 일교차 기반 점수 고도화

## 1.5차 포함 범위
- Java 21 기반 Spring Boot 4.0.6 백엔드 유지
- 기존 REST API 계약 유지
- 기존 JPA/MySQL 저장 구조 유지
- 기존 규칙 기반 추천 점수 체계 유지
- 기상청 단기예보 JSON 호출 계약 문서화
- `WeatherProvider` 구현체 확장
- 외부 API 응답을 내부 `WeatherCondition`으로 매핑
- `StaticWeatherProvider` fallback
- 환경변수 기반 API key, base URL, nx, ny 설정
- Docker Compose 공유 방식 유지
- README와 문서 정합성 갱신

## 1.5차 제외 범위
- 로그인/회원가입
- 사용자별 위치 저장
- 위치 변경 API
- Weather source DB 저장
- 날씨 응답 캐싱용 Redis
- 최근 성공 날씨 DB 캐시
- AI/GPT 추천
- 옷 이미지 업로드
- 이미지 자동 분석/태깅
- 캘린더 연동
- 쇼핑몰 추천
- 관리자 기능
- 소셜/공유 기능
- 정식 프론트엔드 앱 구현
- React/Next/Vue 등 정식 프론트 기술 결정
- AWS 배포
- CD 자동화

## 공유 방식
1.5차 MVP 공유 방식은 Docker Compose로 유지한다.

서비스키가 없어도 앱은 실행되어야 한다. 이 경우 추천은 fallback 날씨로 생성된다. 실제 기상청 API 연동을 확인하려면 `.env`에 `KMA_SERVICE_KEY`를 설정한다.

필수 제공 파일은 다음과 같다.

- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `README.md`
- seed data

README에는 다음 정보를 포함한다.

- Docker Compose 실행 방법
- seed user 정보
- seed data 설명
- Swagger 접속 경로
- Demo UI 접속 경로
- 기상청 API key 설정 방법
- fallback 데모 시나리오
- 실제 API 연동 수동 확인 시나리오

## Weather 정책
추천 로직은 외부 API 응답 모델에 직접 의존하지 않는다. 모든 추천 도메인 로직은 내부 `WeatherCondition`만 사용한다.

`WeatherProvider` 인터페이스는 유지한다.

- 기본 1.5차 구현: `KmaVilageForecastWeatherProvider`
- fallback/test 구현: `StaticWeatherProvider`

`KmaVilageForecastWeatherProvider`는 기상청 단기예보 `getVilageFcst` JSON 응답에서 필요한 category를 읽어 내부 `WeatherCondition`을 생성한다.

`WEATHER_FALLBACK_ENABLED`의 기본값은 `true`다. `true`이면 서비스키 미설정, KMA 호출 실패, `resultCode != 00`, `NODATA`, 필수 category 누락, 파싱 실패 시 `StaticWeatherProvider` fallback 값을 사용한다. `false`이면 strict KMA mode로 동작하며 같은 상황에서 fallback하지 않고 `INTERNAL_SERVER_ERROR`로 실패한다. strict mode 실패는 추천 실패 코드 5종에 포함하지 않고 `RecommendationResult`를 저장하지 않는다.

`StaticWeatherProvider` fallback 값은 1차 MVP와 동일하게 유지한다.

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## KMA API 계약
1.5차에서 사용하는 외부 요청은 아래 하나로 제한한다.

```text
GET {KMA_BASE_URL}/getVilageFcst
```

기본 base URL:

```text
http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
```

요청 parameter:

| Parameter | Value |
| --- | --- |
| `serviceKey` | `KMA_SERVICE_KEY` 환경변수 |
| `pageNo` | `1` |
| `numOfRows` | `1000` |
| `dataType` | `JSON` |
| `base_date` | 계산된 발표일자 `yyyyMMdd` |
| `base_time` | 계산된 발표시각 |
| `nx` | `KMA_NX`, 기본 `60` |
| `ny` | `KMA_NY`, 기본 `127` |

기본 위치는 첨부 격자 위경도 XLSX의 서울특별시 대표 격자 기준이다.

| Location | nx | ny |
| --- | ---: | ---: |
| 서울특별시 | `60` | `127` |

단기예보 발표시각은 아래 값을 사용한다.

```text
0200, 0500, 0800, 1100, 1400, 1700, 2000, 2300
```

API 제공 시각은 각 발표시각 10분 이후로 본다. 현재 KST 기준으로 제공 가능한 최신 발표시각을 선택해 `base_date`, `base_time`으로 요청한다.

## WeatherCondition 매핑
KMA 응답은 `response.body.items.item[]`의 category별 `fcstValue`를 사용한다.

추천 기준 forecast target time은 현재 KST 이후 가장 가까운 예보시각으로 확정한다. KMA 응답의 `fcstDate`, `fcstTime`을 묶어 오름차순으로 정렬하고, 요청 시각 이후의 첫 forecast group을 선택한다. 선택된 forecast group의 category를 함께 사용해야 하며, `TMP`, `SKY`, `PTY`, `PCP`, `WSD` 중 하나라도 누락되면 다른 forecast group으로 이동하지 않고 fallback 또는 strict mode 실패로 처리한다.

| WeatherCondition | KMA category | Rule |
| --- | --- | --- |
| `temperature` | `TMP` | 정수 섭씨로 변환 |
| `weatherType` | `PTY`, `SKY` | `PTY` 우선, 없으면 `SKY` |
| `rainy` | `PTY`, `PCP` | `PTY != 0` 또는 강수량 존재 시 true |
| `windy` | `WSD` | `WSD >= 4.0`이면 true |

`weatherType` 세부 매핑:

| KMA value | WeatherType |
| --- | --- |
| `PTY=1`, `PTY=2`, `PTY=4` | `RAINY` |
| `PTY=3` | `SNOWY` |
| `PTY=0` + `SKY=1` | `SUNNY` |
| `PTY=0` + `SKY=3` 또는 `SKY=4` | `CLOUDY` |

`PCP` 값이 `-`, `null`, `0`, `강수없음`이면 강수 없음으로 본다.

`POP`, `REH`, `TMN`, `TMX`, `SNO`, `UUU`, `VVV`, `VEC` 등은 1.5차 추천 점수에 사용하지 않는다.

## 사용자 모델
1.5차에서도 회원가입과 로그인을 구현하지 않는다.

API는 테스트용 `userId`를 request parameter로 전달받는다. seed data에는 기본 seed user를 포함한다.

사용자별 위치 저장은 1.5차 범위가 아니다. 위치는 앱 전역 환경변수 `KMA_NX`, `KMA_NY`로 설정한다.

## 옷 관리 범위
API 기준으로는 1차 MVP에서 제공한 기능을 유지한다.

- 옷 등록
- 옷 목록 조회
- 옷 상세 조회
- 옷 수정
- 옷 보관 처리

옷 속성, enum, validation 규칙은 1차 MVP와 동일하다.

## 핵심 도메인
- `ClothingItem`: 사용자가 등록한 옷. 카테고리, 색상, 재질, 온도 적합 범위, 비 적합 여부, 보관 여부를 가진다.
- `WeatherCondition`: 추천 로직에서 사용하는 내부 날씨 상태.
- `WeatherProvider`: 현재 추천에 사용할 내부 `WeatherCondition`을 제공하는 인터페이스.
- `KmaVilageForecastWeatherProvider`: 기상청 단기예보 JSON 응답을 `WeatherCondition`으로 매핑하는 1.5차 기본 구현체.
- `StaticWeatherProvider`: 외부 API 실패 또는 테스트에서 사용하는 fallback 제공자.
- `OutfitCandidate`: TOP/BOTTOM 또는 TOP/BOTTOM/OUTER로 구성된 추천 후보 조합.
- `RecommendationScore`: 후보 조합의 총점과 세부 점수.
- `RecommendationReason`: 점수 근거를 사용자에게 설명하는 문장 목록.
- `RecommendationResult`: 저장된 추천 결과와 선택된 옷 조합, 점수, 이유, 착용 완료 여부.
- `WearHistory`: 사용자가 실제 착용 완료한 옷/코디 이력.
- `User`: 인증 없는 테스트용 사용자 식별자.

## 추천 점수 기준
추천 총점은 100점 기준으로 유지한다.

- 날씨 적합도: 35점
- 색상 조합: 25점
- 최근 착용 이력: 20점
- 최근 추천 이력: 10점
- 다양성 보정: 10점

KMA 연동은 날씨 입력 source를 바꾸는 작업이며, 점수 배점과 후보 선택 규칙을 변경하지 않는다.

## 추천 실패 케이스
추천 가능한 조합이 없으면 임의 추천을 만들지 않고 명시적 실패 코드를 반환한다.

- `NO_TOP_AVAILABLE`
- `NO_BOTTOM_AVAILABLE`
- `NO_WEATHER_SUITABLE_ITEM`
- `OUTER_REQUIRED_BUT_NOT_AVAILABLE`
- `INSUFFICIENT_CLOSET_ITEMS`

외부 기상청 API 실패는 추천 실패 코드로 노출하지 않는다. fallback이 활성화된 경우 fallback 날씨로 추천을 계속 생성한다.

## 주요 API
- `POST /api/clothes?userId={userId}`: 옷 등록
- `GET /api/clothes?userId={userId}`: 옷 목록 조회
- `GET /api/clothes/{clothingId}?userId={userId}`: 옷 상세 조회
- `PUT /api/clothes/{clothingId}?userId={userId}`: 옷 수정
- `PATCH /api/clothes/{clothingId}/archive?userId={userId}`: 옷 보관 처리
- `POST /api/recommendations?userId={userId}`: 추천 생성
- `PATCH /api/recommendations/{recommendationId}/worn?userId={userId}`: 추천 결과 착용 완료 처리

today 추천 GET 경로는 API 계약으로 사용하지 않는다.

## 완료 기준
- `WeatherProvider`가 KMA JSON 기반 날씨와 fallback 날씨를 모두 제공할 수 있다.
- 서비스키 미설정 상태에서도 Docker Compose 실행과 추천 생성이 성공한다.
- 서비스키 설정 시 `getVilageFcst` JSON 응답의 `TMP`, `SKY`, `PTY`, `PCP`, `WSD`가 내부 `WeatherCondition`에 반영된다.
- 현재 KST 이후 가장 가까운 `fcstDate`, `fcstTime` forecast group을 선택한다.
- `WEATHER_FALLBACK_ENABLED=false` strict KMA mode에서는 KMA 설정/호출/매핑 실패 시 추천 결과를 저장하지 않고 `INTERNAL_SERVER_ERROR`로 실패한다.
- 추천 생성 API 계약은 `POST /api/recommendations?userId={userId}`로 유지된다.
- 추천 결과에는 weather snapshot, outfit, score breakdown, reasons, worn, createdAt이 포함된다.
- 추천 도메인의 100점 배점과 실패 코드 5종이 변경되지 않는다.
- README만 보고 fallback 데모와 실제 API 연동 확인 방법을 이해할 수 있다.

## 테스트/검증 기준
- KMA JSON 정상 응답에서 `TMP`, `SKY`, `PTY`, `PCP`, `WSD` 매핑을 검증한다.
- 현재 KST 이후 가장 가까운 `fcstDate`, `fcstTime` forecast group 선택을 검증한다.
- 선택 forecast group의 필수 category 누락 시 다른 group으로 이동하지 않는지 검증한다.
- `WEATHER_FALLBACK_ENABLED=true`에서 `resultCode != 00`, `NODATA_ERROR`, 필수 category 누락, 서비스키 미설정 시 fallback을 검증한다.
- `WEATHER_FALLBACK_ENABLED=false`에서 같은 오류가 `INTERNAL_SERVER_ERROR`와 추천 결과 미저장으로 이어지는지 검증한다.
- base date/time 계산이 단기예보 발표시각과 API 제공 시각 기준을 따른다.
- 추천 스코어링, 날씨 필터링, 색상 점수, 최근 착용 패널티, 최근 추천 패널티, 다양성 보정 테스트는 계속 통과해야 한다.
- 추천 실패 코드 5종을 계속 테스트한다.
- Docker Compose는 서비스키 없이도 fallback으로 데모 가능해야 한다.
- 실제 서비스키가 있는 로컬 환경에서는 Swagger 또는 Demo UI에서 KMA 기반 추천 날씨가 응답에 반영되는지 수동 확인한다.

## 향후 MVP 후보
- 2차 MVP: 사용자 위치 저장, 위치 선택 API, 정식 프론트엔드 앱
- 3차 MVP: 옷 이미지 업로드, S3 연동, 이미지 기반 수동 태깅 보조
- 4차 MVP: 개인화 추천 고도화, 계절/선호도/스타일 태그, 추천 피드백 반영
- 5차 MVP: AI/GPT 설명 보조, 캘린더 연동, 쇼핑몰 추천, Redis 캐싱, 관리자 기능, AWS 배포, CD 자동화

## 결정된 사항
- Spring Boot 버전은 `4.0.6`으로 고정한다.
- 추천 생성 API는 `POST /api/recommendations?userId={userId}`를 유지한다.
- 1.5차 위치는 환경변수 `KMA_NX`, `KMA_NY`로만 관리한다.
- 기본 격자는 서울특별시 `nx=60`, `ny=127`이다.
- 외부 API 실패는 fallback으로 처리한다.
- KMA 연동 결정은 `docs/adr/006-kma-vilage-forecast-weather-provider.md`를 따른다.
