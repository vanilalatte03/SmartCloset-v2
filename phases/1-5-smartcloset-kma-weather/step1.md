# 단계 1: kma-time-and-mapping-core

범위: Must-have / 1.5 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/API.md`
- `docs/COMMANDS.md`
- `docs/adr/006-kma-vilage-forecast-weather-provider.md`
- `phases/1-5-smartcloset-kma-weather/step0.md`
- `src/main/java/com/smartcloset/weather/domain/WeatherCondition.java`
- `src/main/java/com/smartcloset/weather/domain/WeatherType.java`

이전 단계에서 만들어진 설정 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
HTTP와 Spring bean wiring에 의존하지 않는 KMA 시간 계산과 응답 item 매핑 core를 구현한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/weather/infrastructure/kma/**`
- `src/test/java/com/smartcloset/weather/infrastructure/kma/**`

## 구현 메모
- base date/time 계산:
  - 단기예보 발표시각은 `0200`, `0500`, `0800`, `1100`, `1400`, `1700`, `2000`, `2300`이다.
  - 각 발표시각 10분 이후부터 API에서 사용할 수 있다고 본다.
  - 현재 KST 기준 제공 가능한 최신 발표분을 선택한다.
  - 자정 전후 케이스에서 전날 `2300` 발표분을 선택할 수 있어야 한다.
- forecast target time 선택:
  - KMA item의 `fcstDate + fcstTime` group을 오름차순으로 묶는다.
  - 현재 KST 이후 가장 가까운 첫 forecast group을 선택한다.
  - 선택 group에 필수 category `TMP`, `SKY`, `PTY`, `PCP`, `WSD` 중 하나라도 없으면 다른 group으로 이동하지 않고 실패를 반환하거나 예외를 던진다.
- WeatherCondition 매핑:
  - `TMP`: 정수 섭씨로 변환해 `temperature`에 사용한다.
  - `PTY=1`, `PTY=2`, `PTY=4`: `RAINY`
  - `PTY=3`: `SNOWY`
  - `PTY=0`이고 `SKY=1`: `SUNNY`
  - `PTY=0`이고 `SKY=3` 또는 `SKY=4`: `CLOUDY`
  - `rainy`: `PTY != 0` 또는 `PCP`가 `-`, `null`, `0`, `강수없음`이 아니면 `true`
  - `windy`: `WSD >= 4.0`이면 `true`
- `POP`, `REH`, `TMN`, `TMX` 등은 1.5차 점수 계산에 사용하지 않는다.
- 테스트는 고정된 `Clock` 또는 명시적 `ZonedDateTime`을 사용해 결정적으로 작성한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
```

## 인수 기준
- base date/time 계산이 발표시각 10분 이후 규칙을 따른다.
- forecast target group은 현재 KST 이후 가장 가까운 group으로 결정된다.
- 선택 group의 필수 category 누락 시 다른 group으로 이동하지 않는다.
- `TMP`, `SKY`, `PTY`, `PCP`, `WSD` 매핑 단위 테스트가 있다.
- 매핑 결과는 내부 `WeatherCondition`이며 추천 도메인은 KMA DTO에 의존하지 않는다.

## 금지사항
- KMA HTTP client를 구현하지 마라. 이유: 이 단계는 순수 시간 계산과 매핑 core만 검증한다.
- 추천 점수 계산 로직을 수정하지 마라. 이유: KMA category는 provider에서 `WeatherCondition`으로 변환된 뒤 기존 추천 규칙을 사용한다.
- 누락 category가 있을 때 다음 forecast group으로 넘어가지 마라. 이유: 문서에서 선택 group 누락은 fallback 또는 strict 실패로 확정했다.
- JVM 기본 timezone에 의존하지 마라. 이유: KMA 기준은 현재 KST다.
