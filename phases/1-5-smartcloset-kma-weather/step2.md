# 단계 2: kma-http-client

범위: Must-have / 1.5 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/COMMANDS.md`
- `docs/adr/006-kma-vilage-forecast-weather-provider.md`
- `phases/1-5-smartcloset-kma-weather/step0.md`
- `phases/1-5-smartcloset-kma-weather/step1.md`
- `build.gradle`
- `src/main/java/com/smartcloset/common/exception/**`

이전 단계에서 만들어진 설정, 시간 계산, 매핑 core를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
기상청 단기예보 `getVilageFcst` JSON 호출을 담당하는 infrastructure client를 구현한다. 이 단계는 외부 호출과 KMA 응답 검증까지만 다루고, `WeatherProvider` 기본 bean 교체는 이후 step에서 한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/weather/infrastructure/kma/**`
- `src/test/java/com/smartcloset/weather/infrastructure/kma/**`
- 필요 시 `build.gradle`

## 구현 메모
- 외부 endpoint는 아래 하나로 제한한다.
  - `GET {KMA_BASE_URL}/getVilageFcst`
- 요청 parameter:
  - `serviceKey`
  - `pageNo=1`
  - `numOfRows=1000`
  - `dataType=JSON`
  - `base_date`
  - `base_time`
  - `nx`
  - `ny`
- JSON 응답에서 최소한 아래 경로를 읽는다.
  - `response.header.resultCode`
  - `response.header.resultMsg`
  - `response.body.items.item[]`
- `resultCode`가 `00`이 아니면 KMA client 실패로 처리한다.
- `NODATA_ERROR`, `items.item` 비어 있음, 응답 파싱 실패는 KMA client 실패로 처리한다.
- 실패는 provider가 fallback 여부를 판단할 수 있도록 infrastructure 전용 unchecked exception이나 결과 타입으로 전달한다.
- client 테스트는 실제 공공데이터 API를 호출하지 않고 mock server, fake transport, 또는 test double로 요청 parameter와 응답 처리를 검증한다.
- 필요하면 `KmaForecastClient` 같은 작은 interface를 두어 이후 provider/API 테스트에서 fake client를 주입하기 쉽게 한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
```

## 인수 기준
- `getVilageFcst` 요청 parameter가 문서 계약과 일치한다.
- 정상 JSON 응답에서 item 목록을 반환하거나 매핑 core에 넘길 수 있는 내부 모델로 변환한다.
- `resultCode != 00`, `NODATA_ERROR`, 빈 item, 파싱 실패 테스트가 있다.
- 테스트 중 실제 외부 API를 호출하지 않는다.
- `KMA_SERVICE_KEY` 실제 값은 테스트 fixture에 포함하지 않는다.

## 금지사항
- SmartCloset 공개 API endpoint를 추가하지 마라. 이유: KMA 호출은 내부 provider 구현 계약이다.
- 추천 도메인이나 `RecommendationService`에서 KMA client를 직접 호출하게 만들지 마라. 이유: 추천 유스케이스는 `WeatherProvider` 인터페이스에만 의존해야 한다.
- 실제 네트워크에 의존하는 테스트를 만들지 마라. 이유: 하네스와 CI에서 결정적으로 통과해야 한다.
- `getVilageFcst` 외의 KMA API를 호출하지 마라. 이유: 1.5차 외부 API 범위는 단기예보 상세 조회 하나로 제한된다.
